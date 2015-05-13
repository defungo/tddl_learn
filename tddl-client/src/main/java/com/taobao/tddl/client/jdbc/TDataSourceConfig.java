package com.taobao.tddl.client.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.alibaba.common.lang.io.ByteArrayInputStream;
import com.taobao.tddl.client.controller.SpringBasedDispatcherImpl;
import com.taobao.tddl.client.dispatcher.SqlDispatcher;
import com.taobao.tddl.client.dsmatrixcreator.DataSourceMatrixCreatorImp;
import com.taobao.tddl.client.jdbc.listener.HookPoints;
import com.taobao.tddl.client.jdbc.sqlexecutor.parallel.ParallelDiamondConfigManager;
import com.taobao.tddl.client.pipeline.DefaultPipelineFactory;
import com.taobao.tddl.client.pipeline.NewRulePipelineFactory;
import com.taobao.tddl.client.pipeline.PipelineFactory;
import com.taobao.tddl.client.pipeline.RuleLePipelineFactory;
import com.taobao.tddl.client.rule.le.RuleLeBeanConvert;
import com.taobao.tddl.client.util.DataSourceType;
import com.taobao.tddl.common.ConfigServerHelper;
import com.taobao.tddl.common.ConfigServerHelper.DataListener;
import com.taobao.tddl.common.DataSourceChangeListener;
import com.taobao.tddl.common.Monitor;
import com.taobao.tddl.common.RuntimeConfigHolder;
import com.taobao.tddl.common.config.DefaultTddlConfigParser;
import com.taobao.tddl.common.config.PropertiesConfigParser;
import com.taobao.tddl.common.config.TddlConfigParser;
import com.taobao.tddl.common.config.beans.AppRule;
import com.taobao.tddl.common.jdbc.DataSourceConfig;
import com.taobao.tddl.common.util.DataSourceFetcher;
import com.taobao.tddl.common.util.StringXmlApplicationContext;
import com.taobao.tddl.common.util.TDDLMBeanServer;
import com.taobao.tddl.common.util.TDataSourceConfigHolder;
import com.taobao.tddl.common.util.mbean.TDDLMBean;
import com.taobao.tddl.interact.monitor.TotalStatMonitor;
import com.taobao.tddl.interact.rule.VirtualTableRoot;
import com.taobao.tddl.interact.rule.bean.DBType;
import com.taobao.tddl.jdbc.group.DataSourceWrapper;
import com.taobao.tddl.jdbc.group.TGroupDataSource;
import com.taobao.tddl.parser.SQLParser;
import com.taobao.tddl.parser.SQLParserImp;
import com.taobao.tddl.rule.bean.PropertyBaseTDDLRoot;
import com.taobao.tddl.rule.bean.TDDLRoot;
import com.taobao.tddl.rule.le.TddlRuleInner;
import com.taobao.tddl.rule.le.bean.RuleChangeListener;

/**
 * TDataSource�������࣬ר�ŷ���TDataSource�����ô�����룬Ϊ�����÷���
 */
public class TDataSourceConfig implements ApplicationContextAware,
		DataSourceChangeListener,RuleChangeListener{

	protected static final Log logger = LogFactory
			.getLog(TDataSourceConfig.class);
	public static final String DBINDEX_DSKEY_CONN_CHAR = "_";
	public static final String STR_LINE_MARK = "-";
	private static final String DEFAULT_WRITE_RULE_ID = "tddl_root";

	private String ruleUrl;
	protected DBType dbType = DBType.MYSQL;
	protected Map<String, SqlDispatcher> dispatcherMap;
	protected SqlDispatcher defaultDispatcher;
	protected HookPoints hookPoints = HookPoints.DEFAULT;
	/**
	 * ָ����TDS�Ƿ���Ҫ�����и��ƣ���ʼ�������и������ã�
	 */
	protected boolean isHandleReplication;
	protected final RuntimeConfigHolder<TddlRuntime> runtimeConfigHolder = new RuntimeConfigHolder<TddlRuntime>();
	/**
	 * �Ƿ���statlog�ϴ�ӡ������db��tablesִ����Ϣ��Ĭ�Ϲر�
	 */
	protected boolean enableProfileRealDBAndTables;

	protected boolean isReadOnly = false; // �Ƿ�ֻ��
	protected boolean isMasterOnly = false; // �Ƿ�ֻʹ��master����
	protected boolean isSlaveOnly = false; // �Ƿ�ֻʹ��slave����

	protected PipelineFactory pipelineFactory = null;

	protected ParallelDiamondConfigManager parallelManager = null;

	protected DataSourceMatrixCreatorImp dataSourceMatrixCreator;
	private Map<String, ? extends Object> rwDataSourcePoolConfig;

	private ApplicationContext springContext; // �õ�������

	private String appName;
	private boolean isUseLocalConfig = false;
	private String[] appRuleFiles;
	private String appRulePropertiesString;
	private String appRuleString;
	/**
	 * ʹ��tbdatasource����druid
	 */
	private DataSourceType dataSourceType = DataSourceType.TbDataSource;
	/**
	 * out specify classloader for load rule bean;
	 */
	private ClassLoader specifyClassLoader = null;

	/**
	 * add by junyu ,���Ϊtrue,��ô����244�µİ汾���� 2.3.x��ʼ�Ĺ���ΪĬ�Ϲ��򣬴��µĹ����������޸�
	 */
	private boolean useNewRule = false;

	private boolean dynamicRule = false;
	//�����ⲿע��
	private TddlRuleInner tddlRule=null;
	/**
	 * let user response the rule change if necessary
	 */
	private List<RuleChangeListener> outListeners = new ArrayList<RuleChangeListener>();

	protected DBType defaultDbType;

	private String ruleRootBeanID = DEFAULT_WRITE_RULE_ID;

	public static final TotalStatMonitor statMonitor = TotalStatMonitor.getInstance();
	private final TddlConfigParser<AppRule> shardRuleParser = new DefaultTddlConfigParser<AppRule>();
	// ��������ĵļ�����
	private DataListener shardRuleListener = new DataListener() {
		public void onDataReceiveAtRegister(Object data) {
			Object tddlConfig = shardRuleParser.parseCongfig((String) data);
			if (tddlConfig != null) {
				if (tddlConfig instanceof AppRule) {
					TDataSourceConfig.this.init((AppRule) tddlConfig);
				} else if (tddlConfig instanceof VirtualTableRoot) {
					TDataSourceConfig.this.init((VirtualTableRoot) tddlConfig);
				}
			}
		}

		public void onDataReceive(Object data) {
			logger.warn("�ݲ�֧�ֶ�̬�޸ķֿ�ֱ�����յ����ͣ�" + data);
		}
	};

	/**
	 * ���Թ�Springʹ�á����ҵ���ƿ�springֱ��ʹ�ã�������set������ע�����ԣ��ٵ���init
	 * ���ʹ��Properties��ʽ��ʼ����Ĭ��ֻ֧��1�������ļ�����appRuleFiles[0]
	 */
	public void init() {
		if (appRuleFiles != null) {
			this.isUseLocalConfig = true;
            // ʹ���ⲿ  springContext ���������ļ�·��
		    appRuleFiles = doExternalResolve(appRuleFiles);
			if (appRuleFiles[0] != null
					&& appRuleFiles[0].indexOf(".xml") != -1) {
				initBySpringBaseAppRuleFile(appRuleFiles);
			} else if (appRuleFiles[0] != null
					&& appRuleFiles[0].indexOf(".properties") != -1) {
				initByPropertyBaseAppRuleFile(appRuleFiles[0]);
			} else {
				throw new IllegalArgumentException(
						"appRuleFile����Ϊ�ջ��߲�֧�ֵĹ����ļ�����");
			}
		} else if(appRuleString!=null){
			this.isUseLocalConfig=true;
			initByXmlBaseAppRuleString(appRuleString);
		} else if (appRulePropertiesString != null) {
			this.isUseLocalConfig = true;
			// �ַ�����ʽ����ֻ֧��properties�����ļ�
			initByPropertyBaseAppRuleString(appRulePropertiesString);
		} else if(!isUseLocalConfig){
			// ���ķֿ�ֱ����
			if(!dynamicRule){
				// ���ķֿ�ֱ����
				Object firstFetchedShardRule = ConfigServerHelper
						.subscribeShardRuleConfig(appName, shardRuleListener);
				if (firstFetchedShardRule == null) {
					throw new IllegalStateException("û�н��յ��ֿ�ֱ��������");
				}
			}else{
				if(tddlRule==null){
				    tddlRule=new TddlRuleInner();
					tddlRule.setAppName(appName);
					tddlRule.addRuleChangeListener(this);
					tddlRule.init();
				}
				
				for(RuleChangeListener listener:this.outListeners){
					tddlRule.addRuleChangeListener(listener);
				}
				String tddlRuleStr=RuleLeBeanConvert.convertLeAndOriRuleStr2InteractRule(tddlRule.getOldRuleStr());
				this.initByXmlBaseAppRuleString(tddlRuleStr);
			}
		}

		initDSMap();
		initPipeline();

		statMonitor.setAppName(appName);
		statMonitor.start();
	}

	/**
	 * ʹ���ⲿ springContext ���������ļ�·����
	 *
	 * @param locations
	 */
	protected String[] doExternalResolve(String... locations) {
		if (this.springContext == null) {
			return this.appRuleFiles;
		}
		String[] externals = new String[locations.length];
		// Resolving appRuleFiles location by springContext.
		for (int i = 0; i < locations.length; i++) {
			try {
				URL appRuleRes = springContext.getResource(locations[i])
						.getURL();
				logger.info("Resolving file: " + locations[i] + " --> "
						+ appRuleRes);
				externals[i] = appRuleRes.toExternalForm();
			} catch (IOException e) {
				logger.warn("error on resolving file: " + locations[i]);
				externals[i] = locations[i];
			}
		}
		return externals;
	}

	/**
	 * ���ڲ���,����Ҫ�����κ����ԣ�new֮��ֱ�ӵ��ø÷�����ɳ�ʼ��
	 */
	public void init(Map<String, DataSource> dataSourcePool,
			VirtualTableRoot vtr) {
		this.rwDataSourcePoolConfig = dataSourcePool;
		init(vtr);
		initDSMap();
		initPipeline();
	}

	private void initPipeline() {
		if (null == pipelineFactory) {
	        if(dynamicRule&&tddlRule!=null){
				pipelineFactory= new RuleLePipelineFactory(tddlRule);
			}else if(useNewRule){
				pipelineFactory = new NewRulePipelineFactory();
			}else{
				pipelineFactory = new DefaultPipelineFactory();
			}
		}
		pipelineFactory.setDefaultDispatcher(defaultDispatcher);
		pipelineFactory.setDispatcherMap(dispatcherMap);

		// ��ʼ��SQL����ִ�п�����
		parallelManager = new ParallelDiamondConfigManager(appName);

		// �ṩ��getDBAndTables()ʹ��,��Ӱ������ʹ��
		if (null != defaultDispatcher) {
			this.defaultDispatcher.setPipelineFactory(pipelineFactory);
		}
		// modified by shen.���һ��appName��ע�ᡣ������statlog�оͿ���ֱ��ʹ��add���������ô�Ĵ���
		Monitor.setAppName(appName);
	}

	/**
	 * ��ʼ��Spring�����ļ�
	 *
	 * @param appRuleFiles
	 */
	private void initBySpringBaseAppRuleFile(String[] appRuleFiles) {
		FileSystemXmlApplicationContext ctx = null;
		if (null != this.specifyClassLoader) {
			ctx = new FileSystemXmlApplicationContext(appRuleFiles) {
				@Override
				public ClassLoader getClassLoader() {
					return specifyClassLoader;
				}
			};
		} else {
			ctx = new FileSystemXmlApplicationContext(appRuleFiles);
		}
		this.initXmlAppRule(ctx);
	}

	/**
	 * ��ʼ��Properties�����ļ�
	 *
	 * @param propertyBaseRuleFile
	 */
	private void initByPropertyBaseAppRuleFile(String propertyBaseRuleFile) {
		Properties prop = new Properties();
		try {
			InputStream is=null;
			if (null == springContext) {
				is= TDataSourceConfig.class.getClassLoader()
						.getResourceAsStream(propertyBaseRuleFile);
			} else {
				is = springContext
						.getResource(propertyBaseRuleFile).getInputStream();
			}
			prop.load(is);
		} catch (Exception e) {
			throw new IllegalStateException("��ȡproperty�����ļ�����", e);
		}
		if (prop.get(PropertyBaseTDDLRoot.TABLE_RULES) != null) {
			// TODO
			// ������Щ�߼����¹���һ���ŵ�PropertiesConfigParser�У�PropertyBaseTDDLRoot�����ɵ���
			AppRule appRule = new AppRule();
			PropertyBaseTDDLRoot root = new PropertyBaseTDDLRoot();
			root.init(prop);
			appRule.setDefaultTddlRoot(root);
			this.init(appRule);
		} else if (prop
				.getProperty(PropertiesConfigParser.Prop_Key_244_tableRules) != null) {
			// �¹���
			VirtualTableRoot vtr = PropertiesConfigParser
					.parseVirtualTableRoot(prop);
			this.init(vtr);
		} else {
			throw new IllegalStateException("No tableRules in properties:"
					+ propertyBaseRuleFile);
		}
	}

	/**
	 * ��ʼ��properties�����ַ���
	 *
	 * @param propertyBaseRuleFile
	 */
	private void initByPropertyBaseAppRuleString(String propertyRuleString) {
		AppRule appRule = new AppRule();
		PropertyBaseTDDLRoot root = new PropertyBaseTDDLRoot();
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
				propertyRuleString.getBytes());
		Properties prop = new Properties();
		try {
			prop.load(byteArrayInputStream);
		} catch (IOException e) {
			throw new IllegalStateException("��ȡproperty�����ļ�����");
		}
		root.init(prop);
		appRule.setDefaultTddlRoot(root);
		this.init(appRule);
	}

	/**
	 * ��ʼ��Xml�����ַ���
	 *
	 * @param propertyBaseRuleFile
	 */
	private void initByXmlBaseAppRuleString(String xmlRuleString) {
		StringXmlApplicationContext ctx = new StringXmlApplicationContext(
				xmlRuleString);
		initXmlAppRule(ctx);
	}

	/**
	 * ��ʼ��ApplicationContext��AppRule
	 * @param ctx
	 */
	private void initXmlAppRule(ApplicationContext ctx){
		if (ctx.containsBean("root")) {
			AppRule appRule = (AppRule) ctx.getBean("root");
			this.init(appRule);
		} else if (ctx.containsBean("vtabroot")) {
			// 244�¹���
			VirtualTableRoot vtr = (VirtualTableRoot) ctx.getBean("vtabroot");
			TDDLMBean mbean = new TDDLMBean("TDDL 2.4.4 Rule Info");
			mbean.setAttribute("dbType", vtr.getDbType().toString());
			if (vtr.getDbIndexMap() != null) {
				mbean.setAttribute("dbIndexMap", vtr.getDbIndexMap());
			}
			if (vtr.getDefaultDbIndex() != null) {
				mbean.setAttribute("defaultDBIndex", vtr.getDefaultDbIndex());
			}
			mbean.setAttribute("tableRules", vtr.getVirtualTableMap().toString());
			TDDLMBeanServer.registerMBeanWithId(mbean, "vtabroot");
			this.init(vtr);
		} else {
			TDDLRoot root = (TDDLRoot) ctx.getBean(ruleRootBeanID);
			TDDLMBean mbean = new TDDLMBean("Tddl Rule Info");
			if ( root.getDefaultDBSelectorID() != null) {
			    mbean.setAttribute("defaultDBIndex", root.getDefaultDBSelectorID());
			}
			mbean.setAttribute("dbType", root.getDBType().toString());
			mbean.setAttribute("logicTableMap", root.getLogicTableMap().toString());
			TDDLMBeanServer.registerMBean(mbean, "default rule");
			AppRule appRule = new AppRule();
			appRule.setDefaultTddlRoot(root);
			this.init(appRule);
		}
	}


	/**
	 * ����ShardRule��ʼ��Dispatcher: ��master�ֿ���򣬳�ʼ��ΪwriteDispatcher;
	 * ��slave�ֿ���򣬳�ʼ��ΪreadDispatcher;
	 */
	private void init(AppRule appRule) {
		SQLParser parser = new SQLParserImp();

		TDataSourceConfigHolder.setApplicationContext(this.springContext);

		appRule.init();

		TDataSourceConfigHolder.setApplicationContext(null);

		defaultDispatcher = buildSqlDispatcher(parser,
				appRule.getDefaultTddlRoot());

		dispatcherMap = new HashMap<String, SqlDispatcher>(4);

		for (Entry<String, TDDLRoot> entry : appRule.getRootMap().entrySet()) {
			TDDLMBean mbean = new TDDLMBean("Tddl Rule Info");
			if (entry.getValue().getDefaultDBSelectorID() != null) {
				mbean.setAttribute("defaultDBIndex", entry.getValue()
						.getDefaultDBSelectorID());
			}
			mbean.setAttribute("dbType", entry.getValue().getDBType().toString());
			mbean.setAttribute("logicTableMap", entry.getValue()
					.getLogicTableMap().toString());
			TDDLMBeanServer.registerMBean(mbean, entry.getKey());
			dispatcherMap.put(entry.getKey(),
					buildSqlDispatcher(parser, entry.getValue()));
		}
	}


	private void init(VirtualTableRoot vtr) {
		SQLParser parser = new SQLParserImp();
		defaultDispatcher = buildSqlDispatcher(parser, vtr);
		dispatcherMap = new HashMap<String, SqlDispatcher>(1);
		// ���ݸ���selectKeyȡdispatcher
		dispatcherMap.put("master", defaultDispatcher);
		this.useNewRule = true;
	}

	private static SpringBasedDispatcherImpl buildSqlDispatcher(
			SQLParser parser, TDDLRoot tddlRoot) {
		if (tddlRoot != null) {
			SpringBasedDispatcherImpl dispatcher = new SpringBasedDispatcherImpl();
			dispatcher.setParser(parser);
			dispatcher.setRoot(tddlRoot);
			return dispatcher;
		} else {
			return null;
		}
	}

	private static SpringBasedDispatcherImpl buildSqlDispatcher(
			SQLParser parser, VirtualTableRoot vtabroot) {
		if (vtabroot != null) {
			SpringBasedDispatcherImpl dispatcher = new SpringBasedDispatcherImpl();
			dispatcher.setParser(parser);
			dispatcher.setVtabroot(vtabroot);
			return dispatcher;
		} else {
			return null;
		}
	}

	private Map<String, DataSource> initDSMap() {
		Map<String, DataSource> dsMap = null;
		if (this.rwDataSourcePoolConfig != null) {
			logger.warn("init data source map by local config ,ds map :["
					+ dsMap + "]");
			dsMap = this.buildDbSelectors(this.rwDataSourcePoolConfig);
		} else {
			if (dataSourceMatrixCreator == null) {
				logger.warn("doesn't specfic datasource Matrix creator, use default ");
				dataSourceMatrixCreator = new DataSourceMatrixCreatorImp(dataSourceType);
			}
			if (appName == null || "".equals(appName)) {
				throw new IllegalArgumentException(
						"���û��ָ��rwDatasource,��ô�����������ȥȡ����˱���ָ��appName.");
			}
			dataSourceMatrixCreator.setNewDSMatrixKey(appName);
			dsMap = dataSourceMatrixCreator.getDataSourceMap();
			dataSourceMatrixCreator.addPropertiesChangeListener(this);
		}
		// return dsMap;

		if (dispatcherMap != null) {
			for (String key : dispatcherMap.keySet()) {
				if (dsMap.containsKey(key)) {
					throw new IllegalArgumentException(
							"����Դ�е�key����������ƶ���key��ͬ����ͬ��key�ǣ�" + key);
				}
			}
		} else {
			logger.warn("dispatcher Map is null");
		}

		this.runtimeConfigHolder.set(new TddlRuntime(dsMap));
		return dsMap;
	}

	private DataSourceFetcher springDataSourceFetcher = null;

	private Map<String, DataSource> buildDbSelectors(
			Map<String, ? extends Object> dataSourcePool) {
		Map<String, DataSource> dsMap = new HashMap<String, DataSource>();
		for (Map.Entry<String, ? extends Object> e : dataSourcePool.entrySet()) {
			String dsKey = e.getKey();
			if (e.getValue() instanceof DataSource) {
				DataSourceConfig dbConfig = new DataSourceConfig();
				dbConfig.setDsObject((DataSource) e.getValue());
				dsMap.put(dsKey, (DataSource) e.getValue());
			} else if (e.getValue() instanceof String) {
				if (springDataSourceFetcher == null) {
					springDataSourceFetcher = new DataSourceFetcher() {
						@Override
						public DataSource getDataSource(String key) {
							return (DataSource) TDataSourceConfig.this.springContext
									.getBean(key);
						}

						@Override
						public DBType getDataSourceDBType(String key) {
							return dbType;
						}
					};
				}
				dsMap.put(dsKey, TGroupDataSource.build(dsKey,
						(String) e.getValue(), springDataSourceFetcher, dataSourceType));
			} else if (e.getValue() instanceof DataSourceConfig) {
				dsMap.put(dsKey,
						((DataSourceConfig) e.getValue()).getDataSource());
			} else if (e.getValue() instanceof DataSourceConfig[]) {
				throw new IllegalArgumentException("not support");
			}
		}
		return Collections.unmodifiableMap(dsMap);
	}

	public TGroupDataSource buildGroupDS(String dbGroupKey,
			List<DataSourceWrapper> dss) {
		TGroupDataSource tGroupDataSource = new TGroupDataSource();
		tGroupDataSource.setDbGroupKey(dbGroupKey);
		tGroupDataSource.setDataSourceType(dataSourceType);
		tGroupDataSource.init(dss);
		return tGroupDataSource;
	}

	public void setDataSourcePool(Map<String, ? extends Object> dataSourcePool) {
		this.rwDataSourcePoolConfig = dataSourcePool;
	}

	public void setRwDataSourcePool(Map<String, ? extends Object> dataSourcePool) {
		this.rwDataSourcePoolConfig = dataSourcePool;
	}

	public void setRuleUrl(String ruleUrl) {
		throw new IllegalArgumentException(
				"��ʵ���н�����֧�־��й������ϣ��ʹ�þ��й�����ѡ��2.1.9��Ʒ��");
	}

	public void setDefaultDbType(String defaultDbType) {
		this.defaultDbType = DBType.valueOf(defaultDbType);
	}

	public boolean isEnableProfileRealDBAndTables() {
		return enableProfileRealDBAndTables;
	}

	public void setEnableProfileRealDBAndTables(
			boolean enableProfileRealDBAndTables) {
		this.enableProfileRealDBAndTables = enableProfileRealDBAndTables;
	}

	public void setReadOnly(boolean isReadOnly) {
		this.isReadOnly = isReadOnly;
	}

	public void setMasterOnly(boolean isMasterOnly) {
		this.isMasterOnly = isMasterOnly;
	}

	public void setSlaveOnly(boolean isSlaveOnly) {
		this.isSlaveOnly = isSlaveOnly;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	/**
	 * ������setter
	 *
	 * @param appName
	 */
	public void setSlaveDispatcherRuleUrl(String[] appName) {
		setAppRuleFiles(appName);
	}

	/**
	 * ������setter
	 *
	 * @param appName
	 */
	public void setMasterDispatcherRuleUrl(String[] appName) {
		setAppRuleFiles(appName);
	}

	/**
	 * ������setter
	 *
	 * @param masterSlaveDispatcherRuleUrl
	 */
	public void setMasterSlaveDispatcherRuleUrl(
			String[] masterSlaveDispatcherRuleUrl) {
		setAppRuleFiles(masterSlaveDispatcherRuleUrl);
	}

	public void setAppRuleFile(String appRuleFile) {
		this.appRuleFiles = appRuleFile.split(",");
	}

	public void setAppRuleFiles(String[] appRuleFiles) {
		this.appRuleFiles = appRuleFiles;
	}

	public void setUseLocalConfig(boolean isUseLocalConfig) {
		this.isUseLocalConfig = isUseLocalConfig;
	}

	public boolean isUseLocalConfig() {
		return isUseLocalConfig;
	}

	public boolean isHandleReplication() {
		return isHandleReplication;
	}

	public void setHandleReplication(boolean isHandleReplication) {
		this.isHandleReplication = isHandleReplication;
	}

	public void setHookPoints(HookPoints hookPoints) {
		this.hookPoints = hookPoints;
	}

	public HookPoints getHookPoints() {
		return hookPoints;
	}

	public Map<String, SqlDispatcher> getDispatcherMap() {
		return dispatcherMap;
	}

	public void setDispatcherMap(Map<String, SqlDispatcher> dispatcherMap) {
		this.dispatcherMap = dispatcherMap;
	}

	public SqlDispatcher getDefaultDispatcher() {
		return defaultDispatcher;
	}

	public void setDefaultDispatcher(SqlDispatcher defaultDispatcher) {
		this.defaultDispatcher = defaultDispatcher;
	}

	public RuntimeConfigHolder<TddlRuntime> getRuntimeConfigHolder() {
		return runtimeConfigHolder;
	}

	public String getRuleUrl() {
		return ruleUrl;
	}

	public DBType getDbType() {
		return dbType;
	}

	public void setDbType(DBType dbType) {
		this.dbType = dbType;
	}

	public String getWriteRuleRootBeanID() {
		return ruleRootBeanID;
	}

	public void setWriteRuleRootBeanID(String writeRuleRootBeanID) {
		this.ruleRootBeanID = writeRuleRootBeanID;
	}

	public String getReadRuleRootBeanID() {
		return ruleRootBeanID;
	}

	public void setReadRuleRootBeanID(String readRuleRootBeanID) {
		this.ruleRootBeanID = readRuleRootBeanID;
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.springContext = applicationContext;
	}

	public ApplicationContext getSpringContext() throws BeansException {
		return this.springContext;
	}

	@Override
	public synchronized void onDataSourceChanged(
			Map<String, DataSource> newDataSourceMap) {
		if (this.rwDataSourcePoolConfig != null) {
			logger.warn("receive a set of ds map . but TDDL DS has already using local rwDataSourcePool.abandon! ");
			logger.warn("ds map to be abandoned:" + newDataSourceMap);
		} else {
			this.runtimeConfigHolder.set(new TddlRuntime(newDataSourceMap));
		}
	}

	public void setPipelineFactory(PipelineFactory pipelineFactory) {
		this.pipelineFactory = pipelineFactory;
	}

	public PipelineFactory getPipelineFactory() {
		return pipelineFactory;
	}

	public String getAppRulePropertiesString() {
		return appRulePropertiesString;
	}

	public void setAppRulePropertiesString(String appRulePropertiesString) {
		this.appRulePropertiesString = appRulePropertiesString;
	}

	public void setDynamicRule(boolean dynamicRule) {
		this.dynamicRule = dynamicRule;
	}

	public void setAppRuleString(String appRuleString) {
		this.appRuleString = RuleLeBeanConvert.convertLeAndOriRuleStr2InteractRule(appRuleString);;
	}

	public void setShutDownMBean(boolean shutDownMBean) {
		TDDLMBeanServer.shutDownMBean = shutDownMBean;
	}

	public void addOuterListener(RuleChangeListener listener){
		this.outListeners.add(listener);
	}

	@Override
	public void onRuleRecieve(String ruleString) {
		String tddlRuleStr=RuleLeBeanConvert.convertLeAndOriRuleStr2InteractRule(ruleString);
		//local rule bean should be re-inited
		this.initByXmlBaseAppRuleString(tddlRuleStr);
	}

	public void setSpecifyClassLoader(ClassLoader specifyClassLoader) {
		this.specifyClassLoader = specifyClassLoader;
	}

	public void setTddlRule(TddlRuleInner tddlRule) {
		this.tddlRule = tddlRule;
	}

	public DataSourceType getDataSourceType() {
		return dataSourceType;
	}

	public void setDataSourceType(DataSourceType dataSourceType) {
		this.dataSourceType = dataSourceType;
	}
	public void destroyDataSource()
	{
		dataSourceMatrixCreator.destroyAll();
	}

}
