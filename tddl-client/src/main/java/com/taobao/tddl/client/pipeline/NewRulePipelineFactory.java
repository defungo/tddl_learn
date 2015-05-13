//Copyright(c) Taobao.com
package com.taobao.tddl.client.pipeline;

import java.sql.SQLException;
import java.util.Map;

import com.taobao.tddl.client.ThreadLocalString;
import com.taobao.tddl.client.dispatcher.SqlDispatcher;
import com.taobao.tddl.client.handler.executionplan.BatchTargetSqlHandler;
import com.taobao.tddl.client.handler.executionplan.ExecutionPlanHandler;
import com.taobao.tddl.client.handler.executionplan.SqlDirectDispatchHandler;
import com.taobao.tddl.client.handler.rulematch.NewRuleRouteMatchHandler;
import com.taobao.tddl.client.handler.sqlparse.RouteConditionHandler;
import com.taobao.tddl.client.handler.sqlparse.SqlParseHandler;
import com.taobao.tddl.client.handler.validate.SqlDispatchHandler;
import com.taobao.tddl.client.util.ThreadLocalMap;
import com.taobao.tddl.common.SQLPreParser;
import com.taobao.tddl.interact.rule.VirtualTable;
import com.taobao.tddl.interact.rule.bean.DBType;
import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.DirectlyRouteCondition;

/**
 * @description 244�¹���֧��,����NewRuleRouteMatchHandler,��������
 *              ʵ��һ��,ʵ�����µĹ���,ͬʱ��дAbstractPipelineFactory��
 *              sqlԤ�����ͱ���dbType����(����ṹ�仯������Ҫ��д).
 *
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 2.4.3
 * @since 1.6
 * @date 2010-12-01����03:35:43
 */
public class NewRulePipelineFactory extends AbstractPipelineFactory {

	private Pipeline defaultPipeline = new DefaultPipeline();

	{
		/**
		 * ��ʼ���������ߣ����߶�Ϊ����
		 * ÿ�β�ѯ�����ݸ��¶��᷵��ͬһ������ʵ����
		 * Ψһ��һ��������Ӧ�����������ݣ�������
		 * �����ľֲ������ڹ����еĲ�ͬ������֮�䴫�ݡ�
		 * �Ӷ��������ϱ�����߳����⡣
		 */
		defaultPipeline.addLast(RouteConditionHandler.HANDLER_NAME,new RouteConditionHandler());
		defaultPipeline.addLast(SqlParseHandler.HANDLER_NAME,new SqlParseHandler());
		defaultPipeline.addLast(NewRuleRouteMatchHandler.HANDLER_NAME,new NewRuleRouteMatchHandler());
		defaultPipeline.addLast(SqlDirectDispatchHandler.HANDLER_NAME, new SqlDirectDispatchHandler());
		defaultPipeline.addLast(SqlDispatchHandler.HANDLER_NAME,new SqlDispatchHandler());
		defaultPipeline.addLast(ExecutionPlanHandler.HANDLER_NAME, new ExecutionPlanHandler());
	    defaultPipeline.addLast(BatchTargetSqlHandler.HANDLER_NAME, new BatchTargetSqlHandler());
	}

	public NewRulePipelineFactory() {}

	public NewRulePipelineFactory(Pipeline pipeline) {
		this.defaultPipeline=pipeline;
	}

	public Pipeline getPipeline() {
		return defaultPipeline;
	}

	public void setDefaultPipeline(Pipeline defaultPipeline) {
		this.defaultPipeline = defaultPipeline;
	}

	@Override
	public DirectlyRouteCondition sqlPreParse(String sql) throws SQLException {
		//����û�ָ����ROUTE_CONDITION����DB_SELECTOR����ô����Ԥ��������ֹ����
		if (null != ThreadLocalMap.get(ThreadLocalString.ROUTE_CONDITION)
				|| null != ThreadLocalMap.get(ThreadLocalString.DB_SELECTOR)
				|| null != ThreadLocalMap.get(ThreadLocalString.RULE_SELECTOR)) {
			return null;
		}

		String firstTable = SQLPreParser.findTableName(sql);
		if (null != firstTable) {
			Map<String, VirtualTable> vtabMap = this.defaultDispatcher
					.getVtabroot().getVirtualTableMap();

			if(null!=vtabMap.get(firstTable)){
				return null;
			}
		}

		logger.debug("no logic table in defaultDispather's logicTableMap,try to produce DirectlyRouteCondition");

		DirectlyRouteCondition condition = new DirectlyRouteCondition();
		//�������Ƿ���dbIndexMap�� add by jiechen.qzm
		Map<String, String> dbIndexMap = this.defaultDispatcher.getVtabroot().getDbIndexMap();
		if(dbIndexMap != null && dbIndexMap.get(firstTable) != null){
			condition.setDBId(dbIndexMap.get(firstTable));
			return condition;
		}

		String defaultDbIndex = this.defaultDispatcher.getVtabroot().getDefaultDbIndex();
		if(defaultDbIndex == null){
		    throw new SQLException("the defaultDispatcher have no dbIndexMap and defaultDbIndex");
		}
		//����������logicTable map �� dbIndexMap�У�ָ��ִ��dbIndex����
		condition.setDBId(defaultDbIndex);
		return condition;
	}

	@Override
	public DBType decideDBType(String sql,SqlDispatcher sqlDispatcher)throws SQLException{
		String firstTable = SQLPreParser.findTableName(sql);
		if (null != firstTable) {
			Map<String, VirtualTable> vtabMap = sqlDispatcher.getVtabroot().getVirtualTableMap();
			DBType findInLogicTab=null;
			if(null!=vtabMap.get(firstTable)){
				findInLogicTab=vtabMap.get(firstTable).getDbType();
			}
			return findInLogicTab;
		}

		return sqlDispatcher.getVtabroot().getDbTypeEnumObj();
	}
}
