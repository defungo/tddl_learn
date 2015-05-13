package com.taobao.tddl.client.jdbc;

import static com.taobao.tddl.client.util.ExceptionUtils.appendToExceptionList;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.client.RouteCondition;
import com.taobao.tddl.client.RouteCondition.ROUTE_TYPE;
import com.taobao.tddl.client.ThreadLocalString;
import com.taobao.tddl.client.databus.StartInfo;
import com.taobao.tddl.client.jdbc.TDataSource.TDSProperties;
import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlan;
import com.taobao.tddl.client.jdbc.listener.Context;
import com.taobao.tddl.client.jdbc.listener.Handler;
import com.taobao.tddl.client.jdbc.listener.HookPoints;
import com.taobao.tddl.client.jdbc.resultset.newImp.CountTResultSet;
import com.taobao.tddl.client.jdbc.resultset.newImp.DistinctTResultSet;
import com.taobao.tddl.client.jdbc.resultset.newImp.DummyTResultSet;
import com.taobao.tddl.client.jdbc.resultset.newImp.MaxTResultSet;
import com.taobao.tddl.client.jdbc.resultset.newImp.MinTResultSet;
import com.taobao.tddl.client.jdbc.resultset.newImp.OrderByTResultSet;
import com.taobao.tddl.client.jdbc.resultset.newImp.ShallowTResultSetWrapper;
import com.taobao.tddl.client.jdbc.resultset.newImp.SimpleTResultSet;
import com.taobao.tddl.client.jdbc.resultset.newImp.SumTResultSet;
import com.taobao.tddl.client.jdbc.sqlexecutor.RealSqlExecutor;
import com.taobao.tddl.client.jdbc.sqlexecutor.RealSqlExecutorImp;
import com.taobao.tddl.client.jdbc.sqlexecutor.SimpleRealSqlExecutorImp;
import com.taobao.tddl.client.jdbc.sqlexecutor.UpdateReturn;
import com.taobao.tddl.client.jdbc.sqlexecutor.parallel.ParallelRealSqlExecutor;
import com.taobao.tddl.client.jdbc.sqlexecutor.serial.SerialRealSqlExecutor;
import com.taobao.tddl.client.pipeline.DefaultPipelineFactory;
import com.taobao.tddl.client.pipeline.bootstrap.Bootstrap;
import com.taobao.tddl.client.util.ExceptionUtils;
import com.taobao.tddl.client.util.LogUtils;
import com.taobao.tddl.client.util.ThreadLocalMap;
import com.taobao.tddl.common.jdbc.ParameterContext;
import com.taobao.tddl.interact.monitor.TotalStatMonitor;
import com.taobao.tddl.interact.rule.bean.SqlType;
import com.taobao.tddl.sqlobjecttree.GroupFunctionType;
import com.taobao.tddl.sqlobjecttree.OrderByEle;
import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.DirectlyRouteCondition;

@SuppressWarnings("unchecked")
public class TStatementImp extends PreparedStatementExecutorCommon implements
		Statement {
	private TDSProperties properties;

	/**
	 * ���߹���
	 */
	protected final Bootstrap bootstrap;

	protected SerialRealSqlExecutor serialRealSqlExecutor;
	protected ParallelRealSqlExecutor parallelRealSqlExecutor;
	protected SerialRealSqlExecutor simpleSerialRealSqlExecutor;

	private static final Log log = LogFactory.getLog(TStatementImp.class);

	private static final Log sqlLog = LogFactory.getLog(LogUtils.TDDL_SQL_LOG);

	/**
	 * query time out . ��ʱʱ�䣬�����ʱʱ�䲻Ϊ0����ô��ʱӦ�ñ�set��������query�С�
	 */
	protected int queryTimeout = 0;
	protected int maxRows=0;
	protected int fetchSize=0;

	/**
	 * ���������Ľ����������ʹ�� getResult��������.
	 *
	 * һ��statementֻ������һ�������
	 */
	protected DummyTResultSet currentResultSet;
	/**
	 * ò����ֻ�д洢�����л���ֶ����� ��˲�֧��
	 */
	protected boolean moreResults;
	/**
	 * ���¼��������ִ���˶�Σ���ô���ֵֻ�᷵�����һ��ִ�еĽ���� �����һ��query����ô���ص�����Ӧ����-1
	 */
	protected int updateCount;
	/**
	 * �жϵ�ǰstatment �Ƿ��ǹرյ�
	 */
	protected boolean closed;

	private int resultSetType = -1;

	private int resultSetConcurrency = -1;

	private int resultSetHoldability = -1;

	protected List<String> batchedArgs;

	private HookPoints hookPoints;
	private Context context;

	protected static void dumpSql(String originalSql,
			Map<String, List<RealSqlContext>> targets,
			Map<Integer, ParameterContext> parameters) {
		if (sqlLog.isDebugEnabled()) {
			StringBuilder buffer = new StringBuilder();
			buffer.append("\n[original sql]:").append(originalSql.trim())
					.append("\n");
			for (Entry<String, List<RealSqlContext>> entry : targets.entrySet()) {
				for (RealSqlContext targetSql : entry.getValue()) {
					buffer.append(" [").append(entry.getKey()).append(".")
							.append(targetSql.getRealTable()).append("]:")
							.append(targetSql.getSql().trim()).append("\n");
				}
			}

			if (parameters != null && !parameters.isEmpty()
					&& !parameters.values().isEmpty()) {
				buffer.append("[parameters]:").append(
						parameters.values().toString());
			}

			sqlLog.debug(buffer.toString());
		}

		//����־
		for (Entry<String, List<RealSqlContext>> entry : targets.entrySet()) {
			for (RealSqlContext rsc : entry.getValue()) {
				StringBuilder sb=new StringBuilder();
				sb.append(entry.getKey());
				sb.append(TotalStatMonitor.logFieldSep);
				sb.append(rsc.getRealTable());
				TotalStatMonitor.dbTabIncrement(sb.toString());
			}
		}
	}

	public TStatementImp(ConnectionManager connectionManager,
			Bootstrap bootstrap) {
		super(connectionManager);
		this.bootstrap = bootstrap;
	}

	public int executeUpdate(String sql) throws SQLException {
		return executeUpdateInternal(sql, -1, null, null);
	}

	public int executeUpdate(String sql, int autoGeneratedKeys)
			throws SQLException {
		return executeUpdateInternal(sql, autoGeneratedKeys, null, null);
	}

	public int executeUpdate(String sql, int[] columnIndexes)
			throws SQLException {
		return executeUpdateInternal(sql, -1, columnIndexes, null);
	}

	public int executeUpdate(String sql, String[] columnNames)
			throws SQLException {
		return executeUpdateInternal(sql, -1, null, columnNames);
	}

	protected RouteCondition getRouteContiongFromThreadLocal(String key) {
		RouteCondition rc = (RouteCondition) ThreadLocalMap.get(key);
		if (rc != null) {
			ROUTE_TYPE routeType = rc.getRouteType();
			if (ROUTE_TYPE.FLUSH_ON_EXECUTE.equals(routeType)) {
				ThreadLocalMap.put(key, null);
			}
		}
		return rc;
	}

	public void closeInterval(boolean closeInvokeByCurrTStatement)
			throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug("invoke close");
		}
		if (closed) {
			return;
		}

		List<SQLException> exceptions = null;

		closed = true;
		try {
			// �رպ���
			try {
				// bug fix by shenxun :�ڲ�������remove,��TStatment��ͳһclear������
				if (currentResultSet != null) {
					currentResultSet.closeInternal();
				}

			} catch (SQLException e) {
				exceptions = appendToExceptionList(exceptions, e);
			}
			// @IMPORTANT: ��ΪĿǰStatementû�г���ʹ�ù���dbIndex�����ã���������ﲻ�ܺܺõĹر�����
			// ��Ϊʹ�ö��TStatement�ĳ���̫�٣���������ط����Ż��ǲ��걸�ġ���ʹ�ö��TStatement��ʱ�򣬾�ֻ���������ⲿ��ʾ�ĵ���close()�����ˡ�
		} finally {
			closed = true;
			currentResultSet = null;
			if (closeInvokeByCurrTStatement) {
				connectionManager.removeCurrentStatement(this);
			}
		}
		ExceptionUtils.throwSQLException(exceptions, "close",
				Collections.emptyList());
	}

	/**
	 * update �ĺ��ķ���
	 *
	 * ��ѭ�ļ���������������: 1. �𼶹��� 2. ��һ������ʧ�ܣ���ô��ǰ����Դ���и��¶�ʧ�ܡ� 3.
	 * �������ݿ�ĸ��»��ǻ����ִ�еģ�������ͳһ�׳��쳣��
	 *
	 * �������һ�����ݿ�ִ�и��£��������������У���ô�������ݿ�ȫ������������� ����������ǲ�����������
	 *
	 * @param sql
	 * @param autoGeneratedKeys
	 * @param columnIndexes
	 * @param columnNames
	 * @param sqlParam
	 * @return
	 * @throws SQLException
	 */
	protected int executeUpdateInternal(String sql, int autoGeneratedKeys,
			int[] columnIndexes, String[] columnNames,
			Map<Integer, ParameterContext> sqlParam, SqlType sqlType,
			TStatementImp statementImp) throws SQLException {
		try {
			return executeUpdateInternalInTry(sql, autoGeneratedKeys,
					columnIndexes, columnNames, sqlParam, sqlType, statementImp);
		} finally {
			if (this.connectionManager.getAutoCommit()) {
				this.context.reset();
			}
		}
	}

	protected int executeUpdateInternalInTry(String sql, int autoGeneratedKeys,
			int[] columnIndexes, String[] columnNames,
			Map<Integer, ParameterContext> sqlParam, SqlType sqlType,
			TStatementImp tStatementImp) throws SQLException {

		checkClosed();
		ensureResultSetIsEmpty();
		long startTime = System.currentTimeMillis();
		ExecutionPlan context = buildSqlExecutionContextUsePipeline(sql,
				sqlParam, sqlType);

		if (context.getEvents() != null) {
			this.context.getEvents().addAll(context.getEvents());
		}

		if (context.mappingRuleReturnNullValue()) {
			return 0;
		}

		beforeSqlExecute();

		int tablesSize = 0;
		Map<String, List<RealSqlContext>> sqlMap = context.getSqlMap();
		int databaseSize = sqlMap.size();

		dumpSql(sql, sqlMap, null);

		int affectedRows = 0;

		List<SQLException> exceptions = new LinkedList<SQLException>();
		Set<Entry<String, List<RealSqlContext>>> set = sqlMap.entrySet();

		RealSqlExecutor rse = new RealSqlExecutorImp(parallelRealSqlExecutor,
				serialRealSqlExecutor, tStatementImp, context);

		context.setAutoGeneratedKeys(autoGeneratedKeys);
		context.setColumnIndexes(columnIndexes);
		context.setColumnNames(columnNames);

		for (Entry<String/* dbIndex */, List<RealSqlContext>> entry : set) {
			UpdateReturn ur = null;
			try {
				ur = rse.update();
			} catch (SQLException e) {
				exceptions.add(e);
				break;
			}

			affectedRows += ur.getAffectedRows();
			exceptions.addAll(ur.getExceptions());
			tablesSize += entry.getValue().size();
		}

		long elapsedTime = System.currentTimeMillis() - startTime;

		ExceptionUtils.throwSQLException(exceptions, sql, sqlParam);

		this.currentResultSet = null;
		this.moreResults = false;
		this.updateCount = affectedRows;
		this.context.setAffectedRows(affectedRows);
		profileUpdate(sql, context, tablesSize, databaseSize, exceptions,
				elapsedTime);

		afterSqlExecute();
		return affectedRows;
	}

	// update����Ҫ����
	private int executeUpdateInternal(String sql, int autoGeneratedKeys,
			int[] columnIndexes, String[] columnNames) throws SQLException {
		SqlType sqlType = DefaultPipelineFactory.getSqlType(sql);
		return executeUpdateInternal(sql, autoGeneratedKeys, columnIndexes,
				columnNames, null, sqlType, this);
	}

	protected void profileUpdate(String sql, ExecutionPlan context,
			int tablesSize, int databaseSize, List<SQLException> exceptions,
			long elapsedTime) throws SQLException {
		profileWithException(exceptions, context.getVirtualTableName()
				.toString(), sql, elapsedTime);
		profileNumberOfDBAndTablesAndDuringTime(context.getVirtualTableName()
				.toString(), databaseSize, tablesSize, sql, elapsedTime);

	}

	protected void afterSqlExecute() throws SQLException {
		if (connectionManager.getAutoCommit()) {
			// ��¼һ��ִ�зֿ⸴����ǰ��ʱ�䡣
			if (hookPoints.getAfterExecute() != Handler.DUMMY_HANDLER
					&& !context.isEventsEmpty()) {
				for (SqlExecuteEvent event : context.getEvents()) {
					event.setAfterMainDBSqlExecuteTime(System
							.currentTimeMillis());
				}
			}
			hookPoints.getAfterExecute().execute(context);
		}
	}

	private Statement createStatementInternal(Connection connection)
			throws SQLException {
		Statement stmt;
		if (this.resultSetType != -1 && this.resultSetConcurrency != -1
				&& this.resultSetHoldability != -1) {
			stmt = connection.createStatement(this.resultSetType,
					this.resultSetConcurrency, this.resultSetHoldability);
		} else if (this.resultSetType != -1 && this.resultSetConcurrency != -1) {
			stmt = connection.createStatement(this.resultSetType,
					this.resultSetConcurrency);
		} else {
			stmt = connection.createStatement();
		}
		return stmt;
	}

	private boolean executeInternal(String sql, int autoGeneratedKeys,
			int[] columnIndexes, String[] columnNames) throws SQLException {

		SqlType sqlType = DefaultPipelineFactory.getSqlType(sql);
		if (sqlType == SqlType.SELECT || sqlType == SqlType.SELECT_FOR_UPDATE ||sqlType == SqlType.SHOW) {
			executeQuery(sql);
			return true;
		} else if (sqlType == SqlType.INSERT || sqlType == SqlType.UPDATE
				|| sqlType == SqlType.DELETE || sqlType == SqlType.REPLACE||sqlType==SqlType.TRUNCATE) {
			if (autoGeneratedKeys == -1 && columnIndexes == null
					&& columnNames == null) {
				executeUpdate(sql);
			} else if (autoGeneratedKeys != -1) {
				executeUpdate(sql, autoGeneratedKeys);
			} else if (columnIndexes != null) {
				executeUpdate(sql, columnIndexes);
			} else if (columnNames != null) {
				executeUpdate(sql, columnNames);
			} else {
				executeUpdate(sql);
			}

			return false;
		} else {
			throw new SQLException(
					"only select, insert, update, delete,truncate sql is supported");
		}
	}

	public boolean execute(String sql) throws SQLException {
		return executeInternal(sql, -1, null, null);
	}

	public boolean execute(String sql, int autoGeneratedKeys)
			throws SQLException {
		return executeInternal(sql, autoGeneratedKeys, null, null);
	}

	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		return executeInternal(sql, -1, columnIndexes, null);
	}

	public boolean execute(String sql, String[] columnNames)
			throws SQLException {
		return executeInternal(sql, -1, null, columnNames);
	}

	public void addBatch(String sql) throws SQLException {
		checkClosed();
		if (batchedArgs == null) {
			batchedArgs = new LinkedList<String>();
		}
		if (sql != null) {
			batchedArgs.add(sql);
		}
	}

	public void clearBatch() throws SQLException {
		checkClosed();
		if (batchedArgs != null) {
			batchedArgs.clear();
		}
	}

	public void close() throws SQLException {
		closeInterval(true);
	}

	public int[] executeBatch() throws SQLException {
		checkClosed();
		ensureResultSetIsEmpty();
//		����ط��ȵ�ִ�мƻ�����֮�󣬷���ȷʵ�п������������Ծܾ���
//		if (!connectionManager.getAutoCommit()) {
//			throw new SQLException("executeBatch�ݲ�֧������");
//		}
		if (batchedArgs == null || batchedArgs.isEmpty()) {
			return new int[0];
		}

		List<SQLException> exceptions = new ArrayList<SQLException>();
		List<Integer> result = new ArrayList<Integer>();
		Map<String/* ����ԴID */, List<String/* ����Դ��ִ�е�SQL */>> sqls = null;
		try {
			DirectlyRouteCondition ruleCondition = (DirectlyRouteCondition) getRouteContiongFromThreadLocal(ThreadLocalString.RULE_SELECTOR);

			// if (directlyRouteCondition != null) {
			// // ��ֱ��·�ɵ�condition
			// String dbRuleId = directlyRouteCondition.getDbRuleID();
			// if (connectionManager.containDBIndex(dbRuleId)) {
			// // ������ֱ��ִ��sql
			// throw new SQLException("batch not support");
			// } else {
			// // ������Ŀ��id
			// // ��ô�ж�һ�µ�ǰrc�����Ƿ�����Ҫ�滻�ı������������Ҫ�滻��
			// // ���������׳��쳣��Ŀ�����ݿ�δ�ҵ������û��Ҫ�滻�ı��������߹���ѡ��
			// if
			// (directlyRouteCondition.getShardTableMap().get(dbRuleId).isEmpty())
			// {
			// sqls = sortBatch(batchedArgs, dbRuleId);
			// } else {
			// throw new SQLException("can't find target db : "
			// + dbRuleId);
			// }
			// }

			if (ruleCondition != null) {
				String dbRuleId = ruleCondition.getDbRuleID();
				sqls = sortBatch(batchedArgs, dbRuleId);
			} else {
				sqls = sortBatch(batchedArgs, null);
			}
			//add by jiechen.qzm batch��֧�ֿ������
			if(sqls.size() > 1 && !connectionManager.getAutoCommit()) {
				throw new SQLException("executeBatch�ݲ�֧�ֿ�����񣬸������漰 " + sqls.size() + " ���⡣");
			}

			for (Entry<String, List<String>> entry : sqls.entrySet()) {
				String dbSelectorID = entry.getKey();
				List<Integer> list = null;
				try {
					// ������ʾ��ʹ��go slaveΪfalse
					// ��ʺ����
					Connection conn = connectionManager.getConnection(
							dbSelectorID, false);
					try {
						list = executeBatchOnOneConnAndCloseStatement(
								exceptions, entry.getValue(), conn);
						result.addAll(list);
					} finally {
						exceptions = tryCloseConnection(exceptions,
								dbSelectorID);
					}
				} catch (SQLException e) {
					exceptions = appendToExceptionList(exceptions, e);
				}
			}
		} finally {
			batchedArgs.clear();
		}
		currentResultSet = null;
		moreResults = false;
		updateCount = 0;
		ExceptionUtils.throwSQLException(exceptions, "batch",
				Collections.EMPTY_MAP);

		
		return fromListToArray(result);
//		return new int[0];
	}

	public Map<String, List<String>> sortBatch(List<String> sql,
			String selectKey) throws SQLException {
		Map<String, List<String>> targetSqls = new HashMap<String, List<String>>(
				8);
		StartInfo startInfo=new StartInfo();
		for (String originalSql : sql) {
			startInfo.setSql(originalSql);
			startInfo.setSqlType(DefaultPipelineFactory.getSqlType(originalSql));
			bootstrap.bootstrapForBatch(startInfo, false,
					targetSqls, selectKey);
		}
		return targetSqls;
	}

	public Map<String, Map<String, List<List<ParameterContext>>>> sortPreparedBatch(
			String sql, List<Map<Integer, ParameterContext>> batchedParameters,
			String selectKey) throws SQLException {
		Map<String, Map<String, List<List<ParameterContext>>>> targetSqls = new HashMap<String, Map<String, List<List<ParameterContext>>>>(
				16);
		StartInfo startInfo=new StartInfo();
		startInfo.setSql(sql);
		startInfo.setSqlType(DefaultPipelineFactory.getSqlType(sql));
		for (Map<Integer, ParameterContext> map : batchedParameters) {
			startInfo.setSqlParam(map);
			bootstrap.bootstrapForPrepareBatch(startInfo, false, targetSqls,
					selectKey);
		}
		return targetSqls;
	}

	/**
	 * ��������ȱ�ע�͵�����Ϊ�޷����� batch�Ľ����ֻ��SqlException�ǲ����ġ�
	 * @param exceptions
	 * @param sqls
	 * @param conn
	 * @return
	 *//*
	protected List<SQLException> executeBatchOnOneConnAndCloseStatement(
			List<SQLException> exceptions, List<String> sqls, Connection conn) {
		try {
			Statement stmt = createStatementInternal(conn);

			try {
				try {
					for (String targetSql : sqls) {
						stmt.addBatch(targetSql);
					}
					stmt.executeBatch();
				} catch (SQLException e) {
					exceptions = appendToExceptionList(exceptions, e);
				}
				stmt.clearBatch();
			} finally {
				stmt.close();
			}
		} catch (SQLException e) {
			exceptions = appendToExceptionList(exceptions, e);
		}
		return exceptions;
	}*/
	
	/**
	 * ����Ӱ��������list,����sql�Ļ�ֱ��append
	 * @param exceptions
	 * @param sqls
	 * @param conn
	 * @return
	 */
	protected List<Integer> executeBatchOnOneConnAndCloseStatement(
			List<SQLException> exceptions, List<String> sqls, Connection conn) {
		List<Integer> result = new ArrayList<Integer>();
		try {
			Statement stmt = createStatementInternal(conn);
			try {
				try {
					int[] temp = null;
					for (String targetSql : sqls) {
						stmt.addBatch(targetSql);
					}
					temp = stmt.executeBatch();
					result.addAll(fromArrayToList(temp));
				} catch (SQLException e) {
					exceptions = appendToExceptionList(exceptions, e);
				}
				stmt.clearBatch();
			} finally {
				stmt.close();
			}
		} catch (SQLException e) {
			exceptions = appendToExceptionList(exceptions, e);
		}
		return result;
	}
		
	/**
	 * �� int[] �� List<Integer> ��׼��
	 * @param array
	 * @return
	 */
	public static List<Integer> fromArrayToList(int[] array){
		if(array == null) {
			return null;
		}
		List<Integer> result = new ArrayList<Integer>();
		for(int num : array){
			result.add(num);
		}
		return result;
	}
	
	/**
	 * �� List<Integer> �� int[] ��׼��
	 * @param list
	 * @return
	 */
	public static int[] fromListToArray(List<Integer> list){
		if(list == null) {
			return null;
		}
		int[] result = new int[list.size()];
		for(int i=0; i<list.size(); i++) {
			result[i] = list.get(i);
		}
		return result;
	}

	protected void checkClosed() throws SQLException {
		if (closed) {
			throw new SQLException(
					"No operations allowed after statement closed.");
		}
	}

	/**
	 * ����½��˲�ѯ����ô��һ�β�ѯ�Ľ����Ӧ�ñ���ʾ�Ĺرյ�������Ƿ���jdbc�淶��
	 *
	 * @throws SQLException
	 */
	protected void ensureResultSetIsEmpty() throws SQLException {

		if (currentResultSet != null) {
			log.debug("result set is not null,close current result set");
			try {
				currentResultSet.close();
			} catch (SQLException e) {
				log.error(
						"exception on close last result set . can do nothing..",
						e);
			} finally {
				// ����Ҫ��ʾ�Ĺر���
				currentResultSet = null;
			}
		}
	}

	protected ResultSet executeQueryInternal(String sql,
			Map<Integer, ParameterContext> originalParameterSettings,
			SqlType sqlType, TStatementImp tStatementImp) throws SQLException {
		checkClosed();
		ensureResultSetIsEmpty();
		ExecutionPlan context = null;
		context = buildSqlExecutionContextUsePipeline(sql,
				originalParameterSettings, sqlType);

		/*
		 * modified by shenxun: ������Ҫ�Ǵ���mappingRule���ؿյ�����£�Ӧ�÷��ؿս����
		 */
		if (context.mappingRuleReturnNullValue()) {
			this.currentResultSet = getEmptyResultSet(this);
			return currentResultSet;
		}
		// int tablesSize = 0;
		dumpSql(sql, context.getSqlMap(), originalParameterSettings);

		// beforeSqlExecute();

		DummyTResultSet result = null;
		// ���������׳��쳣�����쳣��ʾ�龡�������û��һ���ɷ��ص�
		result = mergeResultSets(this, connectionManager, context);

		this.currentResultSet = result;
		this.moreResults = false;
		this.updateCount = -1;
		// ��¼�򿪵�resultSet,����һ��ر�ʱ���ҵõ�

		// afterSqlExecute();
		if (connectionManager.getAutoCommit()) {
			this.context.reset();
		}
		return result;
	}

	public ResultSet executeQuery(String sql) throws SQLException {
		SqlType sqlType = DefaultPipelineFactory.getSqlType(sql);
		return executeQueryInternal(sql, null, sqlType, this);
	}

	// TODO : ʹ�ò���ģʽ���кϲ�����֧�ֲ�����ѯ�ͷǲ�����ѯ
	protected DummyTResultSet mergeResultSets(TStatementImp tStatementImp,
			ConnectionManager connectionManager, ExecutionPlan context)
			throws SQLException {

		if (context.getOrderByColumns() != null
				&& !context.getOrderByColumns().isEmpty()
				&& context.getGroupFunctionType() != GroupFunctionType.NORMAL) {
			throw new SQLException(
					"'group function' and 'order by' can't be together!");
		}
		// ��������н���һ������Դ������ֻ��һ������ô����򵥵�ģʽ��
		/*
		 * ��ģʽ����ֱ��ȥ���ݿ�ִ�У�ʲô������������ģʽ�¿���֧���κβ�ѯ������Ҫ�ϲ�������Ҫ
		 * ���������顣����ȥ���ݿ��ѯ��Ȼ�����statement����resultSet.���ɣ����в��� ֱ��ͨ����������С�����Ҫ�������жϡ�
		 */
		Map<String, List<RealSqlContext>> map = context.getSqlMap();
		if (map.size() == 1) {
			for (List<RealSqlContext> rscs : map.values()) {
				if (rscs.size() == 1) {
					{
						return new ShallowTResultSetWrapper(tStatementImp,
								connectionManager, context);
					}
				}
			}
		}

		RealSqlExecutor rse = new RealSqlExecutorImp(parallelRealSqlExecutor,
				serialRealSqlExecutor, tStatementImp, context);

		// �����ave��ô����֧��
		if (context.getGroupFunctionType() == GroupFunctionType.AVG) {
			throw new SQLException(
					"The group function 'AVG' is not supported now!");
		} else if (context.getGroupFunctionType() == GroupFunctionType.COUNT) {
			// �����count����кϲ�
			return new CountTResultSet(tStatementImp, connectionManager,
					context, rse);
		} else if (context.getGroupFunctionType() == GroupFunctionType.MAX) {
			// �����
			return new MaxTResultSet(tStatementImp, connectionManager, context,
					rse);
		} else if (context.getGroupFunctionType() == GroupFunctionType.MIN) {
			return new MinTResultSet(tStatementImp, connectionManager, context,
					rse);
		} else if (context.getGroupFunctionType() == GroupFunctionType.SUM) {
			return new SumTResultSet(tStatementImp, connectionManager, context,
					rse);
		} else if (context.getDistinctColumns() != null&&context.getDistinctColumns().size()!=0) {
			// ��ʱ�϶��Ѿ��ǵ�������߶����case��,
			DistinctTResultSet rs=new DistinctTResultSet(tStatementImp, connectionManager,
					context, rse);
			rs.setDistinctColumn(context.getDistinctColumns());
			return rs;
		} else if (context.getOrderByColumns() != null
				&& !context.getOrderByColumns().isEmpty()) {
			OrderByColumn[] orderByColumns = new OrderByColumn[context
					.getOrderByColumns().size()];
			int i = 0;
			for (OrderByEle element : context.getOrderByColumns()) {
				orderByColumns[i] = new OrderByColumn();
				orderByColumns[i].setColumnName(element.getName());
				orderByColumns[i++].setAsc(element.isASC());
			}
			OrderByTResultSet orderByTResultSet = new OrderByTResultSet(
					tStatementImp, connectionManager, context, rse);
			orderByTResultSet.setOrderByColumns(orderByColumns);
			orderByTResultSet.setLimitFrom(context.getSkip());
			orderByTResultSet.setLimitTo(context.getMax());
			return orderByTResultSet;
		} else {
			/**
			 * ����е�����
			 */
			RealSqlExecutor spe = new SimpleRealSqlExecutorImp(
					parallelRealSqlExecutor, simpleSerialRealSqlExecutor,
					tStatementImp, context);
			SimpleTResultSet simpleTResultSet = new SimpleTResultSet(
					tStatementImp, connectionManager, context, spe);
			simpleTResultSet.setLimitFrom(context.getSkip());
			simpleTResultSet.setLimitTo(context.getMax());
			return simpleTResultSet;
		}
		// ��ʱ�ų��߶�����GroupBy,Having,Distinct
	}

	protected void beforeSqlExecute() throws SQLException {
		if (connectionManager.getAutoCommit()) {
			hookPoints.getBeforeExecute().execute(context);
		}
	}

	public Connection getConnection() throws SQLException {
		return connectionManager.getProxyConnection();
	}

	private ExecutionPlan buildSqlExecutionContextUsePipeline(String sql,
			Map<Integer, ParameterContext> originalParameterSettings,
			SqlType sqlType) throws SQLException {
		StartInfo startInfo=new StartInfo();
		startInfo.setSql(sql);
		startInfo.setSqlType(sqlType);
		startInfo.setSqlParam(originalParameterSettings);
		return this.bootstrap.bootstrap(startInfo);
	}

	/**
	 * ����Ϊ��֧�ֵķ���
	 */
	public int getFetchDirection() throws SQLException {
		throw new UnsupportedOperationException("getFetchDirection");
	}

	public int getFetchSize() throws SQLException {
		return this.fetchSize;
	}

	public int getMaxFieldSize() throws SQLException {
		throw new UnsupportedOperationException("getMaxFieldSize");
	}

	public int getMaxRows() throws SQLException {
		return this.maxRows;
	}

	public boolean getMoreResults() throws SQLException {
		return moreResults;
	}

	public int getQueryTimeout() throws SQLException {
		return queryTimeout;
	}

	public void setQueryTimeout(int queryTimeout) throws SQLException {
		this.queryTimeout = queryTimeout;
	}

	public void setCursorName(String cursorName) throws SQLException {
		throw new UnsupportedOperationException("setCursorName");
	}

	public void setEscapeProcessing(boolean escapeProcessing)
			throws SQLException {
		throw new UnsupportedOperationException("setEscapeProcessing");
	}

	public SQLWarning getWarnings() throws SQLException {
		return null;
	}

	public void clearWarnings() throws SQLException {
	}

	public boolean getMoreResults(int current) throws SQLException {
		throw new UnsupportedOperationException("getMoreResults");
	}

	public ResultSet getResultSet() throws SQLException {
		return currentResultSet;
	}

	public int getResultSetConcurrency() throws SQLException {
		return resultSetConcurrency;
	}

	public int getResultSetHoldability() throws SQLException {
		return resultSetHoldability;
	}

	public int getResultSetType() throws SQLException {
		return resultSetType;
	}

	public int getUpdateCount() throws SQLException {
		return updateCount;
	}

	public void setFetchDirection(int fetchDirection) throws SQLException {
		throw new UnsupportedOperationException("setFetchDirection");
	}

	public void setFetchSize(int fetchSize) throws SQLException {
		this.fetchSize=fetchSize;
	}

	public void setMaxFieldSize(int maxFieldSize) throws SQLException {
		throw new UnsupportedOperationException("setMaxFieldSize");
	}

	public void setMaxRows(int maxRows) throws SQLException {
		this.maxRows=maxRows;
	}

	public ResultSet getGeneratedKeys() throws SQLException {
		throw new UnsupportedOperationException("getGeneratedKeys");
	}

	public void cancel() throws SQLException {
		throw new UnsupportedOperationException("cancel");
	}

	public boolean isCurrentRSClosedOrNull() {
		return currentResultSet == null ? true : currentResultSet.isClosed();
	}

	public void setHookPoints(HookPoints hookPoints) {
		this.hookPoints = hookPoints;
	}

	public HookPoints getHookPoints() {
		return hookPoints;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public Context getContext() {
		return context;
	}

	public int getQueryTimeOut() {
		return queryTimeout;
	}

	public void setResultSetType(int resultSetType) {
		this.resultSetType = resultSetType;
	}

	public void setResultSetConcurrency(int resultSetConcurrency) {
		this.resultSetConcurrency = resultSetConcurrency;
	}

	public void setResultSetHoldability(int resultSetHoldability) {
		this.resultSetHoldability = resultSetHoldability;
	}

	public TDSProperties getProperties() {
		return properties;
	}

	public void setProperties(TDSProperties properties) {
		this.properties = properties;
	}

	public void setSerialRealSqlExecutor(
			SerialRealSqlExecutor serialRealSqlExecutor) {
		this.serialRealSqlExecutor = serialRealSqlExecutor;
	}

	public void setParallelRealSqlExecutor(
			ParallelRealSqlExecutor parallelRealSqlExecutor) {
		this.parallelRealSqlExecutor = parallelRealSqlExecutor;
	}

	public void setSimpleSerialRealSqlExecutor(
			SerialRealSqlExecutor simpleSerialRealSqlExecutor) {
		this.simpleSerialRealSqlExecutor = simpleSerialRealSqlExecutor;
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return this.getClass().isAssignableFrom(iface);
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		try {
			return (T) this;
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}

	public boolean isClosed() throws SQLException {
		return closed;
	}

	public void setPoolable(boolean poolable) throws SQLException {
		throw new SQLException("not support exception");
	}

	public boolean isPoolable() throws SQLException {
		throw new SQLException("not support exception");
	}
}
