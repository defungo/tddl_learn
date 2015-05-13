package com.taobao.tddl.client.jdbc.resultset.newImp;

import static com.taobao.tddl.client.util.ExceptionUtils.appendToExceptionList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.taobao.tddl.client.jdbc.ConnectionManager;
import com.taobao.tddl.client.jdbc.RealSqlContext;
import com.taobao.tddl.client.jdbc.TPreparedStatementImp;
import com.taobao.tddl.client.jdbc.TStatementImp;
import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlan;
import com.taobao.tddl.client.util.ExceptionUtils;

/**
 * һ�������Ľ������װ�࣬�ṩǳ��װ��
 *
 * ���з���ֱ��ָ��resultSet�ķ�����ֻ�����ڲ�һ�ű�ĳ�����
 *
 * @author shenxun
 * @author junyu
 *
 */
public class ShallowTResultSetWrapper extends ProxyTResultSet implements ResultSet{
	private long startQueryTime = 0;

	private final boolean isPreparedStatement;
	/**
	 * ������statement
	 */
	private  Statement statement;
	/**
	 * ������resultSet
	 */
	private  ResultSet resultSet;
	private TStatementImp tStatementImp;
	private ExecutionPlan executionPlan;
	public ShallowTResultSetWrapper(TStatementImp tStatementImp,
			ConnectionManager connectionManager, ExecutionPlan executionPlan) throws SQLException {
		super(connectionManager);
		startQueryTime = System.currentTimeMillis();
		Map<String/* db Selector id */, List<RealSqlContext>/* �����ڵ�ǰdatabase��ִ�е�sql���б� */> sqlMap = executionPlan
				.getSqlMap();
		if (tStatementImp instanceof TPreparedStatementImp) {
			isPreparedStatement = true;
		} else {
			isPreparedStatement = false;
		}
		this.tStatementImp = tStatementImp;
		this.executionPlan = executionPlan;

		//bug 2011-10-20 junyu,not add before;
		// modified by jiechen.qzm 2012-02-23
		super.setResultSetProperty(tStatementImp);
		// modified end

		boolean firstElement = true;
		//check size
		for (Entry<String, List<RealSqlContext>> dbEntry : sqlMap.entrySet()) {
			String dbSelectorId = dbEntry.getKey();

				Connection connection = connectionManager
						.getConnection(dbSelectorId, executionPlan.isGoSlave());
				List<RealSqlContext> sqlList = dbEntry.getValue();
				for (RealSqlContext sql : sqlList) {
					long start = System.currentTimeMillis();

					if (!isPreparedStatement) {
						executeQueryIntervalST(connection, sql,tStatementImp);
					} else {
						executeQueryIntervalPST(connection, sql,tStatementImp);
					}
					// ����ܹ��ߵ������ʾ���ݿ����쳣��������ֹ����ѭ����

					long during = System.currentTimeMillis() - start;
					profileRealDatabaseAndTables(dbSelectorId, sql, during);
					if(firstElement){
						firstElement = false;
					}else{
						throw new SQLException("only one table execution was allowed on ShallowTRS! ");
					}
				}
		}
	}

	/*private void setResultSetProperty(TStatementImp tStatementImp) throws SQLException{
		setResultSetType(tStatementImp.getResultSetType());
		setResultSetConcurrency(tStatementImp.getResultSetConcurrency());
		setResultSetHoldability(tStatementImp.getResultSetHoldability());
		setFetchSize(tStatementImp.getFetchSize());
		setMaxRows(tStatementImp.getMaxRows());
		setQueryTimeout(tStatementImp.getQueryTimeout());
	}*/


	private void executeQueryIntervalST(Connection connection,
			RealSqlContext sql,TStatementImp statementImp) throws SQLException {
		// �����Ự
		statement = createStatementInternal(connection);
		statement.setQueryTimeout(getQueryTimeout());
		statement.setFetchSize(getFetchSize());
		statement.setMaxRows(getMaxRows());

		resultSet = statement.executeQuery(sql.getSql());
	    /**
	     * add by junyu
	     */
		super.currentResultSet=resultSet;

	}

	private void executeQueryIntervalPST(Connection connection,
			RealSqlContext sql,TStatementImp statementImp) throws SQLException {
		// �����Ự
		PreparedStatement stmt = prepareStatementInternal(connection, sql
				.getSql());
		stmt.setQueryTimeout(getQueryTimeout());
		stmt.setFetchSize(getFetchSize());
		stmt.setMaxRows(getMaxRows());

		setParameters(stmt, sql.getArgument());
		statement = stmt;
		resultSet = stmt.executeQuery();
		/**
		 * add by junyu
		 */
		super.currentResultSet=resultSet;

	}
	public void checkSize(Map<String/* db Selector id */, List<RealSqlContext>/* �����ڵ�ǰdatabase��ִ�е�sql���б� */> sqlMap)
	throws SQLException{
		if(sqlMap.size() != 1){
			throw new SQLException("should not be here , ONLY ONE ds allowed!");
		}
	}

	public void checkRSIsNull() throws SQLException{
		if(resultSet == null){
			throw new SQLException("exception on execution query,result set is already closed!");
		}
	}

	/**
	 * bug fix by shenxun : ԭ���ᷢ��һ������������TStatement������close()����
	 * ������������TResultSetû��closedʱ���ⲿ��ʹ��iterator������ÿһ��
	 * TResultSet�����ùرյķ���������ΪTResultSet��close������ص�
	 * TStatement�������ڴ���iterator��Set<ResultSet>���󣬲�ʹ��remove������
	 * ��ͻ��׳�һ��concurrentModificationException��
	 *
	 * @param removeThis
	 * @throws SQLException
	 */
	public void closeInternal() throws SQLException {
		checkRSIsNull();

		List<SQLException> exceptions = null;


		if (closed) {
			return;
		}
		// ͳ��������ѯ�ĺ�ʱ�������Ǻ�׼�����Ƚ���Ҫ��
		long elapsedTime = System.currentTimeMillis() - startQueryTime;

		profileDuringTime(exceptions, executionPlan.getVirtualTableName().toString(),
				executionPlan.getOriginalSql(), elapsedTime);

		try {
			// �ر�resultset
				try {
					resultSet.close();
				} catch (SQLException e) {
					exceptions = appendToExceptionList(exceptions, e);
				}

			// �ر�statement
				try {
					statement.close();
				} catch (SQLException e) {
					exceptions = appendToExceptionList(exceptions, e);
				}
		} finally {
			//������
			closed = true;
		}
		// ֪ͨ����ر���������
		for (String key : executionPlan.getSqlMap().keySet()) {
			exceptions = tryCloseConnection(exceptions, key);
		}
		//�׳��쳣�����exception ��Ϊnull
		ExceptionUtils.throwSQLException(exceptions,
				"sql exception during close resources", Collections.emptyList());
	}

	public Statement getStatement() throws SQLException {
		checkRSIsNull();
		//shenxun : ���ﷵ�ذ�װ��
		return tStatementImp;
	}
}
