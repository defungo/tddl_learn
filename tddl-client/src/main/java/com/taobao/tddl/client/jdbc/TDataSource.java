package com.taobao.tddl.client.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.taobao.tddl.client.controller.SpringBasedDispatcherImpl;
import com.taobao.tddl.client.jdbc.listener.Adapter;
import com.taobao.tddl.client.jdbc.listener.HookPoints;
import com.taobao.tddl.client.jdbc.replication.ReplicationAdapter;
import com.taobao.tddl.client.jdbc.replication.ReplicationConfig;
import com.taobao.tddl.common.sync.BizTDDLContext;
import com.taobao.tddl.rule.bean.LogicTable;

public class TDataSource extends TDataSourceConfig implements DataSource, Cloneable {
	private final TDSProperties properties = new TDSProperties();
	private static final long DEFAULT_TIMEOUT_THRESHOLD = 100;
	private static DataSource indexMappingCacheDatasource = null;

	private ReplicationConfig replicationConfig;
	private String replicationConfigFile; // �±����ļ�����
	@SuppressWarnings("unchecked")
	private Map<String, DataSource> replicationTargetDataSources = Collections.EMPTY_MAP;
	
	public TDataSourceState state = new TDataSourceState(""); //��ȫ����

	public void init() {
		state = new TDataSourceState(getAppName());
		super.init();
		initReplication();
	}

	/**
	 * ��ʼ�����ݸ�������
	 * @author junyu
	 */
	private void initReplication() {
		if (this.isHandleReplication) {
			this.replicationConfig = new ReplicationConfig();
			this.replicationConfig.setAppName(this.getAppName());
			this.replicationConfig.setUseLocalConfig(this.isUseLocalConfig());
			this.replicationConfig.setReplicationConfigFile(
			        doExternalResolve(this.replicationConfigFile)[0]); /* ʹ���ⲿ  springContext ���������ļ�·�� */
			this.replicationConfig.init(this);

			if (this.defaultDispatcher instanceof SpringBasedDispatcherImpl) {
				SpringBasedDispatcherImpl dispatcher = (SpringBasedDispatcherImpl) this.defaultDispatcher;
				for (Map.Entry<String, BizTDDLContext> e : this.replicationConfig.getLogicTableName2TDDLContext()
						.entrySet()) {
					LogicTable logicTable = dispatcher.getRoot().getLogicTable(e.getKey());
					if (logicTable != null) {
						logicTable.setNeedRowCopy(true);
						logicTable.setUniqueKeys(e.getValue().getUniqueKey());
					}
				}
			}
		}

		try {
			this.hookPoints = new HookPoints();
			for (Adapter adapter : adapters) {
				adapter.init(this, hookPoints);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Connection getConnection() throws SQLException {
		TConnectionImp returnConnection = null;
		if (TransactionCrossDb.NOT_ALLOW_READ == restrictionLEVEL) {
			returnConnection = new TConnectionImp(enableProfileRealDBAndTables, pipelineFactory);
		} else if (TransactionCrossDb.ALLOW_READ == restrictionLEVEL) {
			returnConnection = new AllowReadLevelTConnection(enableProfileRealDBAndTables, pipelineFactory);
		} else if (TransactionCrossDb.ALLOW_ALL == restrictionLEVEL) {
			returnConnection = new AllowAllLevelTconnection(enableProfileRealDBAndTables, pipelineFactory);
		}
		buildConnection1(returnConnection);
		return returnConnection;
	}

	private void buildConnection1(TConnectionImp connectionImp) {
		TddlRuntime rt = super.runtimeConfigHolder.get();
		connectionImp.setHookPoints(hookPoints);
		if (rt == null) {
			throw new IllegalArgumentException("rt is null");
		}
		connectionImp.setProperties(properties);
		connectionImp.setDsMap(rt.dsMap);
	}

	public Connection getConnection(String username, String password) throws SQLException {
		return getConnection();
	}

	public int getLoginTimeout() throws SQLException {
		throw new UnsupportedOperationException("getLoginTimeout");
	}

	public void setLoginTimeout(int seconds) throws SQLException {
		throw new UnsupportedOperationException("setLoginTimeout");
	}

	public PrintWriter getLogWriter() throws SQLException {
		throw new UnsupportedOperationException("getLogWriter");
	}

	public void setLogWriter(PrintWriter out) throws SQLException {
		throw new UnsupportedOperationException("setLogWriter");
	}

	@Override
	protected TDataSource clone() throws CloneNotSupportedException {
		return (TDataSource) super.clone();
	}

	@Override
	public String toString() {
		return new StringBuilder("{MasterOnly:").append(isMasterOnly).append(",SlaveOnly:").append(this.isSlaveOnly)
				.append(",ReadOnly:").append(this.isReadOnly).append("}").toString();
	}

	public void setTimeoutThreshold(long timeoutThreshold) {
		this.properties.timeoutThreshold = timeoutThreshold;
	}

	public static DataSource getIndexMappingCacheDatasource() {
		return indexMappingCacheDatasource;
	}

	public static void setIndexMappingCacheDatasource(DataSource indexMappingCacheDatasource) {
		TDataSource.indexMappingCacheDatasource = indexMappingCacheDatasource;
	}

	@SuppressWarnings("unchecked")
	private List<Adapter> adapters = Collections.EMPTY_LIST;

	public void setListenerAdapter(Adapter adapter) throws SQLException {
		LinkedList<Adapter> adapters = new LinkedList<Adapter>();
		adapters.add(adapter);
		setListenerAdapters(adapters);
	}

	public void setListenerAdapters(List<Adapter> adapters) throws SQLException {
		this.adapters = adapters;
		for (Adapter adapter : adapters) {
			if (adapter instanceof ReplicationAdapter) {
				this.isHandleReplication = true;
				break;
			}
		}
	}

	public ReplicationConfig getReplicationConfig() {
		return replicationConfig;
	}

	public void setReplicationConfigFile(String replicationConfigFile) {
		this.replicationConfigFile = replicationConfigFile;
	}

	public String getReplicationConfigFile() {
		return replicationConfigFile;
	}

	public boolean isEnableReplaceTableSuffixByIdentifier() {
		return properties.enableReplaceTableSuffixByIdentifier;
	}

	public void setEnableReplaceTableSuffixByIdentifier(boolean enableReplaceTableSuffixByIdentifier) {
		this.properties.enableReplaceTableSuffixByIdentifier = enableReplaceTableSuffixByIdentifier;
	}

	public void setUsePlaceHolder(boolean usePlaceHolder) {
		this.properties.usePlaceHolder = usePlaceHolder;
	}

	public boolean getUsePlaceHolder() {
		return this.properties.usePlaceHolder;
	}

	public void setRemoveThreadLocalOnCloseConn(boolean re) {
		this.properties.removeThreadLocalOnCloseConn = re;
	}

	public boolean getRemoveThreadLocalOnCloseConn() {
		return properties.removeThreadLocalOnCloseConn;
	}

	public DataSource getDataSource(String dbIndex) {
		return this.runtimeConfigHolder.get().dsMap.get(dbIndex);
	}

	public static class TDSProperties {
		/**
		 * ��ʱʱ��
		 */
		public long timeoutThreshold = DEFAULT_TIMEOUT_THRESHOLD;
		/**
		 * �Ƿ�����replace��׺������ģʽ
		 */
		public boolean enableReplaceTableSuffixByIdentifier = false;
		/**
		 * �Ƿ�ʹ��@table@��ģʽ�����б���ָ��
		 */
		public boolean usePlaceHolder = false;
		/**
		 * �Ƿ�Ӧ���ڹر����ӵ�ʱ���Ƴ�ThreadLocal
		 */
		public boolean removeThreadLocalOnCloseConn = false;
	}

	/**
	 * ������������Ĭ��Ϊ���ж��������
	 */
	private TransactionCrossDb restrictionLEVEL = TransactionCrossDb.NOT_ALLOW_READ;

	public enum TransactionCrossDb {
		/**
		 * ������״̬�£��������ݿ�����Ӳ������
		 */
		NOT_ALLOW_READ,
		/**
		 * ������״̬�£��������ݿ������ֻ�������������д�룬д����״���
		 */
		ALLOW_READ,
		/**
		 * ������״̬�£��������ݿ���������дҲ�������
		 */
		ALLOW_ALL;
	}

	public TransactionCrossDb getTransactionCrossDb() {
		return restrictionLEVEL;
	}

	public void setTransactionCrossDb(TransactionCrossDb restrictionLEVEL) {
		this.restrictionLEVEL = restrictionLEVEL;
	}

	public Map<String, DataSource> getReplicationTargetDataSources() {
		return replicationTargetDataSources;
	}

	public void setReplicationTargetDataSources(Map<String, DataSource> replicationTargetDataSources) {
		this.replicationTargetDataSources = replicationTargetDataSources;
	}


	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return this.getClass().isAssignableFrom(iface);
	}

	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
		try {
			return (T) this;
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}
}
