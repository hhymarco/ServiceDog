package service;

import java.io.File;
import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import util.SCUtils;

public class ServiceMonitor extends TimerTask {

	private File file = new File(System.getProperty("user.dir") + "\\dump");
	private final Logger logger = Logger.getLogger(ServiceMonitor.class);
	private final String serviceName = "NetcallServer";

	@Override
	public void run() {
		logger.info("dump path:" + file.getAbsolutePath());
		if (file.exists() && file.listFiles().length > 0) {
			logger.warn("-----------------Exception-----------------");
			String filename = file.listFiles()[0].getName();
			file.listFiles()[0].delete();
			String pid = filename.substring(8,filename.lastIndexOf("."));
			try {
				logger.info("kill process pid：" + pid);
				Runtime.getRuntime().exec("cmd /c taskkill /F /pid " + pid);
				
				TimeUnit.SECONDS.sleep(3);
				logger.info("begin to restart netcallService");
				
				SCUtils.serviceStart(serviceName);
			} catch (IOException e) {
				logger.error(e.getMessage());
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			}
		}
	}
}
