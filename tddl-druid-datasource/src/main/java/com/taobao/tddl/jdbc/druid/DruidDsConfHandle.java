package com.taobao.tddl.jdbc.druid;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.common.lang.StringUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.taobao.tddl.common.Monitor;
import com.taobao.tddl.common.config.ConfigDataListener;
import com.taobao.tddl.jdbc.druid.common.DruidConURLTools;
import com.taobao.tddl.jdbc.druid.common.DruidConfParser;
import com.taobao.tddl.jdbc.druid.common.DruidConstants;
import com.taobao.tddl.jdbc.druid.config.DbConfManager;
import com.taobao.tddl.jdbc.druid.config.DbPasswdManager;
import com.taobao.tddl.jdbc.druid.config.DiamondDbConfManager;
import com.taobao.tddl.jdbc.druid.config.DiamondDbPasswdManager;
import com.taobao.tddl.jdbc.druid.config.object.DruidDbStatusEnum;
import com.taobao.tddl.jdbc.druid.config.object.DruidDbTypeEnum;
import com.taobao.tddl.jdbc.druid.config.object.DruidDsConfDO;
import com.taobao.tddl.jdbc.druid.exception.DruidAlreadyInitException;
import com.taobao.tddl.jdbc.druid.exception.DruidIllegalException;
import com.taobao.tddl.jdbc.druid.exception.DruidInitialException;
import com.taobao.tddl.jdbc.druid.jdbc.TDataSourceWrapper;
import com.taobao.tddl.jdbc.druid.listener.DruidDbStatusListener;

/**
 * ���ݿ⶯̬�л���Handle�࣬�������ݿ�Ķ�̬�л� ��������������
 * 
 * @author qihao
 * 
 */
class DruidDsConfHandle {
	private static Log logger = LogFactory.getLog(DruidDsConfHandle.class);

	private String appName;

	private String dbKey;

	/**
	 * ����ʱ����
	 */
	private volatile DruidDsConfDO runTimeConf = new DruidDsConfDO();

	/**
	 * �������ã����������͵Ķ�̬����
	 */
	private DruidDsConfDO localConf = new DruidDsConfDO();

	/**
	 * ȫ�����ã�Ӧ�����ö��Ĺ���
	 */
	private DbConfManager dbConfManager;

	/**
	 * �������ö��Ĺ���
	 */
	private DbPasswdManager dbPasswdManager;

	/**
	 * druid����Դͨ��init��ʼ��
	 */
	private volatile DruidDataSource druidDataSource;

	/**
	 * ���ݿ�״̬�ı�ص�
	 */
	private volatile List<DruidDbStatusListener> dbStatusListeners;

	/**
	 * ��ʼ�����Ϊһ����ʼ���������б��ص����ý�ֹ�Ķ�
	 */
	private volatile boolean initFalg;

	/**
	 * ����Դ������������Ҫ������Դ�����ؽ�����ˢ��ʱ��Ҫ�Ȼ�ø���
	 */
	private final ReentrantLock lock = new ReentrantLock();

	/**
	 * ��ʼ��������������Ӧ������Դ��ֻ�ܱ�����һ��
	 * 
	 * @throws Exception
	 */
	protected void init() throws Exception {
		if (initFalg) {
			throw new DruidAlreadyInitException(
					"[AlreadyInit] double call Init !");
		}
		// 1.��ʼ���������
		if (StringUtil.isBlank(this.appName) || StringUtil.isBlank(this.dbKey)) {
			String errorMsg = "[attributeError] TAtomDatasource of appName Or dbKey is Empty !";
			logger.error(errorMsg);
			throw new DruidIllegalException(errorMsg);
		}
		// 2.����dbConfManager
		DiamondDbConfManager defaultDbConfManager = new DiamondDbConfManager();
		defaultDbConfManager.setGlobalConfigDataId(DruidConstants
				.getGlobalDataId(this.dbKey));
		defaultDbConfManager.setAppConfigDataId(DruidConstants.getAppDataId(
				this.appName, this.dbKey));
		// ��ʼ��dbConfManager
		defaultDbConfManager.init();
		dbConfManager = defaultDbConfManager;
		// 3.��ȡȫ������
		String globaConfStr = dbConfManager.getGlobalDbConf();
		// ע��ȫ�����ü���
		registerGlobaDbConfListener(defaultDbConfManager);
		if (StringUtil.isBlank(globaConfStr)) {
			String errorMsg = "[ConfError] read globalConfig is Empty !";
			logger.error(errorMsg);
			throw new DruidInitialException(errorMsg);
		}
		// 4.��ȡӦ������
		String appConfStr = dbConfManager.getAppDbDbConf();
		// ע��Ӧ�����ü���
		registerAppDbConfListener(defaultDbConfManager);
		if (StringUtil.isBlank(appConfStr)) {
			String errorMsg = "[ConfError] read appConfig is Empty !";
			logger.error(errorMsg);
			throw new DruidInitialException(errorMsg);
		}
		lock.lock();
		try {
			// 5.��������string��TAtomDsConfDO
			runTimeConf = DruidConfParser.parserTAtomDsConfDO(globaConfStr,
					appConfStr);
			// 6.��������������
			overConfByLocal(localConf, runTimeConf);
			// 7.���û�����ñ������룬���ö������룬��ʼ��passwdManager
			if (StringUtil.isBlank(this.runTimeConf.getPasswd())) {
				// ���dbKey�Ͷ�Ӧ��userName�Ƿ�Ϊ��
				if (StringUtil.isBlank(runTimeConf.getUserName())) {
					String errorMsg = "[attributeError] TAtomDatasource of UserName is Empty !";
					logger.error(errorMsg);
					throw new DruidIllegalException(errorMsg);
				}
				DiamondDbPasswdManager diamondDbPasswdManager = new DiamondDbPasswdManager();
				diamondDbPasswdManager.setPasswdConfDataId(DruidConstants
						.getPasswdDataId(runTimeConf.getDbName(),
								runTimeConf.getDbType(),
								runTimeConf.getUserName()));
				diamondDbPasswdManager.init();
				dbPasswdManager = diamondDbPasswdManager;
				// ��ȡ����
				String passwd = dbPasswdManager.getPasswd();
				registerPasswdConfListener(diamondDbPasswdManager);
				if (StringUtil.isBlank(passwd)) {
					String errorMsg = "[PasswdError] read passwd is Empty !";
					logger.error(errorMsg);
					throw new DruidInitialException(errorMsg);
				}
				runTimeConf.setPasswd(passwd);
			}
			// 8.ת��tAtomDsConfDO
			DruidDataSource druidDataSource = convertTAtomDsConf2DruidConf(
					this.runTimeConf,
					DruidConstants.getDbNameStr(this.appName, this.dbKey));
			// 9.������������������ȷֱ���׳��쳣
			if (!checkLocalTxDataSourceDO(druidDataSource)) {
				String errorMsg = "[ConfigError]init dataSource Prams Error! config is : "
						+ druidDataSource.toString();
				logger.error(errorMsg);
				throw new DruidInitialException(errorMsg);
			}
//			 10.��������Դ
//			druidDataSource.setUseJmx(false);
//			LocalTxDataSource localTxDataSource = TaobaoDataSourceFactory
//					.createLocalTxDataSource(localTxDataSourceDO);
			//11.�������õ�����Դ��ָ��TAtomDatasource��
			druidDataSource.init();
			this.druidDataSource = druidDataSource;
			clearDataSourceWrapper();
			initFalg = true;
		} finally {
			lock.unlock();
		}
	}

	private void clearDataSourceWrapper() {
		Monitor.removeSnapshotValuesCallback(wrapDataSource);
		wrapDataSource = null;
	}

	/**
	 * ע������仯������
	 * 
	 * @param dbPasswdManager
	 */
	private void registerPasswdConfListener(DbPasswdManager dbPasswdManager) {
		dbPasswdManager.registerPasswdConfListener(new ConfigDataListener() {
			public void onDataRecieved(String dataId, String data) {
				logger.error("[Passwd HandleData] dataId : " + dataId
						+ " data: " + data);
				if (null == data || StringUtil.isBlank(data)) {
					return;
				}
				lock.lock();
				try {
					String localPasswd = DruidDsConfHandle.this.localConf
							.getPasswd();
					if (StringUtil.isNotBlank(localPasswd)) {
						// �������������passwdֱ�ӷ��ز�֧�ֶ�̬�޸�
						return;
					}
					String newPasswd = DruidConfParser.parserPasswd(data);
					String runPasswd = DruidDsConfHandle.this.runTimeConf
							.getPasswd();
					if (!StringUtil.equals(runPasswd, newPasswd)) {
						try {
							DruidDataSource newDruidDataSource = DruidDsConfHandle.this.druidDataSource.cloneDruidDataSource();
							newDruidDataSource.setPassword(newPasswd);
							newDruidDataSource.init();
							DruidDataSource tempDataSource = DruidDsConfHandle.this.druidDataSource;
							DruidDsConfHandle.this.druidDataSource = newDruidDataSource;
							tempDataSource.close();
							logger.warn("[DRUID CHANGE PASSWORD] ReCreate DataSource !");
							// �����µ����ø�������ʱ������
							clearDataSourceWrapper();
							DruidDsConfHandle.this.runTimeConf
									.setPasswd(newPasswd);
						} catch (Exception e) {
							logger.error(
									"[DRUID CHANGE PASSWORD] ReCreate DataSource Error!",
									e);
						}
					}
				} finally {
					lock.unlock();
				}
			}
		});
	}

	/**
	 * ȫ�����ü���,ȫ�����÷����仯�� ��Ҫ����FLUSH����Դ
	 * 
	 * @param defaultDbConfManager
	 */
	private void registerGlobaDbConfListener(DbConfManager dbConfManager) {
		dbConfManager.registerGlobaDbConfListener(new ConfigDataListener() {
			public void onDataRecieved(String dataId, String data) {
				logger.error("[DRUID GlobaConf HandleData] dataId : " + dataId
						+ " data: " + data);
				if (null == data || StringUtil.isBlank(data)) {
					return;
				}
				lock.lock();
				try {
					String globaConfStr = data;
					// �����ȫ�����÷����仯��������IP,PORT,DBNAME,DBTYPE,STATUS
					DruidDsConfDO tmpConf = DruidConfParser
							.parserTAtomDsConfDO(globaConfStr, null);
					DruidDsConfDO newConf = DruidDsConfHandle.this.runTimeConf
							.clone();
					// �������͵����ã����ǵ�ǰ������
					newConf.setIp(tmpConf.getIp());
					newConf.setPort(tmpConf.getPort());
					newConf.setDbName(tmpConf.getDbName());
					newConf.setDbType(tmpConf.getDbType());
					newConf.setDbStatus(tmpConf.getDbStatus());
					// ��������������
					overConfByLocal(DruidDsConfHandle.this.localConf, newConf);
					// ������͹��������ݿ�״̬�� RW/R->NA,ֱ�����ٵ�����Դ������ҵ���߼���������
					if (DruidDbStatusEnum.NA_STATUS != DruidDsConfHandle.this.runTimeConf
							.getDbStautsEnum()
							&& DruidDbStatusEnum.NA_STATUS == tmpConf
									.getDbStautsEnum()) {
						try {
							DruidDsConfHandle.this.druidDataSource.close();
							logger.warn("[DRUID NA STATUS PUSH] destroy DataSource !");
						} catch (Exception e) {
							logger.error(
									"[DRUID NA STATUS PUSH] destroy DataSource  Error!",
									e);
						}
					} else {
						// ת��tAtomDsConfDO
						DruidDataSource druidDataSource;
						try {
							druidDataSource = convertTAtomDsConf2DruidConf(
									newConf, DruidConstants.getDbNameStr(
											DruidDsConfHandle.this.appName,
											DruidDsConfHandle.this.dbKey));
						} catch (Exception e1) {
							logger.error("[DRUID GlobaConfError] convertTAtomDsConf2DruidConf Error! dataId : "
									+ dataId + " config : " + data);
							return;
						}
						// ���ת�������Ƿ���ȷ
						if (!checkLocalTxDataSourceDO(druidDataSource)) {
							logger.error("[DRUID GlobaConfError] dataSource Prams Error! dataId : "
									+ dataId + " config : " + data);
							return;
						}
						// ������͵�״̬ʱ NA->RW/R ʱ��Ҫ���´�������Դ��������ˢ��
						if (DruidDsConfHandle.this.runTimeConf
								.getDbStautsEnum() == DruidDbStatusEnum.NA_STATUS
								&& (newConf.getDbStautsEnum() == DruidDbStatusEnum.RW_STATUS
										|| newConf.getDbStautsEnum() == DruidDbStatusEnum.R_STATUS || newConf
										.getDbStautsEnum() == DruidDbStatusEnum.W_STATUS)) {
							// ��������Դ
							try {
								// �ر�TB-DATASOURCE��JMXע��
								// localTxDataSourceDO.setUseJmx(false);
								// LocalTxDataSource localTxDataSource =
								// TaobaoDataSourceFactory
								// .createLocalTxDataSource(localTxDataSourceDO);
								druidDataSource.init();
								DruidDataSource tempDataSource = DruidDsConfHandle.this.druidDataSource;
								DruidDsConfHandle.this.druidDataSource = druidDataSource;
								tempDataSource.close();
								logger.warn("[DRUID NA->RW/R STATUS PUSH] ReCreate DataSource !");
							} catch (Exception e) {
								logger.error(
										"[DRUID NA->RW/R STATUS PUSH] ReCreate DataSource Error!",
										e);
							}
						} else {
							boolean needCreate = checkGlobaConfChange(
									DruidDsConfHandle.this.runTimeConf, newConf);
							// ������������ñ仯�Ƿ���Ҫ�ؽ�����Դ
							// druid û��flush������ֻ���ؽ�����Դ jiechen.qzm
							if (needCreate) {
								try {
									// ��������Դ
									druidDataSource.init();
									DruidDataSource tempDataSource = DruidDsConfHandle.this.druidDataSource;
									DruidDsConfHandle.this.druidDataSource = druidDataSource;
									tempDataSource.close();
									logger.warn("[DRUID CONFIG CHANGE STATUS] Always ReCreate DataSource !");
								} catch (Exception e) {
									logger.error(
											"[DRUID Create GlobaConf Error]  Always ReCreate DataSource Error !",
											e);
								}
							}
						}
					}
					//�������ݿ�״̬������
					processDbStatusListener(DruidDsConfHandle.this.runTimeConf.getDbStautsEnum(),
							newConf.getDbStautsEnum());
					//�����µ����ø�������ʱ������
					DruidDsConfHandle.this.runTimeConf = newConf;
					clearDataSourceWrapper();
				} finally {
					lock.unlock();
				}
			}

			private boolean checkGlobaConfChange(DruidDsConfDO runConf,
					DruidDsConfDO newConf) {
				boolean needFlush = false;
				if (!StringUtil.equals(runConf.getIp(), newConf.getIp())) {
					needFlush = true;
					return needFlush;
				}
				if (!StringUtil.equals(runConf.getPort(), newConf.getPort())) {
					needFlush = true;
					return needFlush;
				}
				if (!StringUtil.equals(runConf.getDbName(), newConf.getDbName())) {
					needFlush = true;
					return needFlush;
				}
				if (runConf.getDbTypeEnum() != newConf.getDbTypeEnum()) {
					needFlush = true;
					return needFlush;
				}
				return needFlush;
			}
		});
	}

	/**
	 * Ӧ�����ü�������Ӧ�����÷����仯ʱ�����ַ��� �仯�����ã�������������flush����reCreate
	 * 
	 * @param defaultDbConfManager
	 */
	private void registerAppDbConfListener(DbConfManager dbConfManager) {
		dbConfManager.registerAppDbConfListener(new ConfigDataListener() {
			public void onDataRecieved(String dataId, String data) {
				logger.error("[DRUID AppConf HandleData] dataId : " + dataId
						+ " data: " + data);
				if (null == data || StringUtil.isBlank(data)) {
					return;
				}
				lock.lock();
				try {
					String appConfStr = data;
					DruidDsConfDO tmpConf = DruidConfParser
							.parserTAtomDsConfDO(null, appConfStr);
					DruidDsConfDO newConf = DruidDsConfHandle.this.runTimeConf
							.clone();
					// ��Щ�������ò��ܱ�������Կ�¡�ϵ����ã�Ȼ���µ�set��ȥ
					newConf.setUserName(tmpConf.getUserName());
					newConf.setMinPoolSize(tmpConf.getMinPoolSize());
					newConf.setMaxPoolSize(tmpConf.getMaxPoolSize());
					newConf.setIdleTimeout(tmpConf.getIdleTimeout());
					newConf.setBlockingTimeout(tmpConf.getBlockingTimeout());
					newConf.setPreparedStatementCacheSize(tmpConf
							.getPreparedStatementCacheSize());
					newConf.setConnectionProperties(tmpConf
							.getConnectionProperties());
					newConf.setOracleConType(tmpConf.getOracleConType());
					// ����3�������ʵ��
					newConf.setWriteRestrictTimes(tmpConf
							.getWriteRestrictTimes());
					newConf.setReadRestrictTimes(tmpConf.getReadRestrictTimes());
					newConf.setThreadCountRestrict(tmpConf
							.getThreadCountRestrict());
					newConf.setTimeSliceInMillis(tmpConf.getTimeSliceInMillis());
					// ��������������
					overConfByLocal(DruidDsConfHandle.this.localConf, newConf);
					// ת��tAtomDsConfDO
					DruidDataSource druidDataSource;
					try {
						druidDataSource = convertTAtomDsConf2DruidConf(
								newConf, DruidConstants.getDbNameStr(
										DruidDsConfHandle.this.appName,
										DruidDsConfHandle.this.dbKey));
					} catch (Exception e1) {
						logger.error("[DRUID GlobaConfError] convertTAtomDsConf2DruidConf Error! dataId : "
								+ dataId + " config : " + data);
						return;
					}
					// ���ת�������Ƿ���ȷ
					if (!checkLocalTxDataSourceDO(druidDataSource)) {
						logger.error("[DRUID GlobaConfError] dataSource Prams Error! dataId : "
								+ dataId + " config : " + data);
						return;
					}
					boolean isNeedReCreate = isNeedReCreate(
							DruidDsConfHandle.this.runTimeConf, newConf);
					if (isNeedReCreate) {
						try {
							DruidDsConfHandle.this.druidDataSource.close();
							logger.warn("[DRUID destroy OldDataSource] dataId : "
									+ dataId);
							druidDataSource.init();
							logger.warn("[DRUID create newDataSource] dataId : "
									+ dataId);
							DruidDsConfHandle.this.druidDataSource = druidDataSource;
							clearDataSourceWrapper();
							DruidDsConfHandle.this.runTimeConf = newConf;
						} catch (Exception e) {
							logger.error(
									"[DRUID Create GlobaConf Error]  Always ReCreate DataSource Error ! dataId: "
											+ dataId, e);
						}
					} else {
						boolean needCreate = isNeedFlush(
								DruidDsConfHandle.this.runTimeConf, newConf);
						/**
						 * ��ֵ�仯����ˢ�³��е�����Դ��ֻҪ����runTimeConf���������wrapDataSource
						 */
						boolean isRestrictChange = isRestrictChange(
								DruidDsConfHandle.this.runTimeConf, newConf);
						if (needCreate) {
							try {
								druidDataSource.init();
								logger.warn("[DRUID create newDataSource] dataId : "
										+ dataId);
								DruidDataSource tempDataSource = DruidDsConfHandle.this.druidDataSource;
								DruidDsConfHandle.this.druidDataSource = druidDataSource;
								tempDataSource.close();
								logger.warn("[DRUID destroy OldDataSource] dataId : "
										+ dataId);
								clearDataSourceWrapper();
								DruidDsConfHandle.this.runTimeConf = newConf;
							} catch (Exception e) {
								logger.error(
										"[DRUID Create GlobaConf Error]  Always ReCreate DataSource Error !",
										e);
							}
						} else if (isRestrictChange) {
							DruidDsConfHandle.this.runTimeConf = newConf;
							clearDataSourceWrapper();
						}
					}
				} finally {
					lock.unlock();
				}
			}

			private boolean isNeedReCreate(DruidDsConfDO runConf,
					DruidDsConfDO newConf) {
				boolean needReCreate = false;
				if (DruidDbTypeEnum.ORACLE == newConf.getDbTypeEnum()) {
					Map<String, String> newProp = newConf
							.getConnectionProperties();
					Map<String, String> runProp = runConf
							.getConnectionProperties();
					if (!runProp.equals(newProp)) {
						return true;
					}
				}
				if (runConf.getMinPoolSize() != newConf.getMinPoolSize()) {
					return true;
				}
				if (runConf.getMaxPoolSize() != newConf.getMaxPoolSize()) {
					return true;
				}
				if (runConf.getBlockingTimeout() != newConf
						.getBlockingTimeout()) {
					return true;
				}
				if (runConf.getIdleTimeout() != newConf.getIdleTimeout()) {
					return true;
				}
				if (runConf.getPreparedStatementCacheSize() != newConf
						.getPreparedStatementCacheSize()) {
					return true;
				}
				return needReCreate;
			}

			private boolean isNeedFlush(DruidDsConfDO runConf,
					DruidDsConfDO newConf) {
				boolean needFlush = false;
				if (DruidDbTypeEnum.MYSQL == newConf.getDbTypeEnum()) {
					Map<String, String> newProp = newConf
							.getConnectionProperties();
					Map<String, String> runProp = runConf
							.getConnectionProperties();
					if (!runProp.equals(newProp)) {
						return true;
					}
				}
				if (!StringUtil.equals(runConf.getUserName(),
						newConf.getUserName())) {
					return true;
				}
				if (!StringUtil.equals(runConf.getPasswd(), newConf.getPasswd())) {
					return true;
				}
				return needFlush;
			}

			private boolean isRestrictChange(DruidDsConfDO runConf,
					DruidDsConfDO newConf) {
				if (runConf.getReadRestrictTimes() != newConf
						.getReadRestrictTimes()) {
					return true;
				}

				if (runConf.getWriteRestrictTimes() != newConf
						.getWriteRestrictTimes()) {
					return true;
				}

				if (runConf.getThreadCountRestrict() != newConf
						.getThreadCountRestrict()) {
					return true;
				}

				if (runConf.getTimeSliceInMillis() != newConf
						.getTimeSliceInMillis()) {
					return true;
				}

				return false;
			}
		});
	}

	/**
	 * ��TAtomDsConfDOת����LocalTxDataSourceDO
	 * 
	 * @param tAtomDsConfDO
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	protected static DruidDataSource convertTAtomDsConf2DruidConf(DruidDsConfDO tAtomDsConfDO, String dbName) throws Exception{
		DruidDataSource localDruidDataSource = new DruidDataSource();
//		if (StringUtil.isNotBlank(dbName)) {
//			localDruidDataSource.setJndiName(dbName);
//		}
		localDruidDataSource.setUsername(tAtomDsConfDO.getUserName());
		localDruidDataSource.setPassword(tAtomDsConfDO.getPasswd());
		localDruidDataSource.setDriverClassName(tAtomDsConfDO.getDriverClass());
		localDruidDataSource.setExceptionSorterClassName(tAtomDsConfDO.getSorterClass());
		//�������ݿ���������conURL��setConnectionProperties
		if (DruidDbTypeEnum.ORACLE == tAtomDsConfDO.getDbTypeEnum()) {
			String conUlr = DruidConURLTools.getOracleConURL(tAtomDsConfDO.getIp(), tAtomDsConfDO.getPort(),
					tAtomDsConfDO.getDbName(), tAtomDsConfDO.getOracleConType());
			localDruidDataSource.setUrl(conUlr);
			//�����oracleû������ConnectionProperties����Ը�Ĭ�ϵ�
			Properties connectionProperties = new Properties();
			if (!tAtomDsConfDO.getConnectionProperties().isEmpty()) {
				connectionProperties.putAll(tAtomDsConfDO.getConnectionProperties());
				
			} else {
				connectionProperties.putAll(DruidConstants.DEFAULT_ORACLE_CONNECTION_PROPERTIES);
			}
			localDruidDataSource.setConnectProperties(connectionProperties);
		} else if (DruidDbTypeEnum.MYSQL == tAtomDsConfDO.getDbTypeEnum()) {
			String conUlr = DruidConURLTools.getMySqlConURL(tAtomDsConfDO.getIp(), tAtomDsConfDO.getPort(),
					tAtomDsConfDO.getDbName(), tAtomDsConfDO.getConnectionProperties());
			localDruidDataSource.setUrl(conUlr);
			//��������ҵ�mysqlDriver�е�Valid��ʹ�ã���������valid
			try {
				//FIXME �쳣code��mysql�Ƿ�һ��
				Class validClass = Class.forName(DruidConstants.DEFAULT_DRUID_MYSQL_VALID_CONNECTION_CHECKERCLASS);
				if (null != validClass) {
					localDruidDataSource
							.setValidConnectionCheckerClassName(DruidConstants.DEFAULT_DRUID_MYSQL_VALID_CONNECTION_CHECKERCLASS);
				} else {
					logger.warn("MYSQL Driver is Not Suport "
							+ DruidConstants.DEFAULT_DRUID_MYSQL_VALID_CONNECTION_CHECKERCLASS);
				}
			} catch (ClassNotFoundException e) {
				logger.warn("MYSQL Driver is Not Suport " + DruidConstants.DEFAULT_DRUID_MYSQL_VALID_CONNECTION_CHECKERCLASS);
			} catch (NoClassDefFoundError e) {
				logger.warn("MYSQL Driver is Not Suport " + DruidConstants.DEFAULT_DRUID_MYSQL_VALID_CONNECTION_CHECKERCLASS);
			}
			
			//��������ҵ�mysqlDriver�е�integrationSorter��ʹ�÷���ʹ��Ĭ�ϵ�
			try {
				Class integrationSorterCalss = Class.forName(DruidConstants.DRUID_MYSQL_INTEGRATION_SORTER_CLASS);
				if (null != integrationSorterCalss) {
					localDruidDataSource.setExceptionSorterClassName(DruidConstants.DRUID_MYSQL_INTEGRATION_SORTER_CLASS);
				} else {
					localDruidDataSource.setExceptionSorterClassName(DruidConstants.DEFAULT_DRUID_MYSQL_SORTER_CLASS);
					logger.warn("MYSQL Driver is Not Suport " + DruidConstants.DRUID_MYSQL_INTEGRATION_SORTER_CLASS
							+ " use default sorter " + DruidConstants.DEFAULT_DRUID_MYSQL_SORTER_CLASS);
				}
			} catch (ClassNotFoundException e) {
				logger.warn("MYSQL Driver is Not Suport " + DruidConstants.DRUID_MYSQL_INTEGRATION_SORTER_CLASS
						+ " use default sorter " + DruidConstants.DEFAULT_DRUID_MYSQL_SORTER_CLASS);
			} catch (NoClassDefFoundError e){
				logger.warn("MYSQL Driver is Not Suport " + DruidConstants.DRUID_MYSQL_INTEGRATION_SORTER_CLASS
						+ " use default sorter " + DruidConstants.DEFAULT_DRUID_MYSQL_SORTER_CLASS);
			}
		}
		// lazy init ������Ϊ0 ��������ִ��ʱ�Ŵ�������
		localDruidDataSource.setInitialSize(tAtomDsConfDO.getInitPoolSize());
		localDruidDataSource.setMinIdle(tAtomDsConfDO.getMinPoolSize());
		localDruidDataSource.setMaxActive(tAtomDsConfDO.getMaxPoolSize());
		if(tAtomDsConfDO.getPreparedStatementCacheSize() > 0){
			localDruidDataSource.setPoolPreparedStatements(true);
			localDruidDataSource.setMaxPoolPreparedStatementPerConnectionSize(tAtomDsConfDO.getPreparedStatementCacheSize());
		}
		if (tAtomDsConfDO.getIdleTimeout() > 0) {
			localDruidDataSource.setTimeBetweenEvictionRunsMillis(tAtomDsConfDO.getIdleTimeout()*60*1000);
			localDruidDataSource.setMinEvictableIdleTimeMillis(tAtomDsConfDO.getIdleTimeout()*60*1000);
		}
		if (tAtomDsConfDO.getBlockingTimeout() > 0) {
			localDruidDataSource.setMaxWait(tAtomDsConfDO.getBlockingTimeout());
		}
		return localDruidDataSource;
	}

	protected static boolean checkLocalTxDataSourceDO(
			DruidDataSource druidDataSource) {
		if (null == druidDataSource) {
			return false;
		}

		if (StringUtil.isBlank(druidDataSource.getUrl())) {
			logger.error("[DsConfig Check] URL is Empty !");
			return false;
		}

		if (StringUtil.isBlank(druidDataSource.getUsername())) {
			logger.error("[DsConfig Check] Username is Empty !");
			return false;
		}

		if (StringUtil.isBlank(druidDataSource.getPassword())) {
			logger.error("[DsConfig Check] Password is Empty !");
			return false;
		}

		if (StringUtil.isBlank(druidDataSource.getDriverClassName())) {
			logger.error("[DsConfig Check] DriverClassName is Empty !");
			return false;
		}
		
//		if (druidDataSource.getInitialSize() < 1) {
//			logger.error("[DsConfig Check] InitialSize Error size is:"
//					+ druidDataSource.getInitialSize());
//			return false;
//		}

		if (druidDataSource.getMinIdle() < 1) {
			logger.error("[DsConfig Check] MinIdle Error size is:"
					+ druidDataSource.getMinIdle());
			return false;
		}
		
		if (druidDataSource.getMaxActive() < 1) {
			logger.error("[DsConfig Check] MaxActive Error size is:"
					+ druidDataSource.getMaxActive());
			return false;
		}

		if (druidDataSource.getMinIdle() > druidDataSource.getMaxActive()) {
			logger.error("[DsConfig Check] MinPoolSize Over MaxPoolSize Minsize is:"
					+ druidDataSource.getMinIdle()
					+ "MaxSize is :"
					+ druidDataSource.getMaxActive());
			return false;
		}
		return true;
	}

	/**
	 * ���ñ������ø��Ǵ����TAtomDsConfDO������
	 * 
	 * @param tAtomDsConfDO
	 */
	private void overConfByLocal(DruidDsConfDO localDsConfDO,
			DruidDsConfDO newDsConfDO) {
		if (null == newDsConfDO || null == localDsConfDO) {
			return;
		}
		if (StringUtil.isNotBlank(localDsConfDO.getDriverClass())) {
			newDsConfDO.setDriverClass(localDsConfDO.getDriverClass());
		}
		if (StringUtil.isNotBlank(localDsConfDO.getSorterClass())) {
			newDsConfDO.setSorterClass(localDsConfDO.getSorterClass());
		}
		if (StringUtil.isNotBlank(localDsConfDO.getPasswd())) {
			newDsConfDO.setPasswd(localDsConfDO.getPasswd());
		}
		if (null != localDsConfDO.getConnectionProperties()
				&& !localDsConfDO.getConnectionProperties().isEmpty()) {
			newDsConfDO.setConnectionProperties(localDsConfDO
					.getConnectionProperties());
		}
	}

	/**
	 * Datasource �İ�װ��
	 */
	private volatile TDataSourceWrapper wrapDataSource = null;

	public DataSource getDataSource() throws SQLException {
		if (wrapDataSource == null) {
			lock.lock();
			try {
				if (wrapDataSource != null) {
					// ˫�����
					return wrapDataSource;
				}
				String errorMsg = "";
				if (null == druidDataSource) {
					errorMsg = "[InitError] TAtomDsConfHandle maybe forget init !";
					logger.error(errorMsg);
					throw new SQLException(errorMsg);
				}
				DataSource dataSource = druidDataSource;
				if (null == dataSource) {
					errorMsg = "[InitError] TAtomDsConfHandle maybe init fail !";
					logger.error(errorMsg);
					throw new SQLException(errorMsg);
				}
				// ������ݿ�״̬������ֱ���׳��쳣
				if (null == this.getStatus()) {
					errorMsg = "[DB Stats Error] DbStatus is Null: "
							+ this.getDbKey();
					logger.error(errorMsg);
					throw new SQLException(errorMsg);
				}
				TDataSourceWrapper tDataSourceWrapper = new TDataSourceWrapper(
						dataSource, runTimeConf);
				tDataSourceWrapper.setDatasourceName(dbKey);
				tDataSourceWrapper.setDatasourceIp(runTimeConf.getIp());
				tDataSourceWrapper.setDatasourcePort(runTimeConf.getPort());
				tDataSourceWrapper.setDatasourceRealDbName(runTimeConf.getDbName());
				tDataSourceWrapper.setDbStatus(getStatus());
				logger.warn("set datasource key: " + dbKey);
				wrapDataSource = tDataSourceWrapper;

				return wrapDataSource;

			} finally {
				lock.unlock();
			}
		} else {
			return wrapDataSource;
		}
	}

	public void flushDataSource() {
		//��ʱ��֧��flush �״�
		logger.error("DRUID DATASOURCE DO NOT SUPPORT FLUSH.");
		throw new RuntimeException("DRUID DATASOURCE DO NOT SUPPORT FLUSH.");
	}

	protected void destroyDataSource() throws Exception {
		if (null != this.druidDataSource) {
			logger.warn("[DataSource Stop] Start!");
			this.druidDataSource.close();
			if (null != this.dbConfManager) {
				this.dbConfManager.stopDbConfManager();
			}
			if (null != this.dbPasswdManager) {
				this.dbPasswdManager.stopDbPasswdManager();
			}
			logger.warn("[DataSource Stop] End!");
		}

	}

	void setSingleInGroup(boolean isSingleInGroup) {
		this.runTimeConf.setSingleInGroup(isSingleInGroup);
	}

	public void setAppName(String appName) throws DruidAlreadyInitException {
		if (initFalg) {
			throw new DruidAlreadyInitException(
					"[AlreadyInit] couldn't Reset appName !");
		}
		this.appName = appName;
	}

	public void setDbKey(String dbKey) throws DruidAlreadyInitException {
		if (initFalg) {
			throw new DruidAlreadyInitException(
					"[AlreadyInit] couldn't Reset dbKey !");
		}
		this.dbKey = dbKey;
	}

	public void setLocalPasswd(String passwd) throws DruidAlreadyInitException {
		if (initFalg) {
			throw new DruidAlreadyInitException(
					"[AlreadyInit] couldn't Reset passwd !");
		}
		this.localConf.setPasswd(passwd);
	}

	public void setLocalConnectionProperties(Map<String, String> map)
			throws DruidAlreadyInitException {
		if (initFalg) {
			throw new DruidAlreadyInitException(
					"[AlreadyInit] couldn't Reset connectionProperties !");
		}
		this.localConf.setConnectionProperties(map);
	}

	public void setLocalDriverClass(String driverClass)
			throws DruidAlreadyInitException {
		if (initFalg) {
			throw new DruidAlreadyInitException(
					"[AlreadyInit] couldn't Reset driverClass !");
		}
		this.localConf.setDriverClass(driverClass);
	}

	public void setLocalSorterClass(String sorterClass)
			throws DruidAlreadyInitException {
		if (initFalg) {
			throw new DruidAlreadyInitException(
					"[AlreadyInit] couldn't Reset sorterClass !");
		}
		this.localConf.setSorterClass(sorterClass);
	}

	public String getAppName() {
		return appName;
	}

	public String getDbKey() {
		return dbKey;
	}

	public DruidDbStatusEnum getStatus() {
		return this.runTimeConf.getDbStautsEnum();
	}

	public DruidDbTypeEnum getDbType() {
		return this.runTimeConf.getDbTypeEnum();
	}

	public void setDbStatusListeners(
			List<DruidDbStatusListener> dbStatusListeners) {
		this.dbStatusListeners = dbStatusListeners;
	}

	private void processDbStatusListener(DruidDbStatusEnum oldStatus,
			DruidDbStatusEnum newStatus) {
		if (null != oldStatus && oldStatus != newStatus) {
			if (null != dbStatusListeners) {
				for (DruidDbStatusListener statusListener : dbStatusListeners) {
					try {
						statusListener.handleData(oldStatus, newStatus);
					} catch (Exception e) {
						logger.error("[call StatusListenner Error] !", e);
						continue;
					}
				}
			}
		}
	}
}