package com.taobao.tddl.client.jdbc.replication;

import static com.taobao.tddl.common.Monitor.KEY3_WRITE_LOG_EXCEPTION;
import static com.taobao.tddl.common.Monitor.KEY3_WRITE_LOG_SUCCESS;
import static com.taobao.tddl.common.Monitor.add;
import static com.taobao.tddl.common.Monitor.buildReplicationSqlKey2;
import static com.taobao.tddl.common.Monitor.buildTableKey1;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.taobao.tddl.client.jdbc.SqlExecuteEvent;
import com.taobao.tddl.client.jdbc.SqlExecuteListener;
import com.taobao.tddl.client.jdbc.TDataSource;
import com.taobao.tddl.client.jdbc.listener.Context;
import com.taobao.tddl.client.jdbc.replication.ReplicationSwitcher.Level;
import com.taobao.tddl.client.util.ExceptionUtils;
import com.taobao.tddl.client.util.LogUtils;
import com.taobao.tddl.client.util.UniqId;
import com.taobao.tddl.common.sync.BizTDDLContext;
import com.taobao.tddl.common.sync.RowBasedReplicationContext;
import com.taobao.tddl.common.sync.RowBasedReplicationExecutor;
import com.taobao.tddl.common.sync.SyncUtils;
import com.taobao.tddl.interact.rule.bean.SqlType;
import com.taobao.tddl.jdbc.group.DataSourceWrapper;
import com.taobao.tddl.jdbc.group.TGroupDataSource;

/*TODO:ListenerӦ����Ҫ�ܹ���������������һЩѡ������Ƿ������update sql ������version�ֶΡ�
 * �Լ��Ƿ���ҪΪrowbasedListener�ṩ�и��������һЩ���Ե���Ϣ��
 * ͬʱӦ��ע����������⡣
 */
public abstract class RowBasedReplicationListener implements SqlExecuteListener{
	private static final Log log = LogFactory.getLog(RowBasedReplicationListener.class);
	private static final Log localSyncLog = LogFactory.getLog(LogUtils.TDDL_LOCAL_SYNC_LOG);
	private static final String syncLogSep = "\t";

	/*
	 * modified by huali����10���޸�Ϊ5���ӣ����ֵ���̫С�ˣ���ô����������û�н�����ʱ�򣬸�������ͱ�syncserver������
	 * ��ô���ʱ���������޸Ļ�û���ύ������ָ��Ƶ����⣬Ȼ�����ֵ���ñȽϴ��һ��Ӱ���ǣ����Ӧ�û����ĸ�������û����ɣ�
	 * ��ôͨ��sync-serverȥ�ָ���ʱ�򣬻�Ҫ����Գ���ʱ�䡣
	 */
	private static final long DEFAULT_MAX_TX_TIME = 1000 * 60 * 5;

	private UniqId uniqId = UniqId.getInstance();
	private long maxTxTime = DEFAULT_MAX_TX_TIME;
	protected ReplicationSwitcher replicationSwitcher;
	private ReplicationCallbackHandler replicationCallbackHandler = new SpecialExceptionPolicy();
	private ReplicationConfig replicationConfig;
	private DataSource syncLogDataSource; //��TGroupDataSource���ԭ�ȵĶ�̬�����Թ���
	private boolean syncLogIsGroupDataSource = true;
	public void setSyncLogDataSource(DataSource syncLogDataSource) {
		this.syncLogDataSource = syncLogDataSource;
		syncLogIsGroupDataSource = syncLogDataSource instanceof TGroupDataSource;
		if(syncLogIsGroupDataSource){
			((TGroupDataSource) syncLogDataSource).setTracerWriteTarget(true);
		}
	}

	//private EquityDbManager syncLogDb;
	//private String syncLogDataSourceConfigFile;
	//private String synclogDatabaseId;
	protected String appName;

	/*
	protected void initSyncLogDb(boolean isUseLocalConfig){
		syncLogDb = new EquityDbManager(synclogDatabaseId);
		if (isUseLocalConfig) {
			if (syncLogDataSourceConfigFile == null) {
				throw new IllegalArgumentException("û������syncLogDataSourceConfigFile");
			}
			syncLogDb.setDataSourceConfigFile(syncLogDataSourceConfigFile);
		} else {
			if (synclogDatabaseId == null) {
				throw new IllegalArgumentException("û������syncLogDataSourceConfigDataId");
			}
			String dbConfigDataId = new MessageFormat(ConfigServerHelper.DATA_ID_SYNCLOG_DBSET).format(new Object[]{synclogDatabaseId});
			String dbWeightDataId = new MessageFormat(ConfigServerHelper.DATA_ID_SYNCLOG_DBWEIGHT).format(new Object[]{synclogDatabaseId});
			syncLogDb.setDbConfigDataId(dbConfigDataId);
			syncLogDb.setDbWeightDataId(dbWeightDataId);
		}
		try {
			syncLogDb.init();
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}
	*/

	public void init(TDataSource tds) {
		if (tds.getReplicationConfig() == null) {
			throw new IllegalArgumentException("���µ�" + getClass().getName() + ",TDataSource��������replicationConfig");
		}
		this.appName = tds.getAppName();
		this.replicationConfig = tds.getReplicationConfig();
		
		//initSyncLogDb(tds.isUseLocalConfig());
		
		if (replicationSwitcher == null) {
			//���û����ʽָ��replicationSwitcher���򴴽�Ĭ�ϵ�
			ConfigServerReplicationSwitcher csrs = new ConfigServerReplicationSwitcher();
			csrs.setAppName(this.appName);
			csrs.init();
			replicationSwitcher = csrs;
		} else if (replicationSwitcher instanceof ConfigServerReplicationSwitcher) {
			ConfigServerReplicationSwitcher csrs = (ConfigServerReplicationSwitcher) replicationSwitcher;
			if (csrs.getAppName() == null) {
				//���û����ʽָ��appName��������ΪTds��appName
				csrs.setAppName(this.appName);
			}
			csrs.init();
		}
	}

	public void init() {
	}

	public void destroy() {
	}

	public void beforeSqlExecute(Context context) throws SQLException {
		if (replicationSwitcher != null && replicationSwitcher.level() == Level.ALL_OFF) {
			return;
		}
		for (SqlExecuteEvent event : context.getEvents()) {
			beforeSqlExecute(event);
		}
	}

	private void beforeSqlExecute(SqlExecuteEvent event) throws SQLException {
		if (!event.isReplicated()) {
			return;
		}
		if (event.getSqlType() == SqlType.DELETE) {
			throw new IllegalArgumentException("���и���ģʽ�в�֧��ʹ��delete:" + event.getSql());
		}
		if (event.getSqlType() == SqlType.INSERT || event.getSqlType() == SqlType.UPDATE) {
			doBeforeSqlExecute(event);
		}
	}
	
	public void afterSqlExecute(Context context) throws SQLException {
		if(context.getAffectedRows() == 0) {
			for(SqlExecuteEvent event : context.getEvents()) {
				RowBasedReplicationExecutor.deleteSyncLog(buildRowBasedReplicationContext(event));
			}
			return;
		}
		if (replicationSwitcher != null
				&& (replicationSwitcher.level() == Level.ALL_OFF || replicationSwitcher.level() == Level.INSERT_LOG)) {
			if(log.isDebugEnabled()) {
				StringBuilder sb =new StringBuilder("current level :")
					.append(replicationSwitcher.level()).append(", return from after sql");
				log.debug(sb.toString());
			}
			return;
		}
		for (SqlExecuteEvent event : context.getEvents()) {
			afterSqlExecute(event);
		}
	}

	private void afterSqlExecute(SqlExecuteEvent event) throws SQLException {
		if (!event.isReplicated()) {
			return;
		}
		if (event.getSqlType() == SqlType.INSERT || event.getSqlType() == SqlType.UPDATE) {
			doAfterSqlExecute(buildRowBasedReplicationContext(event));
		}
	}

	public RowBasedReplicationContext buildRowBasedReplicationContext(SqlExecuteEvent event) {
		RowBasedReplicationContext context = new RowBasedReplicationContext();
		context.setSqlType(event.getSqlType());
		context.setPrimaryKeyColumn(event.getPrimaryKeyColumn());
		context.setPrimaryKeyValue(event.getPrimaryKeyValue());
		context.setMasterLogicTableName(event.getLogicTableName());
		context.setMasterDatabaseShardColumn(event.getDatabaseShardColumn());
		context.setMasterDatabaseShardValue(event.getDatabaseShardValue());
		context.setMasterTableShardColumn(event.getTableShardColumn());
		context.setMasterTableShardValue(event.getTableShardValue());
		context.setSyncLogJdbcTemplate(event.getSyncLogJdbcTemplate());
		context.setSyncLogId(event.getSyncLogId());

		BizTDDLContext xmlElement = this.replicationConfig.getLogicTableName2TDDLContext().get(
				context.getMasterLogicTableName());

		context.setSlaveInfos(xmlElement.getSlaveInfos());
		context.setMasterColumns(xmlElement.getMasterColumns());
		context.setMasterJdbcTemplate(xmlElement.getMasterJdbcTemplate());
		context.setCreateTime(new Timestamp(System.currentTimeMillis())); //fixed by guangxia

		context.setAfterMainDBSqlExecuteTime(event.getAfterMainDBSqlExecuteTime());
		context.setSql(event.getSql());
		return context;
	}

	protected abstract void doAfterSqlExecute(RowBasedReplicationContext context);
	protected abstract void asyncInsertSyncLog2Db(SqlExecuteEvent event);
	private void doBeforeSqlExecute(SqlExecuteEvent event) throws SQLException{
		if (replicationSwitcher == null || replicationSwitcher.insertSyncLogMode() == null) {
			insertSyncLog2Db(event);
			return;
		}
		switch (replicationSwitcher.insertSyncLogMode()) {
		case normal:
			insertSyncLog2Db(event);
			break;
		case logfileonly:
			insertSyncLog2LocalFile(event);
			break;
		case streaking: //ʲô������			
			break;
		case asynchronous:
			asyncInsertSyncLog2Db(event);
			break;
		default:
			throw new IllegalStateException("InsertSyncLogMode������ѡ��");
		}
	}
	
	/*
	protected void insertSyncLog2Db(SqlExecuteEvent event) throws SQLException{
		long time = System.currentTimeMillis();
		//insertSyncLog(event, new ArrayList<SQLException>(syncLogDataSources.size()));
		try {
			this.syncLogDb.tryExecute(insertSyncLogTryer, 3, event);
		} catch (SQLException e) {
			//��¼log�쳣��ʱ��ҲӦ���и�ͳ��
			add(buildTableKey1(event.getLogicTableName()), buildReplicationSqlKey2(event.getSql()),
					KEY3_WRITE_LOG_EXCEPTION, System.currentTimeMillis() - time, 1);
			throw e;
		}			
		add(buildTableKey1(event.getLogicTableName()), buildReplicationSqlKey2(event.getSql()),
				KEY3_WRITE_LOG_SUCCESS, System.currentTimeMillis() - time, 1);
	}
    */
	protected void insertSyncLog2Db(SqlExecuteEvent event) throws SQLException{
		long time = System.currentTimeMillis();
		//insertSyncLog(event, new ArrayList<SQLException>(syncLogDataSources.size()));
		try {
			//this.syncLogDb.tryExecute(insertSyncLogTryer, 3, event);
			event.setSyncLogJdbcTemplate(new JdbcTemplate(syncLogDataSource)); //������Բ���
			
			insertSyncLog(event);
			
			if (syncLogIsGroupDataSource) {
				DataSourceWrapper dsw = ((TGroupDataSource) syncLogDataSource).getCurrentTarget();
				event.setSyncLogDsKey(dsw.getDataSourceKey());
				event.setSyncLogJdbcTemplate(new JdbcTemplate(dsw.getWrappedDataSource()));
			}
		} catch (SQLException e) {
			//��¼log�쳣��ʱ��ҲӦ���и�ͳ��
			add(buildTableKey1(event.getLogicTableName()), buildReplicationSqlKey2(event.getSql()),
					KEY3_WRITE_LOG_EXCEPTION, System.currentTimeMillis() - time, 1);
			
			List<SQLException> exceptions = new LinkedList<SQLException>();
			exceptions.add(e);
			replicationCallbackHandler.insertSyncLogFailed(event, exceptions); 
		}			
		add(buildTableKey1(event.getLogicTableName()), buildReplicationSqlKey2(event.getSql()),
				KEY3_WRITE_LOG_SUCCESS, System.currentTimeMillis() - time, 1);
	}

	/*
	private DataSourceTryer<Object> insertSyncLogTryer = new DataSourceTryer<Object>() {
		public Object tryOnDataSource(String dsKey, DataSource ds, Object... args) throws SQLException {
			SqlExecuteEvent event = (SqlExecuteEvent)args[0];
			event.setSyncLogJdbcTemplate(new JdbcTemplate(ds));
			event.setSyncLogDsKey(dsKey);
			insertSyncLog(event);
			return null;
		}

		public Object onSQLException(List<SQLException> exceptions,ExceptionSorter exceptionSorter, Object... args) throws SQLException {
			replicationCallbackHandler.insertSyncLogFailed((SqlExecuteEvent)args[0], exceptions);
			return null;
		}
	};
	*/
	
	/**
	 * ������־ʧ�ܣ��׳�������쳣
	 */
	public static class SpecialExceptionPolicy implements ReplicationCallbackHandler {
		public void insertSyncLogFailed(SqlExecuteEvent event, List<SQLException> exceptions) throws SQLException {

			if(exceptions == null){
				exceptions = new  LinkedList<SQLException>();
			}
			exceptions.add(0, new SaveSyncLogFailedException("insert log exception,first exception is ",exceptions.size() >0?exceptions.get(0):null));
			ExceptionUtils.throwSQLException(exceptions, "insert sync_log sql", Collections.emptyList()); //TODO null����event����Ϣ
		}
	}
	/**
	 * ֱ��ͨ��log4jд����log�ļ���Ӧ����Ҫ�������ܺ����� 
	 */
	public static class LocalLogPolicy implements ReplicationCallbackHandler {
		public void insertSyncLogFailed(SqlExecuteEvent event, List<SQLException> exceptions) throws SQLException {
			log.warn("insert sync_log sql failed.",ExceptionUtils.mergeException(exceptions));
			insertSyncLog2LocalFile(event);
		}
	}
	
	//д����Log�ļ�
	protected static void insertSyncLog2LocalFile(SqlExecuteEvent event){
		insertSyncLog2LocalFile(localSyncLog, event);
	}
	protected static void insertSyncLog2LocalFile(Log logger, SqlExecuteEvent event){
		logger.fatal(new StringBuilder()
		        .append(event.getSqlType().value()).append(syncLogSep)
		        .append(event.getLogicTableName()).append(syncLogSep)
				.append(event.getPrimaryKeyColumn()).append(syncLogSep)
				.append(event.getPrimaryKeyValue()).append(syncLogSep)
				.append(event.getDatabaseShardColumn()).append(syncLogSep)
				.append(event.getDatabaseShardValue()).append(syncLogSep)
				.append(event.getTableShardColumn()).append(syncLogSep)
				.append(event.getTableShardValue()).toString());
	}

	private void insertSyncLog(SqlExecuteEvent event) throws SQLException {
		String syncLogId = uniqId.getUniqIDHashString();

		StringBuilder sql = new StringBuilder();
		sql.append("insert into sync_log_").append(SyncUtils.getSyncLogTableSuffix(syncLogId));
		sql.append(" (id, sql_type, logic_table_name, primary_key_column, primary_key_value,");
		sql.append(" database_shard_column, database_shard_value, table_shard_column, table_shard_value,");
		sql.append(" hash_code, gmt_create, next_sync_time) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

		Object[] args = new Object[] {
				syncLogId,
				//event.getSqlType() == SqlType.INSERT ? SyncConstants.SQL_TYPE_INSERT : SyncConstants.SQL_TYPE_UPDATE,
				event.getSqlType().value(), event.getLogicTableName(), event.getPrimaryKeyColumn(),
				event.getPrimaryKeyValue(), event.getDatabaseShardColumn(), event.getDatabaseShardValue(),
				event.getTableShardColumn(), event.getTableShardValue(), getHashcode(event.getPrimaryKeyValue()),
				new Date(), new Date(System.currentTimeMillis() + maxTxTime) };

		if (log.isDebugEnabled()) {
			log.debug("insertSyncLog, sql = [" + sql.toString() + "], args = " + Arrays.asList(args));
		}

		try {
			event.getSyncLogJdbcTemplate().update(sql.toString(), args);

			event.setSyncLogId(syncLogId);
		} catch (DataAccessException e) {
			if (e.getCause() instanceof SQLException) {
				throw (SQLException) e.getCause();
			} else {
				throw e;
			}
		}
	}

	/**
	 * @return toString��ϵͳĬ�ϱ���ȡMD5ɢ�е�ǰ�����ֽڣ��������ȷֲ���[0-65535]��Χ��
	 */
	private static int getHashcode(Object pkValue) {
		if (pkValue == null) {
			log.warn("PrimaryKeyValue is null");
			return 0;
		}
		try {
			byte[] bytes = getMd5(pkValue.toString().getBytes());
			if (bytes.length < 2) {
				log.error("Invalid MD5 digest!");
				return 0;
			}
			return ((0xff & bytes[0]) << 8) + (0xff & bytes[1]);
		} catch (NoSuchAlgorithmException e) {
			log.error("HashCode failed!", e);
			return 0;
		}
	}

	private static byte[] getMd5(byte[] src) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		return md.digest(src);
	}

	/**
	 * ���߼���getter/setter
	 */

	public void setMaxTxTime(long maxTxTime) {
		this.maxTxTime = maxTxTime;
	}

	public void setReplicationSwitcher(ReplicationSwitcher replicationSwitcher) {
		this.replicationSwitcher = replicationSwitcher;
	}

	public void setReplicationConfig(ReplicationConfig replicationConfig) {
		this.replicationConfig = replicationConfig;
	}

	public void setReplicationCallbackHandler(ReplicationCallbackHandler replicationCallbackHandler) {
		this.replicationCallbackHandler = replicationCallbackHandler;
	}

	/*public void setSyncLogDataSourceConfigFile(String syncLogDataSourceConfigFile) {
		this.syncLogDataSourceConfigFile = syncLogDataSourceConfigFile;
	}

	public void setSynclogDatabaseId(String synclogDatabaseId) {
		this.synclogDatabaseId = synclogDatabaseId;
	}*/

}
