package com.taobao.tddl.client.jdbc;

import static com.taobao.tddl.client.util.ExceptionUtils.appendToExceptionList;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.client.RouteCondition;
import com.taobao.tddl.client.RouteCondition.ROUTE_TYPE;
import com.taobao.tddl.client.ThreadLocalString;
import com.taobao.tddl.client.jdbc.TDataSource.TDSProperties;
import com.taobao.tddl.client.jdbc.listener.Context;
import com.taobao.tddl.client.jdbc.listener.HookPoints;
import com.taobao.tddl.client.jdbc.sqlexecutor.parallel.ParallelRealSqlExecutor;
import com.taobao.tddl.client.jdbc.sqlexecutor.serial.SerialRealSqlExecutor;
import com.taobao.tddl.client.jdbc.sqlexecutor.serial.SimpleSerialRealSqlExecutor;
import com.taobao.tddl.client.pipeline.PipelineFactory;
import com.taobao.tddl.client.pipeline.bootstrap.Bootstrap;
import com.taobao.tddl.client.pipeline.bootstrap.PipelineBootstrap;
import com.taobao.tddl.client.util.ExceptionUtils;
import com.taobao.tddl.client.util.ThreadLocalMap;

public class TConnectionImp implements ConnectionManager, Connection {
	private TDSProperties properties;
	protected static final Log log = LogFactory.getLog(TConnectionImp.class);

	// TODO: �Ժ������ֵ������������
	private int transactionIsolation = -1;

	private boolean closed;

	private final boolean enableProfileRealDBAndTables;

	private final PipelineFactory pipelineFactory;

	protected boolean isAutoCommit = true;

	protected Set<TStatementImp> openedStatements = new HashSet<TStatementImp>(
			2);

	private HookPoints hookPoints;

	private final Context context = new Context();

	private final static boolean closeInvokedByTStatement = false;

	public TConnectionImp(boolean enableProfileRealDBAndTables,
			PipelineFactory pipelineFactory) {
		this.enableProfileRealDBAndTables = enableProfileRealDBAndTables;
		this.pipelineFactory = pipelineFactory;
	}

	public TConnectionImp(String username, String password,
			boolean enableProfileRealDBAndTables,
			PipelineFactory pipelineFactory) {
		this.enableProfileRealDBAndTables = enableProfileRealDBAndTables;
		this.pipelineFactory = pipelineFactory;
	}

	protected void checkClosed() throws SQLException {
		if (closed) {
			throw new SQLException(
					"No operations allowed after connection closed.");
		}
	}

	public boolean isClosed() throws SQLException {
		return closed;
	}

	/**
	 * ������������
	 */
	private final static int maxTransactionDSCount = 1;

	protected Map<String, DataSource> dsMap = Collections.emptyMap();

	Map<String, Connection> connectionMap = new HashMap<String, Connection>(2);

	/**
	 * ���Ի�ȡһ������ :
	 * 
	 * 
	 * ������:
	 * 
	 * ���goMaster == true ������Դ����һ���� ���״���
	 * 
	 * ����ڷ����� �����״���
	 * 
	 * ��ÿ�γ��Դ��»�ȡ���ӵ�ʱ�򣬶�������ʾ�Ľ���������RetryableDatasourceGrooup.autocommit() Ϊָ����ֵ��
	 * 
	 * ���ɵĵط��ǣ�����������У���ѯ�����⣬�Ƿ�Ӧ����������ѯ�أ�
	 * 
	 * @param dbIndex
	 * @param goSlave
	 *            TODO: ����֧������ʵ�֡�����Ҫ���������У�����ͨ��select��ѯ��������Դ��
	 * @return
	 */
	public Connection getConnection(String dbIndex, boolean goSlave)
			throws SQLException {

		Connection conn = connectionMap.get(dbIndex);
		if (conn == null) {
			DataSource datasource = dsMap.get(dbIndex);
			if (datasource == null) {
				throw new SQLException(
						"can't find datasource by your dbIndex :" + dbIndex);
			}
			// ��ǰdbIndexû�б���������ʹ�ã���ʼ��dsGroupImp,
			if (isAutoCommit) {
				conn = datasource.getConnection();
				conn.setAutoCommit(isAutoCommit);
				connectionMap.put(dbIndex, conn);
			} else {
				// ����״̬��
				validTransactionCondition(true);
				conn = datasource.getConnection();
				conn.setAutoCommit(isAutoCommit);
				connectionMap.put(dbIndex, conn);
			}
		} else {
			// ��ʾ��ǰdbIndex�Ѿ�����������ʹ�á���ô����ǰ������ӵ����������С�
			// ����û����ʾ������autoCommit״̬��ԭ�����ܹ��޸�autoCommit״̬�ĵط�ֻ������
			// ��һ�����½�����һ�£�����һ������setAutoCommit����һ�¡�����ֻ��Ҫ����״̬��
			return conn;
		}

		return conn;
	}

	/**
	 * ��֤������״̬�У��Ƿ���Դ�������Դ
	 * 
	 * @param requiredObject
	 * @param goSlave
	 * @param dbSelector
	 * @throws SQLException
	 */
	protected void validTransactionCondition(boolean createNew)
			throws SQLException {
		// �����������Դ��������,���½����ӵ�ʱ��Ҫ�׳�Ҫ�½����Ǹ����ӣ�����setAutoCommit��ʱ�򣬲���ȥ��
		if (connectionMap.size() > (maxTransactionDSCount - (createNew ? 1 : 0)/*
																				 * �����������datasource��
																				 * -
																				 * ��ǰҪ�����ds��
																				 * ��
																				 */)) {
			//have a nice log
			StringBuilder sb=new StringBuilder("�����п���������Ԥ�ڣ�Ԥ��ֵ: ");
			sb.append(maxTransactionDSCount);
			sb.append(",current dbIndexes in connectionMap is:");
			for(Map.Entry<String, Connection> entry:connectionMap.entrySet()){
				sb.append(entry.getKey());
				sb.append(";");
			}

			throw new SQLException(sb.toString());
			// TODO: ������ʱ�������������У�ʹ�÷���������Դ���в�ѯ������̫�����ˡ�
		}
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency)
			throws SQLException {
		TStatementImp stmt = (TStatementImp) createStatement();
		stmt.setResultSetType(resultSetType);
		stmt.setResultSetConcurrency(resultSetConcurrency);
		return stmt;
	}

	public Statement createStatement() throws SQLException {
		checkClosed();
		Bootstrap bootstrap = new PipelineBootstrap(this, pipelineFactory);
		TStatementImp stmt = new TStatementImp(this, bootstrap);

		SerialRealSqlExecutor serialRealSqlExecutor = new SerialRealSqlExecutor(
				this);
		ParallelRealSqlExecutor parallelRealSqlExecutor = new ParallelRealSqlExecutor(
				this);
		SerialRealSqlExecutor simpleSerialRealSqlExecutor = new SimpleSerialRealSqlExecutor(
				this);

		stmt.setTimeoutThreshold(properties.timeoutThreshold);
		stmt.setHookPoints(hookPoints);
		stmt.setContext(context);
		stmt.setEnableProfileRealDBAndTables(enableProfileRealDBAndTables);
		stmt.setProperties(properties);
		stmt.setSerialRealSqlExecutor(serialRealSqlExecutor);
		stmt.setParallelRealSqlExecutor(parallelRealSqlExecutor);
		stmt.setSimpleSerialRealSqlExecutor(simpleSerialRealSqlExecutor);

		openedStatements.add(stmt);
		return stmt;
	}

	public Statement createStatement(int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		TStatementImp stmt = (TStatementImp) createStatement(resultSetType,
				resultSetConcurrency);

		stmt.setResultSetHoldability(resultSetHoldability);
		return stmt;
	}

	public PreparedStatement prepareStatement(String sql) throws SQLException {
		checkClosed();
		Bootstrap bootstrap = new PipelineBootstrap(this, pipelineFactory);
		TPreparedStatementImp stmt = new TPreparedStatementImp(this, sql,
				bootstrap);

		SerialRealSqlExecutor serialRealSqlExecutor = new SerialRealSqlExecutor(
				this);
		ParallelRealSqlExecutor parallelRealSqlExecutor = new ParallelRealSqlExecutor(
				this);
		SerialRealSqlExecutor simpleSerialRealSqlExecutor = new SimpleSerialRealSqlExecutor(
				this);

		stmt.setTimeoutThreshold(properties.timeoutThreshold);
		stmt.setHookPoints(hookPoints);
		stmt.setContext(context);
		stmt.setEnableProfileRealDBAndTables(enableProfileRealDBAndTables);
		stmt.setProperties(properties);
		stmt.setSerialRealSqlExecutor(serialRealSqlExecutor);
		stmt.setParallelRealSqlExecutor(parallelRealSqlExecutor);
		stmt.setSimpleSerialRealSqlExecutor(simpleSerialRealSqlExecutor);

		openedStatements.add(stmt);
		return stmt;
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		TPreparedStatementImp stmt = (TPreparedStatementImp) prepareStatement(sql);
		stmt.setResultSetType(resultSetType);
		stmt.setResultSetConcurrency(resultSetConcurrency);
		return stmt;
	}

	public boolean containDBIndex(String dbIndex) {
		return dsMap.containsKey(dbIndex);
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		TPreparedStatementImp stmt = (TPreparedStatementImp) prepareStatement(
				sql, resultSetType, resultSetConcurrency);
		stmt.setResultSetHoldability(resultSetHoldability);
		return stmt;
	}

	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
			throws SQLException {
		TPreparedStatementImp stmt = (TPreparedStatementImp) prepareStatement(sql);
		stmt.setAutoGeneratedKeys(autoGeneratedKeys);
		return stmt;
	}

	public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
			throws SQLException {
		TPreparedStatementImp stmt = (TPreparedStatementImp) prepareStatement(sql);
		stmt.setColumnIndexes(columnIndexes);
		return stmt;
	}

	public PreparedStatement prepareStatement(String sql, String[] columnNames)
			throws SQLException {
		TPreparedStatementImp stmt = (TPreparedStatementImp) prepareStatement(sql);
		stmt.setColumnNames(columnNames);
		return stmt;
	}

	public CallableStatement prepareCall(String sql) throws SQLException {
		throw new UnsupportedOperationException("prepareCall");
	}

	public CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		throw new UnsupportedOperationException("prepareCall");
	}

	public CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		throw new UnsupportedOperationException("prepareCall");
	}

	public void commit() throws SQLException {
		log.debug("invoke commit");

		checkClosed();

		if (isAutoCommit) {
			return;
		}

		hookPoints.getBeforeExecute().execute(context);

		// txStart = true;

		List<SQLException> exceptions = null;
		// ����������������
		for (Entry<String, Connection> conn : connectionMap.entrySet()) {
			try {
				// �����setAutoCommit�з������쳣������Ҫ�ؽ����ӣ�����Щ���ڵ������ڵ�connection
				// commit����
				if (isTransactionConnection(conn.getKey())) {
					conn.getValue().commit();
				}
			} catch (SQLException e) {
				if (exceptions == null) {
					exceptions = new LinkedList<SQLException>();
				}
				exceptions.add(e);
			}
		}

		ExceptionUtils.throwSQLException(exceptions, null, (List<Object>) null);

		hookPoints.getAfterExecute().execute(context);
		context.reset();
	}

	public void rollback() throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug("invoke rollback");
		}

		checkClosed();

		if (isAutoCommit) {
			return;
		}

		List<SQLException> exceptions = null;

		for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
			try {
				Connection conn = entry.getValue();
				if (isTransactionConnection(entry.getKey())) {
					conn.rollback();
				}
			} catch (SQLException e) {
				if (exceptions == null) {
					exceptions = new ArrayList<SQLException>();
				}
				exceptions.add(e);

				log.error(
						new StringBuilder("data source name: ").append(
								entry.getKey()).toString(), e);
			}
		}

		context.reset();

		ExceptionUtils.throwSQLException(exceptions, null, (List<Object>) null);
	}

	/**
	 * @param dbIndex
	 * @throws SQLException
	 */
	public void tryClose(String dbIndex) throws SQLException {
		Connection conn = connectionMap.get(dbIndex);
		if (conn == null) {
			// �����ǰdsGroupû����map�ڣ���ô�򵥵ķ���
			// ����һ�����͵ĳ�������setAutoCommit(false->true)�Ĺ����У�ҲҪ��ʾ�Ĺر�
			// ���쳣״̬��ҲҪ�رգ����Ի��Ǵ�log�رհɡ�
			// log.warn("should not be here ");
			return;
		}
		
		if (isAutoCommit && openedStatements.size() <= 1) {
			// ������״̬��,���Ҵ򿪵�statementֻ��һ����
			try {
				// ���е�ǰ���õ�ǰ���£���ʾ�ⲿ�Ѿ�û���ٳ��е�ǰ�����ˡ��ر����ӡ�
				conn.close();
			} finally {
				// �Ƴ���ǰ����Դ
				connectionMap.remove(dbIndex);
			}
			// todo:���ﻹ�и������Ż��ĵط��������openedStatements.size
			// >1��ʱ�򣬱�������statements���������statement.isResultSetClosed��Ϊtrue������Թر�����
		}
	}

	/**
	 * ���õ�ǰ����״̬��
	 * 
	 * ��������е�Manager(autoCommit=false) ,��������Connection.setAutoCommit(true) ��������ô
	 * ��ʱ��ֻ�п�����maxTransactionDSCount�� �����ǰ�����д򿪵�statement����δ�رյĽ���������������ӱ��뱣�ִ򿪡�
	 * ���û��statement,������statementû��resultSet,������resultSet��resultSet�رա������Թرյ�ǰ����
	 * 
	 * ����autoCommit=true ��ΪautoCommit = falseʱ�����ȼ�鵱ǰManager�г����˼���Connection.
	 * 
	 * Ŀǰ�����У�Ӧ�ж������ʱ��Connection�ĸ������������һ�� �׳��ض���TransactionException
	 * 
	 * Ȼ�󽫵�ǰconnection����ΪautoCommit = false;
	 * 
	 * 
	 * �����DatasourceGroup.setAutoCommit��ʱ�������쳣����Ӧ�ùر����ӣ�datasourceGroup���Զ����ԣ�
	 * �׳��쳣�� ��ʾ����ʧ�ܡ�
	 * 
	 * setAutoCommit���������������±�ʹ�ã���һ����connection�ոջ�ȡ��ʱ����ʱ��ֻ������
	 * ��־λ�������׳��쳣���ڶ��������������������Զ��ύ����������£����׳��쳣
	 * 
	 * Ӧ�ý���ǰ��־λ��Ϊ�رա����ر��ڲ��������ӡ�
	 * 
	 * @param isAutoCommit
	 */
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		checkClosed();
		// ���ų����������״̬,true==true ��false == false
		if (this.isAutoCommit == autoCommit) {
			// ʲôҲ����
			return;
		}
		List<SQLException> sqlExceptions = null;
		if (autoCommit) {
			this.isAutoCommit = autoCommit;
			// this.autoCommit false -> true
			// ���������������������ü�����Ϊ1�����ʾ����ʹ�ã����ֻᱻ��ʾ������
			// autoCommitΪtrue.�Ӷ���ʧ�������и��¡�
			// ��autoCommit false-> true
			sqlExceptions = setAutoCommitFalse2True(autoCommit, sqlExceptions);
		} else {

			// �������holdability��Ҫ��Ҫ��ʾ�Ĺر����е�resultSet
			Iterator<TStatementImp> iterator = openedStatements.iterator();
			while (iterator.hasNext()) {
				TStatementImp it = iterator.next();
				if (!it.isCurrentRSClosedOrNull()) {
					try {
						it.getResultSet().close();
					} catch (SQLException e) {
						sqlExceptions = appendToExceptionList(sqlExceptions, e);
					}
				}
			}
			this.isAutoCommit = autoCommit;
			// this.autoCommit == true |autoCommit == false;
			// this.autoCommit true ->false
			sqlExceptions = setAutoCommitTrue2False(autoCommit, sqlExceptions);
		}
		// �׳��쳣��
		if (sqlExceptions != null && !sqlExceptions.isEmpty()) {
			throw ExceptionUtils.mergeException(sqlExceptions);
		}
	}

	/**
	 * 
	 * @param autoCommit
	 * @param sqlExceptions
	 * @return
	 * @throws SQLException
	 *             ���������������Ԥ��ʱ�����Ѿ�ȷ���ڲ���������쳣��û�������쳣��
	 */
	protected List<SQLException> setAutoCommitTrue2False(boolean autoCommit,
			List<SQLException> sqlExceptions) throws SQLException {

		validTransactionCondition(false);

		for (Entry<String, Connection> entry : connectionMap.entrySet()) {
			if (isTransactionConnection(entry.getKey())) {
				sqlExceptions = setAutoCommitAndPutSQLExceptionToList(
						autoCommit, sqlExceptions, entry);
			}
		}
		return sqlExceptions;
	}

	/**
	 * ��ʱ��ֻ�п�����maxTransactionDSCount��
	 * �����ǰ�������е�statementΪ0������ʾ���������û��һ��Tstatement����ʹ������.��ô��ʱ��Ὣ���ӹرա�
	 * �����ǰ�����е�statement������Ϊ0��
	 * �����ʾ�������������Tstatement������ʹ�����ӣ���ʱ��ֻ�ǽ����ӵ�״̬��ΪautoCommit
	 * 
	 * @param autoCommit
	 * @param sqlExceptions
	 * @param clearRetryableDSGroup
	 * @return
	 */
	protected List<SQLException> setAutoCommitFalse2True(boolean autoCommit,
			List<SQLException> sqlExceptions) {
		boolean closeAndclearRetryableDSGroup = true;
		// �ж��Ƿ���Թ黹��ǰ���ӣ��ۺ���˵��ֻҪ������
		// resultSetδ�رգ��Ͳ��ܹ黹���ӣ����statement�ڵ�resultSetΪ�գ�����resultSet�Ѿ�ȫ���رգ�����Թرյ�ǰ���ӡ�
		Iterator<TStatementImp> iterator = openedStatements.iterator();
		while (iterator.hasNext()) {
			TStatementImp it = iterator.next();
			if (!it.isCurrentRSClosedOrNull()) {
				// ֻҪ��һ��statment or preparedStatementû�йرգ��Ͳ��ܹرյ�ǰ���ӡ�
				closeAndclearRetryableDSGroup = false;
			}
		}

		for (Entry<String, Connection> entry : connectionMap.entrySet()) {
			// ����Ѿ�û�д򿪵�statement,����Ϊ��ǰconnection���Թر���
			try {
				if (isTransactionConnection(entry.getKey())) {
					// Ϊ�˱��������һ���ԣ�������������ʾ�ı�false->true
					sqlExceptions = setAutoCommitAndPutSQLExceptionToList(
							autoCommit, sqlExceptions, entry);
				}
				if (closeAndclearRetryableDSGroup) {
					Connection conn = entry.getValue();
					if (conn.getAutoCommit() != true) {
						log.info("trying to close a not auto commit connection ,connection map is "
								+ connectionMap);
					}
					conn.close();
				}
			} catch (SQLException e) {
				sqlExceptions = appendToExceptionList(sqlExceptions, e);
			}

		}
		if (closeAndclearRetryableDSGroup) {
			connectionMap.clear();
		}
		return sqlExceptions;
	}

	protected boolean isTransactionConnection(String dbIndex) {
		return true;
	}

	protected List<SQLException> setAutoCommitAndPutSQLExceptionToList(
			boolean autoCommit, List<SQLException> sqlExceptions,
			Entry<String, Connection> entry) {
		try {
			entry.getValue().setAutoCommit(autoCommit);
		} catch (SQLException e) {
			sqlExceptions = appendToExceptionList(sqlExceptions, e);
		}
		return sqlExceptions;
	}

	public boolean getAutoCommit() throws SQLException {
		checkClosed();
		return isAutoCommit;
	}

	/**
	 * ������ջ��棬�����Ƿ���TStatement��ʱ�������hint.
	 */
	public static void flush_hint() {
		flushOne(ThreadLocalString.ROUTE_CONDITION);
		flushOne(ThreadLocalString.DB_SELECTOR);
		flushOne(ThreadLocalString.RULE_SELECTOR);
	}

	private static void flushOne(String key) {
		RouteCondition rc = (RouteCondition) ThreadLocalMap.get(key);
		if (rc != null) {
			if (ROUTE_TYPE.FLUSH_ON_CLOSECONNECTION.equals(rc.getRouteType())) {
				ThreadLocalMap.put(key, null);
			}
		}
	}

	/**
	 * �رյ�˳���� �����ñ�־λ ; Ȼ��رճ��еĺ��� Ȼ�������Գ��к��ӵ����� ���ر��Լ����е���Դ ��������Լ��Գ�����Դ������
	 * 
	 * ����Ҫע����ǹر��Լ����е���Դ��ʱ��������Դ�����׳��쳣����ʱ������ֹ�ر����̡�
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void close() throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug("invoke close");
		}

		if (closed) {
			return;
		}

		closed = true;

		List<SQLException> exceptions = null;
		try {
			// �ر�statement
			for (TStatementImp stmt : openedStatements) {
				try {
					stmt.closeInterval(closeInvokedByTStatement);
				} catch (SQLException e) {
					exceptions = appendToExceptionList(exceptions, e);
				}
			}
			// �ر�connection
			for (Connection conn : connectionMap.values()) {
				try {
					conn.close();
				} catch (SQLException e) {
					exceptions = appendToExceptionList(exceptions, e);
				}
			}
		} finally {
			flush_hint();
			openedStatements.clear();
			openedStatements = null;
			connectionMap.clear();
			connectionMap = null;
			// sqlExecuteEvents.clear();
		}
		ExceptionUtils.throwSQLException(exceptions, "close tconnection",
				Collections.EMPTY_LIST);
	}

	public int getTransactionIsolation() throws SQLException {
		checkClosed();

		return transactionIsolation;
	}

	public void setTransactionIsolation(int transactionIsolation)
			throws SQLException {
		checkClosed();

		this.transactionIsolation = transactionIsolation;
	}

	public Connection getProxyConnection() {
		return this;
	}

	public void removeCurrentStatement(Statement statement) {
		if (!openedStatements.remove(statement)) {
			log.warn("current statmenet ��" + statement + " doesn't exist!");
		}
	}

	/*---------------------������δʵ�ֵķ���------------------------------*/

	public void rollback(Savepoint savepoint) throws SQLException {
		throw new UnsupportedOperationException("rollback");
	}

	public Savepoint setSavepoint() throws SQLException {
		throw new UnsupportedOperationException("setSavepoint");
	}

	public Savepoint setSavepoint(String name) throws SQLException {
		throw new UnsupportedOperationException("setSavepoint");
	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		throw new UnsupportedOperationException("releaseSavepoint");
	}

	public String getCatalog() throws SQLException {
		throw new UnsupportedOperationException("getCatalog");
	}

	public void setCatalog(String catalog) throws SQLException {
		throw new UnsupportedOperationException("setCatalog");
	}

	public int getHoldability() throws SQLException {
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	public void setHoldability(int holdability) throws SQLException {
		/*
		 * ����㿴�������ô��ϲ������ mysqlĬ����5.x��jdbc driver����Ҳû��ʵ��holdability ��
		 * ����Ĭ�϶���.CLOSE_CURSORS_AT_COMMIT Ϊ�˼����������Ҳ��ֻʵ��close����
		 */
		throw new UnsupportedOperationException("setHoldability");
	}

	public SQLWarning getWarnings() throws SQLException {
		return null;
	}

	public void clearWarnings() throws SQLException {
	}

	public Map<String, Class<?>> getTypeMap() throws SQLException {
		throw new UnsupportedOperationException("getTypeMap");
	}

	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		throw new UnsupportedOperationException("setTypeMap");
	}

	public String nativeSQL(String sql) throws SQLException {
		throw new UnsupportedOperationException("nativeSQL");
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		checkClosed();
		return new TDatabaseMetaData();
	}

	public Map<String, DataSource> getDsMap() {
		return dsMap;
	}

	public void setDsMap(Map<String, DataSource> dsMap) {
		this.dsMap = dsMap;
	}

	/**
	 * ���ֿɶ���д
	 */
	public boolean isReadOnly() throws SQLException {
		return false;
	}

	/**
	 * �����κ�����
	 */
	public void setReadOnly(boolean readOnly) throws SQLException {
		// do nothing
	}

	public void setHookPoints(HookPoints hookPoints) {
		this.hookPoints = hookPoints;
	}

	public HookPoints getHookPoints() {
		return hookPoints;
	}

	public TDSProperties getProperties() {
		return properties;
	}

	public void setProperties(TDSProperties properties) {
		this.properties = properties;
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

	public Clob createClob() throws SQLException {
		throw new SQLException("not support exception");
	}

	public Blob createBlob() throws SQLException {
		throw new SQLException("not support exception");
	}

	public NClob createNClob() throws SQLException {
		throw new SQLException("not support exception");
	}

	public SQLXML createSQLXML() throws SQLException {
		throw new SQLException("not support exception");
	}

	public boolean isValid(int timeout) throws SQLException {
		throw new SQLException("not support exception");
	}

	public void setClientInfo(String name, String value)
			throws SQLClientInfoException {
		throw new RuntimeException("not support exception");
	}

	public void setClientInfo(Properties properties)
			throws SQLClientInfoException {
		throw new RuntimeException("not support exception");
	}

	public String getClientInfo(String name) throws SQLException {
		throw new SQLException("not support exception");
	}

	public Properties getClientInfo() throws SQLException {
		throw new SQLException("not support exception");
	}

	public Array createArrayOf(String typeName, Object[] elements)
			throws SQLException {
		throw new SQLException("not support exception");
	}

	public Struct createStruct(String typeName, Object[] attributes)
			throws SQLException {
		throw new SQLException("not support exception");
	}

}
