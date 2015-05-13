//Copyright(c) Taobao.com
package com.taobao.tddl.client.handler.executionplan;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.client.databus.DataBus;
import com.taobao.tddl.client.databus.PipelineRuntimeInfo;
import com.taobao.tddl.client.dispatcher.SingleLogicTableName;
import com.taobao.tddl.client.handler.AbstractHandler;
import com.taobao.tddl.client.jdbc.RealSqlContext;
import com.taobao.tddl.client.jdbc.RealSqlContextImp;
import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlan;
import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlanImp;
import com.taobao.tddl.common.jdbc.ParameterContext;
import com.taobao.tddl.sqlobjecttree.DMLCommon;
import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.DirectlyRouteCondition;

/**
 * @description ��handler��Ҫ�����Ƕ���ֱ��ָ�����sql����ExecutionPlan,����
 *              ����ExecutionPlanHandlerƽ��.
 *              
 *              ���ǳ�����Ҫͨ��TDataSourceִ�зֿ�ֱ��sql�Ͳ����ֿ�ֱ��sql,
 *              ����2.4.x֮ǰ�����˵�ǱȽ����ѵ�����,��ô�����ں����汾���ṩ��
 *              ��ôһ�ֽ������,ֻҪ����������ĸ�����ִ��,��ôTDDL����������sql
 *              ���н����͹������,ֱ����Ŀ�����ִ�е�sql.�����handlerҲ���ǽ�
 *              ����ֱ��ִ�е������ķ�װ��ExecutionPlan�ṩ��֮���ִ������ִ��.
 *              
 *              ���ǿ���ʹ��RouteHelper.executeByDB(dbIndex)����TDDL����Ҫֱ��ִ��
 *              sql��Ŀ���,��Ȼ������Ҳ�����ڹ����ļ��е�ShardRule Bean������
 *              defaultDbIndex,��ô�㲻������ֱ��ִ�е�Ŀ���,����sql�еĵ�һ�ű�
 *              û�зֿ�ֱ�,���ǽ��������sql��defaultDbIndex��ִ�е�.
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 2.4.4
 * @since 1.6
 * @date 2010-09-08����06:33:32
 */
public class SqlDirectDispatchHandler extends AbstractHandler {
	public static final String HANDLER_NAME = "SqlDirectDispatchHandler";
	private final Log log = LogFactory.getLog(SqlDirectDispatchHandler.class);

	/**
	 * DirectDispatchHandler��Ҫʹ���ڲ��ֿ⣬Ҳ���ֱ��case
	 */
	public void handleDown(DataBus dataBus) throws SQLException {
		if (FlowType.DIRECT == getPipeLineRuntimeInfo(dataBus).getFlowType()) {
			dispatch(dataBus);
		}
	}

	/**
	 * ֱ�ӹ���ִ�мƻ����������ݽṹʽDirectlyRouteCondition
	 * 
	 * @param dataBus
	 * @throws SQLException
	 */
	protected void dispatch(DataBus dataBus) throws SQLException {
		PipelineRuntimeInfo runtime = super.getPipeLineRuntimeInfo(dataBus);
		DirectlyRouteCondition directlyRouteCondition = (DirectlyRouteCondition) runtime
				.getStartInfo().getDirectlyRouteCondition();
		String sql = runtime.getStartInfo().getSql();
		Map<Integer, ParameterContext> sqlParam = runtime.getStartInfo()
				.getSqlParam();

		/**
		 * ����ִ�мƻ�
		 */
		ExecutionPlan executionPlan = getDirectlyExecutionPlan(sql, sqlParam,
				directlyRouteCondition);

		/**
		 * ����ִ�мƻ�
		 */
		setResult(executionPlan, runtime);

		debugLog(log, new Object[] { "sql direct dispatch end." });
	}

	/**
	 * �õ�ֱ��ִ�е�ִ�мƻ�
	 * 
	 * @param sql
	 * @param parameterSettings
	 * @param metaData
	 * @return
	 * @throws SQLException
	 */
	private ExecutionPlan getDirectlyExecutionPlan(String sql,
			Map<Integer, ParameterContext> parameterSettings,
			DirectlyRouteCondition metaData) throws SQLException {
		ExecutionPlanImp executionPlanImp = new ExecutionPlanImp();
		Map<String/* db index key */, List<Map<String/* original table */, String/* targetTable */>>> shardTableMap = metaData
				.getShardTableMap();

		Map<String, List<RealSqlContext>> sqlMap = new HashMap<String, List<RealSqlContext>>(
				shardTableMap.size());
		for (Entry<String/* db index key */, List<Map<String/* original table */, String/* targetTable */>>> entry : shardTableMap
				.entrySet()) {
			List<Map<String, String>> tableMapList = entry.getValue();
			List<RealSqlContext> realSqlContexts = new ArrayList<RealSqlContext>(
					tableMapList.size());
			boolean isUsingRealConnection = false;
			if (tableMapList.isEmpty()) {
				// ���Ϊ�գ���ֱ��ʹ��ԭsql
				RealSqlContextImp realSqlContext = new RealSqlContextImp();
				realSqlContext.setArgument(parameterSettings);
				realSqlContext.setSql(sql);
				realSqlContexts.add(realSqlContext);
				isUsingRealConnection = true;
			} else {
				for (Map<String, String> targetMap/* logicTable->realTable */: entry
						.getValue()) {

					if (!isUsingRealConnection) {
						// ��һ�ν����ʱ���ʾʹ����ʵ����;
						isUsingRealConnection = true;
					} else {
						// isUsingRealConnection = true.��ΪĬ��Ϊfalse.����Ϊtrueһ������Ϊ
						// �ж���һ��sql��Ҫִ�С�
						isUsingRealConnection = false;
					}
					RealSqlContextImp realSqlContext = new RealSqlContextImp();
					realSqlContext.setArgument(parameterSettings);
					realSqlContext.setRealTable(targetMap.values().toString());
					// �滻����
					realSqlContext
							.setSql(replcaeMultiTableName(sql, targetMap));
					realSqlContexts.add(realSqlContext);
				}
			}
			sqlMap.put(entry.getKey()/* dbIndex */, realSqlContexts);
		}
		executionPlanImp.setSqlMap(sqlMap);
		executionPlanImp.setOriginalArgs(parameterSettings);
		executionPlanImp.setOrderByColumns(metaData.getOrderByMessages()
				.getOrderbyList());
		executionPlanImp
				.setSkip(metaData.getSkip() == DMLCommon.DEFAULT_SKIP_MAX ? 0
						: metaData.getSkip());
		executionPlanImp
				.setMax(metaData.getMax() == DMLCommon.DEFAULT_SKIP_MAX ? -1
						: metaData.getMax());
		executionPlanImp.setGroupFunctionType(metaData.getGroupFunctionType());
		executionPlanImp.setVirtualTableName(new SingleLogicTableName(metaData
				.getVirtualTableName()));
		executionPlanImp.setEvents(null);
		executionPlanImp.setOriginalSql(sql);
		return executionPlanImp;
	}

	private void setResult(ExecutionPlan executionPlan,
			PipelineRuntimeInfo runtime) {
		runtime.setExecutionPlan(executionPlan);
	}
}
