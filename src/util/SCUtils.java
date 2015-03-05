package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class SCUtils {
	
	private static Logger logger = Logger.getLogger(SCUtils.class);
	private static final int RETRY_NUMBER = 20;
	
	/**
	 * 启动windows服务
	 * 
	 * @param serviceName 服务名称
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void serviceStart(final String serviceName) throws IOException, InterruptedException {
		logger.info("try to start service:" + serviceName);
		int count = 0;
		while (!isStarted(serviceName)) {
			if (count >= RETRY_NUMBER) {
				logger.info("fail to start service:" + serviceName);
				return;
			}
			startService(serviceName);
			count++;
			TimeUnit.SECONDS.sleep(1);
		}
	}
	
	/**
	 * 关闭windows服务
	 * @param serviceName 服务名称
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void serviceStop(final String serviceName) throws IOException, InterruptedException {
		logger.info("try to stop service:" + serviceName);
		int count = 0;
		while (isStarted(serviceName)) {
			if (count >= RETRY_NUMBER) {
				logger.info("fail to stop netcallService");
				return;
			}
			stopService(serviceName);
			count++;
			TimeUnit.SECONDS.sleep(1);
		}
	}
	

	public static boolean isStarted(final String serviceName) throws IOException {
		boolean flag = false;
		Process processQuery = Runtime.getRuntime().exec(
				"cmd /c sc query \"" + serviceName + "\"");
		BufferedReader brQuery = new BufferedReader(
				new InputStreamReader(processQuery.getInputStream()));
		String queryRes = getSCQueryInfo(brQuery);
		if (queryRes.indexOf("1060") != -1) {
			logger.info("检测" + serviceName + "服务不存在");
		} else {
			if (queryRes.indexOf("STATE") != -1) {
				if (queryRes.indexOf("START_PENDING") != -1
						|| queryRes.indexOf("RUNNING") != -1) {
					logger.info(serviceName + "服务启动已在运行");
					flag = true;
				} else {
					logger.info(serviceName + "服务未启动");
				}
			}
		}
		return flag;
	}
	
	private static void startService(final String serviceName) throws IOException {
		Process process_start = Runtime.getRuntime().exec(
				"cmd /c sc start \"" + serviceName + "\"");
		BufferedReader brStart = new BufferedReader(
				new InputStreamReader(process_start.getInputStream()));

		String startRes = getSCQueryInfo(brStart);

		if (startRes.indexOf("STATE") != -1
				&& (startRes.indexOf("START_PENDING") != -1 || startRes
						.indexOf("RUNNING") != -1)) {
			logger.info(serviceName + "服务启动成功");
		} else {
			logger.info(serviceName + "服务启动失败");
		}
	}
	
	private static void stopService(final String serviceName) throws IOException {
		Process process_stop = Runtime.getRuntime().exec(
				"cmd /c sc stop \"" + serviceName + "\"");
		BufferedReader brStop = new BufferedReader(
				new InputStreamReader(process_stop.getInputStream()));
		
		String stopRes = getSCQueryInfo(brStop);
		if (stopRes.indexOf("1060") != -1) {
			logger.info("检测" + serviceName + "服务不存在");
		} else {
			if (stopRes.indexOf("STATE") != -1) {
				if (stopRes.indexOf("START_PENDING") != -1
						|| stopRes.indexOf("RUNNING") != -1) {
					logger.info(serviceName + "服务未能关闭");
				} else {
					logger.info(serviceName + "服务已关闭");
				}
			}
		}
	}
	
	private static String getSCQueryInfo(BufferedReader br) throws IOException {
		String result = "";
		String temp = br.readLine();
		int i = 0;
		while (i < 7) {
			result += temp;
			temp = br.readLine();
			i++;
		}
		return result;
	}
}
