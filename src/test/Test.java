package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class Test {
	public static void main(String[] args) throws IOException {
		String serviceName = "fileservertomcat";
		
		while (true) {
		Process processQuery = Runtime.getRuntime().exec("sc query \"" + serviceName + "\"");
		BufferedReader brQuery = new BufferedReader(new InputStreamReader(processQuery.getInputStream()));
		
		String queryRes = getSCQueryInfo(brQuery);
		
		if (queryRes.indexOf("1060") != -1) {
			System.out.println("���" + serviceName + "���񲻴���");
		} else {
			if(queryRes.indexOf("STATE") != -1) {
				System.out.println(queryRes);
				if (queryRes.indexOf("START_PENDING") != -1 
						|| queryRes.indexOf("RUNNING") != -1) {
					System.out.println(serviceName + "����������������");
				} else {
					System.out.println("���" + serviceName + "�������������״̬");
					System.out.println(serviceName + "��������....");
					
					Process process_start = Runtime.getRuntime().exec("sc start \"" + serviceName + "\"");
					BufferedReader brStart = new BufferedReader(new InputStreamReader(process_start.getInputStream()));
					
					String startRes = getSCQueryInfo(brStart);
					System.out.println(startRes);
					
					if (startRes.indexOf("STATE") != -1 && (startRes.indexOf("START_PENDING") != -1 
							|| startRes.indexOf("RUNNING") != -1)) {
						System.out.println(serviceName + "���������ɹ�");
					} else {
						System.out.println(serviceName + "��������ʧ��");
					}
				
				}
			}
		}
		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		}
	}
	
	public static String getSCQueryInfo(BufferedReader br) throws IOException{
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
