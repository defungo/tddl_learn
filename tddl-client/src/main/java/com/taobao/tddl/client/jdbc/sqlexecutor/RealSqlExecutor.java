package com.taobao.tddl.client.jdbc.sqlexecutor;

import java.sql.SQLException;

/**
 * @author junyu
 * 
 */
public interface RealSqlExecutor {
	/**
	 * ִ�в�ѯ����Ҫִ�мƻ�ʵ��
	 * 
	 * @param executionPlan
	 * @return
	 * @throws SQLException 
	 */
	public QueryReturn query() throws SQLException;

	/**
	 * ִ�и��£���Ҫִ�мƻ�
	 * 
	 * @param executionPlan
	 * @return
	 */
	public UpdateReturn update()throws SQLException;
	
	/**
	 * ��Ҫ�ǻ���queryQueue��Rs��Statement
	 * 
	 * @throws SQLException
	 */
	public void clearQueryResource() throws SQLException;
}
