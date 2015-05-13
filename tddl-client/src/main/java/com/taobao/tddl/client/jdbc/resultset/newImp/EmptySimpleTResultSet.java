package com.taobao.tddl.client.jdbc.resultset.newImp;

import java.sql.SQLException;

import com.taobao.tddl.client.jdbc.ConnectionManager;
import com.taobao.tddl.client.jdbc.TStatementImp;
import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlan;
import com.taobao.tddl.client.jdbc.sqlexecutor.RealSqlExecutor;

/**
 * ����rs.next��Զ����false�Ŀս������ ��Ҫ����һЩ��������
 * 
 * @author shenxun
 * @author junyu
 * 
 */
public class EmptySimpleTResultSet extends PlainAbstractTResultSet {

	public EmptySimpleTResultSet(TStatementImp tStatementImp,
			ConnectionManager connectionManager, ExecutionPlan executionPlan,RealSqlExecutor realSqlExecutor)
			throws SQLException {
		super(tStatementImp, connectionManager, executionPlan,realSqlExecutor);
	}
	
	@Override
	protected void init(ConnectionManager connectionManager,
			ExecutionPlan context) throws SQLException {
	}

	@Override
	public boolean next() throws SQLException {
		return false;
	}
	
	public void closeInternal() throws SQLException {
		closed = true;
	}
}
