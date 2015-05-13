//Copyright(c) Taobao.com
package com.taobao.tddl.client.pipeline.bootstrap;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.taobao.tddl.client.RouteCondition;
import com.taobao.tddl.client.databus.StartInfo;
import com.taobao.tddl.client.dispatcher.DispatcherResult;
import com.taobao.tddl.client.dispatcher.SqlDispatcher;
import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlan;
import com.taobao.tddl.common.jdbc.ParameterContext;

/**
 * @description ����������ת�������ӿڷ�������,�ṩ��TStatementImpʹ��,
 *              ������ִͨ�мƻ���������,batch������targetSql list����,
 *              �ṩ�����Եõ��ֿ�ֱ����ķ���.
 * 
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 2.4.3
 * @since 1.6
 * @date 2010-09-02����02:43:34
 */
public interface Bootstrap {
    /**
     * ����ִ��SQL�������������
     * @param startInfo  ����sql,parameters,sqlType��Ϣ
     * @return
     * @throws SQLException
     */
	ExecutionPlan bootstrap(StartInfo startInfo) throws SQLException;
	
    /**
     * ִ���޲�batch sql,ֻ��Ҫȡ��Ŀ��sql���ϼ���
     * @param startInfo
     * @param needRowCopy
     * @param targetSqls
     * @param selectKey
     * @throws SQLException
     */
	void bootstrapForBatch(StartInfo startInfo, boolean needRowCopy,
			Map<String, List<String>> targetSqls, String selectKey)
			throws SQLException;

    /**
     * ִ���в�����batch sql,ֻ��Ҫȡ��Ŀ��sql���ϼ���
     * @param startInfo
     * @param needRowCopy
     * @param targetSqls
     * @param selectKey
     * @throws SQLException
     */
	void bootstrapForPrepareBatch(StartInfo startInfo, boolean needRowCopy,
			Map<String, Map<String, List<List<ParameterContext>>>> targetSqls,
			String selectKey) throws SQLException;

	/**
	 * ������ԣ�����RouteConditionȡ��Ŀ�����ݿ�ͱ�(��ֻ����sql�����͹�����㣬
	 * ������ִ�мƻ�����)
	 * @param rc
	 * @param sqlDispatcher
	 * @return
	 * @throws SQLException
	 */
	DispatcherResult bootstrapForGetDBAndTabs(RouteCondition rc,
			SqlDispatcher sqlDispatcher) throws SQLException;

	/**
	 * ������ԣ����ݾ���sql��argsȡ��Ŀ�����ݿ�ͱ�(ֻ����sql�����͹�����㣬
	 * ������ִ�мƻ�����)
	 * @param sql
	 * @param args
	 * @param sqlDispatcher
	 * @return
	 * @throws SQLException
	 */
	DispatcherResult bootstrapForGetDBAndTabs(String sql, List<Object> args,
			SqlDispatcher sqlDispatcher) throws SQLException;
}
