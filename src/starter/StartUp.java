package starter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Properties;
import java.util.Timer;

import service.AutoUpdater;
import service.ServiceMonitor;

/**
 * 启动类
 * @author zbl
 *
 */
public class StartUp {
	private static String autoUpdateTime;
	private static String checkPeriod;
	private static String cloudURL;
	
	static {
		Properties properties = new Properties();
		InputStream in = Object.class.getResourceAsStream("/default.properties");
		try {
			properties.load(in);
			autoUpdateTime = properties.getProperty("autoUpdateTime");
			checkPeriod = properties.getProperty("checkPeriod");
			cloudURL = properties.getProperty("cloudURL");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		final Calendar date = Calendar.getInstance();
		date.set(Calendar.HOUR_OF_DAY, Integer.valueOf(autoUpdateTime));
		
		Long now = Calendar.getInstance().getTimeInMillis();
		Long delay = date.getTimeInMillis() - now;
		if (delay < 0) { //如果当前时间已经超过设定时间，天数加一
			date.add(Calendar.DATE, 1);
			delay = date.getTimeInMillis() - now;
		}
		Timer timer = new Timer();
		timer.schedule(new ServiceMonitor(), 5000, Long.valueOf(checkPeriod));
		timer.schedule(new AutoUpdater(cloudURL), delay, 1000 * 60 * 60 * 24);
	}
}
