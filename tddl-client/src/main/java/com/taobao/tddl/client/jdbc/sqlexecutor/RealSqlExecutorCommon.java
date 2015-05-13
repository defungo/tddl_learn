package com.taobao.tddl.client.jdbc.sqlexecutor;

import static com.taobao.tddl.client.util.ExceptionUtils.appendToExceptionList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.client.jdbc.ConnectionManager;
import com.taobao.tddl.client.jdbc.PreparedStatementExecutorCommon;
import com.taobao.tddl.client.jdbc.RealSqlContext;
import com.taobao.tddl.client.jdbc.TPreparedStatementImp;
import com.taobao.tddl.client.jdbc.TStatementImp;
import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlan;

/**
 * @author junyu
 * 
 */
public class RealSqlExecutorCommon extends PreparedStatementExecutorCommon {
	private static final Log logger = LogFactory
			.getLog(RealSqlExecutorCommon.class);

	public RealSqlExecutorCommon(ConnectionManager connectionManager) {
		super(connectionManager);
	}

	private int resultSetType = -1;
	private int resultSetConcurrency = -1;
	private int resultSetHoldability = -1;
	private int queryTimeout;
	private int fetchSize;
	private int maxRows;

	private int autoGeneratedKeys = -1;
	private int[] columnIndexes;
	private String[] columnNames;

	/**
	 * 检测当前线程是不是Interrupted
	 * 
	 * @throws SQLException
	 */
	protected void checkThreadState() throws SQLException {
		if (Thread.currentThread().isInterrupted()) {
			throw new SQLException("current thread is interrupted!");
		}
	}

	/**
	 * 判定当前的Statement是否是PrepareStatement
	 * 
	 * @param tStatement
	 * @return
	 */
	protected boolean isPreparedStatement(TStatementImp tStatement) {
		if (tStatement instanceof TPreparedStatementImp) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 执行基于Statement的查询
	 * 
	 * @param connection
	 * @param sql
	 * @return
	 */
	protected QueryReturn executeQueryIntervalST(Connection connection,
			RealSqlContext sql) throws SQLException {
		QueryReturn qr = new QueryReturn();
		Statement stmt = createStatementInternal(connection);
		stmt.setQueryTimeout(queryTimeout);
		stmt.setFetchSize(fetchSize);
		stmt.setMaxRows(maxRows);
		
		ResultSet resultset = stmt.executeQuery(sql.getSql());
		qr.setResultset(resultset);
		qr.setStatement(stmt);
		return qr;
	}

	/**
	 * 执行基于PreparedStatement的查询
	 * 
	 * @param connection
	 * @param sql
	 * @return
	 */
	protected QueryReturn executeQueryIntervalPST(Connection connection,
			RealSqlContext sql) throws SQLException {
		QueryReturn qr = new QueryReturn();
		PreparedStatement stmt = prepareStatementInternal(connection, sql
				.getSql());
		stmt.setQueryTimeout(queryTimeout);
		stmt.setFetchSize(fetchSize);
		stmt.setMaxRows(maxRows);
		
		setParameters(stmt, sql.getArgument());
		ResultSet resultset = stmt.executeQuery();
		qr.setResultset(resultset);
		qr.setStatement(stmt);
		return qr;
	}

	/**
	 * 执行基于Statement的数据更新
	 * 
	 * @param executionPlan
	 * @param dbEntry
	 * @return
	 */
	protected UpdateReturn executeUpdateIntervalST(ExecutionPlan executionPlan,
			Entry<String, List<RealSqlContext>> dbEntry) {
		String dbSelectorId = dbEntry.getKey();

		long start = 0;
		RealSqlContext targetSqloutput = null;

		int affectedRows = 0;
		List<SQLException> exceptions = new LinkedList<SQLException>();
		try {
			/**
			 * 第一次查询之前，检查下当前线程有没有被置为 interrupted()
			 */
			checkThreadState();

			Connection conn = connectionManager.getConnection(dbSelectorId,
					executionPlan.isGoSlave());

			Statement stmt = createStatementInternal(conn);
			try {
				stmt.setQueryTimeout(queryTimeout);
				stmt.setFetchSize(fetchSize);
				stmt.setMaxRows(maxRows);
				
				// 执行sql
				for (RealSqlContext targetSql : dbEntry.getValue()) {
					/**
					 * 第一次查询之前，检查下当前线程有没有被置为 interrupted()
					 */
					checkThreadState();

					start = System.currentTimeMillis();
					targetSqloutput = targetSql;
					affectedRows = executeUpdateAtRealConnection(affectedRows,
							targetSql, stmt);
					long during = System.currentTimeMillis() - start;
					profileRealDatabaseAndTables(dbSelectorId, targetSql,
							during);
				}
			} catch (SQLException e) {
				long during = System.currentTimeMillis() - start;
				profileRealDatabaseAndTablesWithException(dbSelectorId,
						targetSqloutput, during);
				throw e;
			} finally {
				stmt.close();
			}
		} catch (SQLException e) {
			exceptions = appendToExceptionList(exceptions, e);
		} finally {
			// 尝试关闭已经使用后的连接
			exceptions = tryCloseConnection(exceptions, dbSelectorId);
		}

		UpdateReturn ur = new UpdateReturn();
		ur.setAffectedRows(affectedRows);
		ur.setExceptions(exceptions);

		return ur;
	}

	/**
	 * 执行PreparedStatement的更新
	 * 
	 * @param executionPlan
	 * @param dbEntry
	 * @return
	 */
	protected UpdateReturn executeUpdateIntervalPST(
			ExecutionPlan executionPlan,
			Entry<String, List<RealSqlContext>> dbEntry) {
		int affectedRows = 0;
		List<SQLException> exceptions = new LinkedList<SQLException>();
		String dbSelectorId = dbEntry.getKey();

		try {
			/**
			 * 第一次查询之前，检查下当前线程有没有被置为 interrupted()
			 */
			checkThreadState();

			Connection conn = connectionManager.getConnection(dbSelectorId,
					executionPlan.isGoSlave());

			List<RealSqlContext> realSqlContexts = dbEntry.getValue();
			for (RealSqlContext sqlContext : realSqlContexts) {
				/**
				 * 第一次查询之前，检查下当前线程有没有被置为 interrupted()
				 */
				checkThreadState();

				if(logger.isDebugEnabled()){
			        logger.debug("real execute sql:"+sqlContext.getSql());
			        logger.debug("sql args:"+sqlContext.getArgument());
				}
				
				// 针对每一个表
				long oneTableStart = System.currentTimeMillis();
				PreparedStatement ps = prepareStatementInternal(conn,
						sqlContext.getSql());
				try {
					ps.setQueryTimeout(queryTimeout);
					ps.setFetchSize(fetchSize);
					ps.setMaxRows(maxRows);
					
					// 这里做了略微的修改，将参数由后端传入，保证参数是能够被修改的
					setParameters(ps, sqlContext.getArgument());
					affectedRows += ps.executeUpdate();
					long during = System.currentTimeMillis() - oneTableStart;
					profileRealDatabaseAndTables(dbSelectorId, sqlContext,
							during);
				} catch (SQLException e) {
					long during = System.currentTimeMillis() - oneTableStart;
					profileRealDatabaseAndTablesWithException(dbSelectorId,
							sqlContext, during);
					throw e;
				} finally {
					ps.close();
				}
			}
		} catch (SQLException e) {
//			SQL异常，这里不打日志，使用于允许大量插入失败的应用，类似主键冲突，减少日志打印量	jiechen.qzm 
//			logger.error("Update Error!", e);
			exceptions = appendToExceptionList(exceptions, e);
		} finally {
			exceptions = tryCloseConnection(exceptions, dbSelectorId);
		}
		
		UpdateReturn ur = new UpdateReturn();
		ur.setAffectedRows(affectedRows);
		ur.setExceptions(exceptions);

		return ur;
	}

	/**
	 * 取得真正的Statement
	 * 
	 * @param connection
	 * @return
	 * @throws SQLException
	 */
	protected Statement createStatementInternal(Connection connection)
			throws SQLException {
		Statement stmt;
		if (resultSetType != -1 && resultSetConcurrency != -1
				&& resultSetHoldability != -1) {
			stmt = connection.createStatement(resultSetType,
					resultSetConcurrency, resultSetHoldability);
		} else if (resultSetType != -1 && resultSetConcurrency != -1) {
			stmt = connection.createStatement(resultSetType,
					resultSetConcurrency);
		} else {
			stmt = connection.createStatement();
		}
		return stmt;
	}

	/**
	 * 取得真正的PrepareStatement
	 * 
	 * @param connection
	 * @param targetSql
	 * @return
	 * @throws SQLException
	 */
	protected PreparedStatement prepareStatementInternal(Connection connection,
			String targetSql) throws SQLException {
		PreparedStatement ps;
		if (resultSetType != -1 && resultSetConcurrency != -1
				&& resultSetHoldability != -1) {
			ps = connection.prepareStatement(targetSql, resultSetType,
					resultSetConcurrency, resultSetHoldability);
		} else if (this.resultSetType != -1 && resultSetConcurrency != -1) {
			ps = connection.prepareStatement(targetSql, resultSetType,
					resultSetConcurrency);
		} else if (autoGeneratedKeys != -1) {
			ps = connection.prepareStatement(targetSql, autoGeneratedKeys);
		} else if (columnIndexes != null) {
			ps = connection.prepareStatement(targetSql, columnIndexes);
		} else if (columnNames != null) {
			ps = connection.prepareStatement(targetSql, columnNames);
		} else {
			ps = connection.prepareStatement(targetSql);
		}

		return ps;
	}

	/**
	 * 执行真正的更新
	 * 
	 * @param affectedRows
	 * @param targetSql
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	protected int executeUpdateAtRealConnection(int affectedRows,
			RealSqlContext targetSql, Statement stmt) throws SQLException {
		if (autoGeneratedKeys == -1 && columnIndexes == null
				&& columnNames == null) {
			affectedRows += stmt.executeUpdate(targetSql.getSql());
		} else if (autoGeneratedKeys != -1) {
			affectedRows += stmt.executeUpdate(targetSql.getSql(),
					autoGeneratedKeys);
		} else if (columnIndexes != null) {
			affectedRows += stmt.executeUpdate(targetSql.getSql(),
					columnIndexes);
		} else if (columnNames != null) {
			affectedRows += stmt.executeUpdate(targetSql.getSql(), columnNames);
		} else {
			affectedRows += stmt.executeUpdate(targetSql.getSql());
		}
		return affectedRows;
	}

	/**
	 * 设置几个查询和更新需要的参数
	 * 
	 * @param tStatementImp
	 * @param executionPlan
	 */
	protected void setSpecialProperty(TStatementImp tStatementImp,
			ExecutionPlan executionPlan) {
		try {
			this.resultSetConcurrency = tStatementImp.getResultSetConcurrency();
			this.resultSetHoldability = tStatementImp.getResultSetHoldability();
			this.resultSetType = tStatementImp.getResultSetType();
			this.queryTimeout = tStatementImp.getQueryTimeout();
			this.fetchSize=tStatementImp.getFetchSize();
			this.maxRows=tStatementImp.getMaxRows();
		} catch (SQLException e) {
			logger
					.error("set resultSetType,queryTimeOut,...property error!",
							e);
		}

		this.autoGeneratedKeys = executionPlan.getAutoGeneratedKeys();
		this.columnIndexes = executionPlan.getColumnIndexes();
		this.columnNames = executionPlan.getColumnNames();
	}
}
