package com.taobao.tddl.common;

import java.io.File;
import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.xml.DOMConfigurator;

import com.taobao.tddl.common.monitor.DailyMaxRollingFileAppender;

/**
 * ������hsf��ͬ���ࡣȥ���汾�ţ���־���·��ȡ${user.home}/logs/tddl/
 */
public class LoggerInit {
	public static final Logger TDDL_LOG = Logger.getLogger("TDDL_LOG");
	public static final Logger TDDL_SQL_LOG = Logger.getLogger("TDDL_SQL_LOG");
	public static final Logger TDDL_MD5_TO_SQL_MAPPING = Logger.getLogger("TDDL_MD5_TO_SQL_MAPPING");
	public static final Logger TDDL_Nagios_LOG = Logger.getLogger("TDDL_Nagios_LOG");
	//modify by junyu ,atom ��matrix��
	public static final Logger TDDL_Atom_Statistic_LOG = Logger.getLogger("TDDL_Atom_Statistic_LOG");
	public static final Logger TDDL_Matrix_Statistic_LOG = Logger.getLogger("TDDL_Matrix_Statistic_LOG");
	
	public static final Logger TDDL_Statistic_LOG = Logger.getLogger("TDDL_Statistic_LOG");
	public static final Logger TDDL_Snapshot_LOG = Logger.getLogger("TDDL_Snapshot_LOG");
	public static final Logger logger = TDDL_LOG; //Logger.getLogger(LoggerInit.class);

	static private volatile boolean initOK = false;

	private static String getLogPath() {
		String userHome = System.getProperty("user.home");
		if (!userHome.endsWith(File.separator)) {
			userHome += File.separator;
		}
		String path = userHome + "logs" + File.separator + "tddl" + File.separator;
		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return path;
	}

	static {
		initTddlLog();
	}

	private static void showAppender(Appender appender) {
		if (appender == null) {
			System.out.println("apender:null");
			return;
		}
		System.out.println("appender=" + appender);
		System.out.println("name=" + appender.getName() + ",layout=" + appender.getLayout());
	}

	private static Appender buildAppender(String name, String fileName, String pattern) {
		DailyRollingFileAppender appender = new DailyRollingFileAppender();
		appender.setName(name);
		appender.setAppend(true);
		appender.setEncoding("GBK");
		appender.setLayout(new PatternLayout(pattern));
		appender.setFile(new File(getLogPath(), fileName).getAbsolutePath());
		appender.activateOptions();// ����Ҫ������ԭ����־���ݻᱻ���
		return appender;
	}
	
	private static Appender buildAtomStaticAppender(String name, String fileName, String pattern){
		DailyMaxRollingFileAppender appender = new DailyMaxRollingFileAppender();
		appender.setName(name);
		appender.setAppend(true);
		appender.setEncoding("GBK");
		appender.setLayout(new PatternLayout(pattern));
		appender.setDatePattern("'.'yyyy-MM-dd-HH");
		appender.setMaxBackupIndex(6);
		appender.setFile(new File(getLogPath(), fileName).getAbsolutePath());
		appender.activateOptions();// ����Ҫ������ԭ����־���ݻᱻ���
		return appender;
	}
	
	private static Appender buildMatrixStaticAppender(String name, String fileName, String pattern){
		DailyMaxRollingFileAppender appender = new DailyMaxRollingFileAppender();
		appender.setName(name);
		appender.setAppend(true);
		appender.setEncoding("GBK");
		appender.setLayout(new PatternLayout(pattern));
		appender.setDatePattern("'.'yyyy-MM-dd-HH");
		appender.setMaxBackupIndex(12);
		appender.setFile(new File(getLogPath(), fileName).getAbsolutePath());
		appender.activateOptions();// ����Ҫ������ԭ����־���ݻᱻ���
		return appender;
	}

	synchronized static public void initTddlLog() {
		if (initOK)
			return;
		initOK=true;
		Appender tddlAppender = buildAppender("TDDL_Appender", "tddl.log", "%d %p [%c{10}] - %m%n");
//		Appender sqlAppender = buildAppender("TDDL_SQL_Appender", "tddl.sql.log", "%d %p [%c{10}] - %m%n");
		Appender md5sqlAppender = buildAppender("TDDL_MD5_TO_SQL_Appender", "tddl.md5sql.log", "%d %p [%c{10}] - %m%n");
		Appender nagiosAppender = buildAppender("TDDL_Nagios_Appender", "Nagios.log", "%m%n");
		Appender atomStatisticAppender = buildAtomStaticAppender("TDDL_Atom_Statistic_Appender", "tddl-atom-statistic.log", "%m");
		Appender matrixStatisticAppender = buildMatrixStaticAppender("TDDL_Matrix_Statistic_Appender", "tddl-matrix-statistic.log", "%m");
		
		Appender statisticAppender = buildAppender("TDDL_Statistic_Appender", "tddl-statistic.log", "%m");
		Appender snapshotAppender = buildAppender("TDDL_Snapshot_Appender", "tddl-snapshot.log", "%m");

		TDDL_LOG.setAdditivity(false);
		TDDL_LOG.removeAllAppenders();
		TDDL_LOG.addAppender(tddlAppender);
		TDDL_LOG.setLevel(Level.WARN);

//      �����־��Ӧ���Լ�����
//		TDDL_SQL_LOG.setAdditivity(false);
//		TDDL_SQL_LOG.removeAllAppenders();
//		TDDL_SQL_LOG.addAppender(sqlAppender);
//		TDDL_SQL_LOG.setLevel(Level.WARN);

		TDDL_MD5_TO_SQL_MAPPING.setAdditivity(false);
		TDDL_MD5_TO_SQL_MAPPING.removeAllAppenders();
		TDDL_MD5_TO_SQL_MAPPING.addAppender(md5sqlAppender);
		TDDL_MD5_TO_SQL_MAPPING.setLevel(Level.DEBUG);

		TDDL_Nagios_LOG.setAdditivity(false);
		TDDL_Nagios_LOG.removeAllAppenders();
		TDDL_Nagios_LOG.addAppender(nagiosAppender);
		TDDL_Nagios_LOG.setLevel(Level.INFO);

		TDDL_Atom_Statistic_LOG.setAdditivity(false);
		TDDL_Atom_Statistic_LOG.removeAllAppenders();
		TDDL_Atom_Statistic_LOG.addAppender(atomStatisticAppender);
		TDDL_Atom_Statistic_LOG.setLevel(Level.INFO);

		TDDL_Matrix_Statistic_LOG.setAdditivity(false);
		TDDL_Matrix_Statistic_LOG.removeAllAppenders();
		TDDL_Matrix_Statistic_LOG.addAppender(matrixStatisticAppender);
		TDDL_Matrix_Statistic_LOG.setLevel(Level.INFO);
		
		
		TDDL_Statistic_LOG.setAdditivity(false);
		TDDL_Statistic_LOG.removeAllAppenders();
		TDDL_Statistic_LOG.addAppender(statisticAppender);
		TDDL_Statistic_LOG.setLevel(Level.INFO);

		TDDL_Snapshot_LOG.setAdditivity(false);
		TDDL_Snapshot_LOG.removeAllAppenders();
		TDDL_Snapshot_LOG.addAppender(snapshotAppender);
		TDDL_Snapshot_LOG.setLevel(Level.INFO);
	}

	static public void initTddlLogByFile() {
		if (initOK)
			return;

		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(LoggerInit.class.getClassLoader());
		// ʹHSF��log4j������Ч(Logger, Appender)
		DOMConfigurator.configure(LoggerInit.class.getClassLoader().getResource("tddl-log4j.xml"));

		// ����log4j.xml�����е�Appender������Щappender�������־ȫ�����иı�
		String logPath = getLogPath();
		//FileAppender fileAppender = null;
		for (Enumeration<?> e = Logger.getLogger("only_for_get_all_appender").getAllAppenders(); e.hasMoreElements();) {
			Appender appender = (Appender) e.nextElement();
			if (FileAppender.class.isInstance(appender)) {
				FileAppender logFileAppender = (FileAppender) appender;
				File deleteFile = new File(logFileAppender.getFile());
				File logFile = new File(logPath, logFileAppender.getFile());
				logFileAppender.setFile(logFile.getAbsolutePath());
				logFileAppender.activateOptions(); // ����Ҫ������ԭ����־���ݻᱻ���
				if (deleteFile.exists()) {
					deleteFile.delete();
				}
				logger.warn("�ɹ��ı�" + deleteFile.getName() + "�����·����:" + logFile.getAbsolutePath());
			}
		}
		Thread.currentThread().setContextClassLoader(loader);
		initOK = true;
	}
}