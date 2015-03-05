package service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class ServiceThread extends Thread {

	private final String serviceName;
	private final long period;
	
	Logger logger = Logger.getLogger(ServiceThread.class);

	public ServiceThread(String serviceName, long period) {
		this.serviceName = serviceName;
		this.period = period;
	}

	@Override
	public void run() {

		while (true) {
			Process processQuery = null;
			try {
				processQuery = Runtime.getRuntime().exec(
						"sc query \"" + serviceName + "\"");
				BufferedReader brQuery = new BufferedReader(
						new InputStreamReader(processQuery.getInputStream()));

				String queryRes = getSCQueryInfo(brQuery);
				if (queryRes.indexOf("1060") != -1) {
					logger.info("���" + serviceName + "���񲻴���");
				} else {
					if (queryRes.indexOf("STATE") != -1) {
						if (queryRes.indexOf("START_PENDING") != -1
								|| queryRes.indexOf("RUNNING") != -1) {
							logger.info(serviceName + "����������������");
						} else {
							logger.info("���" + serviceName + "�������������״̬");
							logger.info(serviceName + "��������....");

							Process process_start = Runtime.getRuntime().exec(
									"sc start \"" + serviceName + "\"");
							BufferedReader brStart = new BufferedReader(
									new InputStreamReader(
											process_start.getInputStream()));

							String startRes = getSCQueryInfo(brStart);
							logger.info(startRes);

							if (startRes.indexOf("STATE") != -1
									&& (startRes.indexOf("START_PENDING") != -1 || startRes
											.indexOf("RUNNING") != -1)) {
								logger.info(serviceName + "���������ɹ�");
							} else {
								logger.info(serviceName + "��������ʧ��");
							}

						}
					}
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			try {
				TimeUnit.MILLISECONDS.sleep(period);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

	public static String getSCQueryInfo(BufferedReader br) throws IOException {
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
