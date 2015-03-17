package service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import sun.misc.BASE64Encoder;
import util.SCUtils;
import util.XMLUtils;

import com.alibaba.fastjson.JSON;

import entity.Config;
import entity.Modify;

/**
 * 服务端插件自动更新类
 * 
 * @author zbl
 *
 */
public class AutoUpdater extends TimerTask {
	private final Logger logger = Logger.getLogger(AutoUpdater.class);
	private static Object LOCK = new Object();
	private final BASE64Encoder base64Encoder = new BASE64Encoder();
	
	private static final String SERVICE_NAME = "NetcallServer";// netcallServer服务名
	private static final String FILE_SERVER = "fileservertomcat";//文件服务器服务名
	private final String cloudUrl;//云端URL
	private final File userDir = new File(System.getProperty("user.dir"));// serverDog目录
	private final String netcallServerPath = userDir.getParentFile()
			.getAbsolutePath();// netcallServer目录
	private final File backupDir = new File(netcallServerPath + File.separator
			+ "oldfiles");// 备份目录
	private final File config = new File(netcallServerPath + File.separator
			+ "config.xml");// 配置文件，用于读取版本号
	private Config conf;// 更新配置
	private static int downloadFileFaildCount = 0;// 文件下载失败次数，用于回滚
	private boolean isRollBack = false;// 回滚标志
	private Map<String, String> rollBacks = new HashMap<String, String>();
	private Modify clientUpdateModify;
	
	public AutoUpdater(String url) {
		this.cloudUrl = url;
	}
	
	@Override
	public void run() {
		synchronized (LOCK) {
			logger.info("start check update,current time is " + Calendar.getInstance().getTime());
			checkUpdate();
			while(null != conf) {
				try {
					if (null != conf.getDelay()) {//延迟更新，避免云端有过大的并发数
						logger.info("delay: " + conf.getDelay() + " min");
						TimeUnit.SECONDS.sleep(60 * Integer.valueOf(conf.getDelay()));
					}
					logger.info("begin to stop netcallService");
					SCUtils.serviceStop(SERVICE_NAME);
					
					logger.info("begin to update files from cloud");
					for (Modify modify : conf.getModifies()) {
						if (!isRollBack) {
							updateFiles(modify);
						} else {
							break;
						}
					}
					
					if (!isRollBack) {//升级成功之后 修改版本号并且删除备份文件
						logger.info("update version to " + conf.getVersion());
						XMLUtils.setElementValue(config, "/NetcallServer/version", conf.getVersion());
						backupDir.delete();
					}
					
					logger.info("begin to restart netcallService");
					SCUtils.serviceStart(SERVICE_NAME);
					
					if (isRollBack) {
						logger.info("return ...........................");
						return;
					}
					if (SCUtils.isStarted(SERVICE_NAME) && null != clientUpdateModify) {
						logger.info("watring for netcallserver start up......");
						TimeUnit.SECONDS.sleep(60 * 3);
						setClientUpdateInfo(clientUpdateModify.getName(), conf.getClientUpdateInfo());
					}
				} catch (IOException e) {
					logger.error(e.getMessage());
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				} finally {
					reset();
				}
				
				checkUpdate();
			}
		}
	}

	/**
	 * 更新完毕之后将数据清零复位
	 */
	private void reset() {
		logger.info("----------update end ,reset flag---------");
		conf = null;
		downloadFileFaildCount = 0;
		isRollBack = false;
		rollBacks.clear();
		clientUpdateModify = null;
	}

	/**
	 * 去云端检查更新
	 */
	private void checkUpdate() {
		String oldVersion = XMLUtils.getElementValue(config,
				"/NetcallServer/version");
		logger.info("current version  --->" + oldVersion);
		String result = null;
		// start to send request
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpGet httpGet = new HttpGet(cloudUrl + "checkUpdate?version="
					+ oldVersion);
			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

				@Override
				public String handleResponse(HttpResponse response)
						throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >= 200 && status < 300) {
						HttpEntity entity = response.getEntity();
						return entity != null ? EntityUtils.toString(entity)
								: null;
					} else {
						throw new ClientProtocolException(
								"Unexpected response status:" + status);
					}
				}
			};
			result = httpClient.execute(httpGet, responseHandler);
		} catch (ClientProtocolException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}

		if (null != result && !"".equals(result) && result.contains("version")) {
			conf = JSON.parseObject(result, Config.class);
		} else {
			// 没有文件需要更新
		}
	}

	/**
	 * 更新文件
	 * 
	 * @param modify
	 */
	private void updateFiles(Modify modify) {
		if (modify.getOperationType().equalsIgnoreCase("add")) {
			try {
				addFiles(modify);
			} catch (IOException e) {
				downloadFileFaildCount++;
				logger.error(e.getMessage());
				if (downloadFileFaildCount <= 10) {
					updateFiles(modify);
				} else {
					isRollBack = true;
					rollBack();
				}
			}
		} else if (modify.getOperationType().equalsIgnoreCase("replace")) {
			try {
				replaceFile(modify);
			} catch (Exception e) {
				downloadFileFaildCount++;
				logger.error(e.getMessage());
				if (downloadFileFaildCount <= 10) {
					updateFiles(modify);
				} else {
					isRollBack = true;
					rollBack();
				}
			}
		} else if (modify.getOperationType().equalsIgnoreCase("delete")) {
			deleteFiles(modify);
		}
	}

	/**
	 * 添加指定文件
	 * 
	 * @param modify
	 *            修改说明
	 * @throws IOException
	 */
	private void addFiles(Modify modify) throws IOException {
		File addFile = new File(netcallServerPath + modify.getPath()
				+ File.separator + modify.getName());
		downLoadFilesFromCloud(modify, addFile);
		logger.info("add file:" + addFile.getAbsolutePath());
		if (addFile.getAbsolutePath().contains("enterprise\\spark")) {
			clientUpdateModify = modify;
		}
	}

	/**
	 * 替换指定文件
	 * 
	 * @param modify
	 *            修改说明
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private void replaceFile(Modify modify) throws IOException, InterruptedException {
		File replaceFile = new File(netcallServerPath + modify.getPath()
				+ File.separator + modify.getName());
		// 先备份并移除老文件
		if (replaceFile.exists()) {
			backup(replaceFile, modify.getPath());
			replaceFile.delete();
		}
		if (replaceFile.getAbsolutePath().contains("apache-tomcat-7.0.37\\webapps")) {
			SCUtils.serviceStop(FILE_SERVER);
		}
		
		downLoadFilesFromCloud(modify, replaceFile);
		
		if (replaceFile.getAbsolutePath().contains("apache-tomcat-7.0.37\\webapps")) {
			SCUtils.serviceStart(FILE_SERVER);
		}
		logger.info("replace file:" + replaceFile.getAbsolutePath());
	}

	/**
	 * 删除指定文件
	 * 
	 * @param modify
	 *            修改说明
	 */
	private void deleteFiles(Modify modify) {
		File delFile = new File(netcallServerPath + modify.getPath()
				+ File.separator + modify.getName());
		if (!delFile.exists()) {
			logger.info("the file not exists,nothing to do.");
		} else {
			backup(delFile, modify.getPath());
			delFile.delete();
		}
		logger.info("delete file:" + delFile.getAbsolutePath());
	}

	/**
	 * 从云端下载最新的文件
	 * 
	 * @param modify
	 *            修改详情
	 * @param targetFile
	 *            下载目标路径
	 */
	private void downLoadFilesFromCloud(Modify modify, File targetFile)
			throws IOException {
		if (!targetFile.exists()) {
			targetFile.createNewFile();
		}
		FileOutputStream fos = new FileOutputStream(targetFile);
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(cloudUrl + "downloadFile?version="
				+ conf.getVersion() + "&filename="
				+ base64Encoder.encode(modify.getAlias().getBytes("UTF-8")));
		httpGet.setHeader("Content-Type", "text/html; charset=UTF-8");
		logger.info("download file:" + modify.getAlias() + "  url: " + httpGet.getURI());
		CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
		try {
			if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw new ClientProtocolException("download failed:"
						+ httpResponse.getStatusLine());
			}

			httpResponse.getEntity().writeTo(fos);

			//check file
			String MD5 = DigestUtils.md5Hex(new FileInputStream(targetFile));
			if (!modify.getMd5().equalsIgnoreCase(MD5)) {
				throw new IOException("Validation file integrity failure. filename is " + modify.getName());
			}
			
			downloadFileFaildCount = 0;// 每次下载成功之后将失败次数归零
		} finally {
			fos.close();
			httpResponse.close();
			httpClient.close();
		}
	}

	/**
	 * 备份文件，用于回滚
	 * 
	 * @param file
	 *            要备份的文件
	 * @param path
	 *            相对路径
	 */
	private void backup(File file, String path) {
		if (!backupDir.exists()) {
			backupDir.mkdir();
		}
		rollBacks.put(file.getAbsolutePath(), backupDir.getAbsolutePath() + path + File.separator + file.getName());
		try {
			FileUtils.copyFileToDirectory(file,
					new File(backupDir.getAbsolutePath() + path));
		} catch (IOException e) {
			logger.error("back up files faild, IOException:" + e.getMessage());
		}
	}

	/**
	 * 回滚文件修改
	 */
	private void rollBack() {
		logger.info("-----------start to roll back ----------------");
		for (Modify modify : conf.getModifies()) {
			if (!rollBacks.containsKey(netcallServerPath + modify.getPath()
						+ File.separator + modify.getName())) {//未做过修改文件不需要回滚
				continue;
			}
			if (modify.getOperationType().equalsIgnoreCase("add")) {//回滚操作为删除
				File delFile = new File(netcallServerPath + modify.getPath()
						+ File.separator + modify.getName());
				if (delFile.exists()) {
					delFile.delete();
				}
			} else if (modify.getOperationType().equalsIgnoreCase("replace")) {
				File replaceFile = new File(netcallServerPath + modify.getPath()
						+ File.separator + modify.getName());
				String copy = rollBacks.get(replaceFile.getAbsolutePath());
				if (replaceFile.exists()) {
					replaceFile.delete();
				}
				try {
					FileUtils.copyFileToDirectory(new File(copy), new File(netcallServerPath + modify.getPath()));
				} catch (IOException e) {
					logger.error(replaceFile.getAbsolutePath() + " roll back faild:" + e.getMessage());
				}
			} else if (modify.getOperationType().equalsIgnoreCase("delete")) {
				File addFile = new File(netcallServerPath + modify.getPath()
						+ File.separator + modify.getName());
				String copy = rollBacks.get(addFile.getAbsolutePath());
				if (addFile.exists()) {
					addFile.delete();
				}
				try {
					FileUtils.copyFileToDirectory(new File(copy), new File(netcallServerPath + modify.getPath()));
				} catch (IOException e) {
					logger.error(addFile.getAbsolutePath() + "roll back faild:" + e.getMessage());
				}
			}
		}
	}
	
	/**
	 * 调用服务设置netcall客户端（PC）更新信息
	 * @param filename 最新的Client.exe
	 * @param clientUpdateInfo 最新的更新信息说明
	 */
	private void setClientUpdateInfo(String filename, String clientUpdateInfo) {
		logger.info("start to update netcallserver spark settings....");
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpGet httpGet = new HttpGet(
					"http://localhost:9090/plugins/uiauthentication/updateclient?name="
			+ filename + "&info=" + clientUpdateInfo);

			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

				@Override
				public String handleResponse(HttpResponse response)
						throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >= 200 && status < 300) {
						HttpEntity entity = response.getEntity();
						return entity != null ? EntityUtils.toString(entity)
								: null;
					} else {
						throw new ClientProtocolException(
								"Unexpected response status:" + status);
					}
				}
			};
			String result = httpClient.execute(httpGet, responseHandler);
			if (result.equals("0")) {
				logger.info("set client update infomation successfully");
			} else {
				logger.info("faild to set client update infomation faild");
			}
		} catch (ClientProtocolException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
	}
	
	public static void main(String[] args) {
		final AutoUpdater updater = new AutoUpdater("http://appserver.netcall.cc/NetCallUpdateService/");
		updater.conf = new Config();
		updater.conf.setVersion("1.0.1");
		final Modify modify = new Modify();
		modify.setMd5("E91BCF71EF07D9B8EF28863CFCF50368");
		modify.setAlias("netcall_2_7_8.exe");
		 
		ExecutorService exec = Executors.newCachedThreadPool();
		
		for (int i = 0 ; i < 20; i++) {
			exec.execute(new Runnable() {
				
				@Override
				public void run() {
					try {
						updater.downLoadFilesFromCloud(modify, new File("D:/" + UUID.randomUUID().toString() + ".exe"));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
}
