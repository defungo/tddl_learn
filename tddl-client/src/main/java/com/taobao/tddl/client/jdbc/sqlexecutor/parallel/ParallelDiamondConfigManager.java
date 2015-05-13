package com.taobao.tddl.client.jdbc.sqlexecutor.parallel;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.common.lang.StringUtil;
import com.alibaba.common.lang.io.ByteArrayInputStream;
import com.taobao.tddl.common.TDDLConstant;
import com.taobao.tddl.common.config.ConfigDataHandler;
import com.taobao.tddl.common.config.ConfigDataHandlerFactory;
import com.taobao.tddl.common.config.ConfigDataListener;
import com.taobao.tddl.common.config.impl.DefaultConfigDataHandlerFactory;

/**
 *����ִ�п����࣬һ��dbIndexһ���̳߳أ��ӳٳ�ʼ����
 *�̳߳ر��Ͳ��Բ������߳�ִ�У�FIXME:�Ӳ��д��жԱ�
 *���Կ���û���⣬������û���飬��Ҫ���飩��
 *
 *�־���������������������useparallelexecute��parallelthreadcount
 *<code>
 *   useParallelExecute=true
 *   parallelThreadCount=10
 *</code>
 *
 *dataId��ʽΪ<code>com.taobao.tddl.jdbc.client.sqlexecutor.{0}</code>
 *����dataId���Ե���<code>ParallelDiamondConfigManager.getSqlExecutorKey(appName)</code>   
 *
 *��������Ҳ����ͨ��tddl���ù������ֱ�����ã�������д
 *
 * @author junyu
 * 
 */
public class ParallelDiamondConfigManager implements ConfigDataListener {
	private static Log logger = LogFactory
			.getLog(ParallelDiamondConfigManager.class);
	public static final String USE_PARALLEL_EXECUTE = "useparallelexecute";
	public static final String PARALLEL_THREAD_COUNT = "parallelthreadcount";

	private static MessageFormat PARALLEL_EXECUTOR_FORMAT = new MessageFormat(
			"com.taobao.tddl.jdbc.client.sqlexecutor.{0}");

	private static boolean useParallel = false;
	private static int esThreadCount = 10;
	private static int queueSize = 2;

	private static Map<String, ThreadPoolExecutor> esMap = new ConcurrentHashMap<String, ThreadPoolExecutor>();

	private boolean inited = false;

	/**
	 * ��Ҫ��TDataSourceʵ����ʱ����
	 */
	public ParallelDiamondConfigManager(String appName) {
		init(appName);
	}

	protected void init(String appName) {
		if (inited) {
			return;
		}

		ConfigDataHandlerFactory configHandlerFactory = new DefaultConfigDataHandlerFactory();
		ConfigDataHandler dataHandler = configHandlerFactory
				.getConfigDataHandler(getSqlExecutorKey(appName),this);
		String data = null;

		try {
			data = dataHandler.getData(TDDLConstant.DIAMOND_GET_DATA_TIMEOUT,ConfigDataHandler.FIRST_CACHE_THEN_SERVER_STRATEGY);
		} catch (Exception e) {
			logger.error("[PARALLEL_EXECUTE]try to get diamond config error.",
					e);
		}

		if (StringUtil.isBlank(data)) {
			logger.warn("no parallel execute info, set useParallel false");
			setUseParallelFalse();
			return;
		}

		logger.warn("[INIT]recieve parallel execute config,start to init.data:"
				+ data);
		configChange(data, true);
		inited = true;
		logger.warn("[INIT]init parallel execute info success!");
	}

	/**
	 * ���յ������ý��н�����ʼ��
	 * 
	 * @param data
	 * @param isInit
	 */
	protected synchronized void configChange(String data, boolean isInit) {
		Properties prop = parseConfigStr2Prop(data.toLowerCase());

		/**
		 * ��ȫ����������������ִ�У���������ÿ���ش�С��
		 */
		if (StringUtil.isBlank((String) prop.get(USE_PARALLEL_EXECUTE))
				|| StringUtil.isBlank((String) prop.get(PARALLEL_THREAD_COUNT))) {
			logger.warn("the parallel config useparallelexecute or parallelthreadcount is blank,must be both configed");
			setUseParallelFalse();
			return;
		}

		boolean newUseParallel = Boolean.valueOf((String) prop
				.get(USE_PARALLEL_EXECUTE));

		if (newUseParallel != useParallel) {
			useParallel = newUseParallel;
		}

		int threadCount = Integer.valueOf((String) prop
				.get(PARALLEL_THREAD_COUNT));

		/**
		 * ����µĳش�С���ϵĳش�С��һ��, ���Ҳ��ǳ�ʼ��,��ô��̬�޸ĳش�С
		 * 
		 */
		if (esThreadCount != threadCount) {
			esThreadCount = threadCount;
			if (!isInit) {
				synchronized (esMap) {
					for (Map.Entry<String, ThreadPoolExecutor> entry : esMap
							.entrySet()) {
						entry.getValue().setCorePoolSize(esThreadCount);
						entry.getValue().setMaximumPoolSize(esThreadCount * 2);
					}
				}
			}
		}
	}

	/**
	 * ��̬��������
	 */
	public void onDataRecieved(String dataId,String data) {
		if (null == data) {
			setUseParallelFalse();
			logger.warn("no parallel execute info, set useParallel false");
			return;
		}

		logger
				.warn("[RUNNING]recieve parallel execute config,dataId:"+dataId+" start to init data:"
						+ data);
		configChange(data, false);
		logger.warn("[RUNNING]reset parallel execute info success!");
	}

	public static String getSqlExecutorKey(String appName) {
		return PARALLEL_EXECUTOR_FORMAT.format(new Object[] { appName });
	}

	/**
	 * ��property�ַ���ת����Properties
	 * 
	 * @param data
	 * @return
	 */
	private Properties parseConfigStr2Prop(String data) {
		Properties prop = new Properties();
		if (StringUtil.isNotBlank(data)) {
			ByteArrayInputStream byteArrayInputStream = null;
			try {
				byteArrayInputStream = new ByteArrayInputStream(data.getBytes());
				prop.load(byteArrayInputStream);
			} catch (IOException e) {
				logger
						.error("[PARALLEL_EXECUTE]parse diamond config error!",
								e);
			} finally {
				byteArrayInputStream.close();
			}
		}

		return prop;
	}

	private void setUseParallelFalse() {
		useParallel = false;
	}

	/**
	 * �ύһ���������̳߳�ִ�У� һ����һ����ά��һ���̳߳أ� ��ֹ�������ܵ����š� ����̳߳�map���治����Ŀ��� ���̳߳أ���ô�½�һ����
	 * 
	 * 
	 * @param dbIndex
	 * @param command
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Future submit(String dbIndex, Runnable command) {
		ThreadPoolExecutor es = null;
		/**
		 * ���������£�����оͲ���Ҫ����
		 */
		if (null != esMap.get(dbIndex)) {
			es = esMap.get(dbIndex);
			return es.submit(command);
		}

		synchronized (esMap) {
			/**
			 * ���ʱ��esMapû��ָ���̳߳صļ��ʱȽϴ� �������ж�Ϊnull
			 */
			if (null == esMap.get(dbIndex)) {
				logger.warn("init threadpool for " + dbIndex);
				logger.warn("dbIndex:" + dbIndex
						+ ",parallel threadPool corepool size:" + esThreadCount
						+ ",maxpool size:" + esThreadCount*2);
				es = new ThreadPoolExecutor(esThreadCount, esThreadCount * 2,
						2000, TimeUnit.MILLISECONDS,
						new LinkedBlockingQueue<Runnable>(queueSize),
						new NamedThreadFactory("TDDL-PARALLEL"),
						new ThreadPoolExecutor.CallerRunsPolicy());
				esMap.put(dbIndex, es);
			} else {
				/**
				 * �����С���ܾ����ڵ�һ���ж���Ϊnull�� �����ؼ�����һ��ָ��key���̳߳�
				 */
				es = esMap.get(dbIndex);
			}
		}
		return es.submit(command);
	}

	public static boolean isUseParallel() {
		return useParallel;
	}
}
