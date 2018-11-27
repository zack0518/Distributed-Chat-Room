package utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
	static SimpleDateFormat df = new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)");
	
	public static void info(String message) {
		System.out.println(df.format(new Date()) + "(INFO) " + message);
	}
	
	public static void warning(String message) {
		System.out.println(df.format(new Date()) + "(WARNING) " + message);
	}
	
	public static void error(String message) {
		System.err.println(df.format(new Date()) + "(ERROR) " + message);
	}
}
