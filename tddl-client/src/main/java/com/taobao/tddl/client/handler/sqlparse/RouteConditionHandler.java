//Copyright(c) Taobao.com
package com.taobao.tddl.client.handler.sqlparse;

import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.client.RouteCondition;
import com.taobao.tddl.client.databus.DataBus;
import com.taobao.tddl.client.databus.PipelineRuntimeInfo;
import com.taobao.tddl.client.handler.AbstractHandler;
import com.taobao.tddl.sqlobjecttree.SqlParserResult;
import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.JoinCondition;
import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.RuleRouteCondition;

/**
 * @description ���handler��Ҫ�Ǹ���RouteCondition����sql�������,
 *              ������SqlParserHandlerƽ��,�����䲻ͬ����,���handler
 *              ֻ��ģ������SqlParser����Ϊ,����������ķֿ�ֱ��ֶ�
 *              �Լ���������(�μ�SimpleCondition��������)���û��ֶ� ָ��,�����Ǹ���sql�����ó�.
 * 
 *              �û�ָ���ֿ�ֱ��ֶο���ʵ����һ��SimpleCondition,����
 *              �趨��Ӧ����,���ս�ʵ������ThreadLocalMap����(���ʾ��).
 * 
 *              �������Դﵽ��һ��Ч������sql�в���Ҫ����ֿ�ֱ��ֶ�,����
 *              ���зֿ�ֱ�,����ThreadLocalMap�����ױ�����(��Ϊÿ��ִ�к�
 *              �������,�����������ʽsqlǰִ��һ������ص�sql,������ʽ
 *              ִ��sqlʱ,SimpleCondition�Ѿ�������Ӷ�����),���Բ��Ƽ���ô ʹ��.
 * 
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 2.4.4
 * @since 1.6
 * @date 2010-09-08����03:33:32
 */
public class RouteConditionHandler extends AbstractHandler {
	public static final String HANDLER_NAME = "RouteConditionHandler";
	private final Log log = LogFactory.getLog(RouteConditionHandler.class);

	/**
	 * RouteConditionHandlerֻ���NOSQLPARSE���͵�ִ�н��д�������ȫ���Թ�
	 */
	public void handleDown(DataBus dataBus) throws SQLException {
		FlowType flowType = getPipeLineRuntimeInfo(dataBus).getFlowType();
		if (FlowType.NOSQLPARSE == flowType || FlowType.DBANDTAB_RC == flowType
			|| FlowType.BATCH_NOSQLPARSER == flowType) {
			parse(dataBus);
		}
	}

	/**
	 * ����SQL��ڣ�RouteCondition��Ҫ�ǵõ�һ��SqlParserResult�ṹ�Ա� ֮���Handlerʹ�á�
	 * 
	 * @param dataBus
	 */
	protected void parse(DataBus dataBus) {
		PipelineRuntimeInfo runtime = getPipeLineRuntimeInfo(dataBus);
		RouteCondition rc = runtime.getStartInfo().getRc();

		if (rc instanceof RuleRouteCondition) {
			SqlParserResult sqlParserResult = ((RuleRouteCondition) rc)
					.getSqlParserResult();

			setResult(sqlParserResult, false, runtime);

			if (rc instanceof JoinCondition) {
				runtime.setVirtualJoinTableNames(((JoinCondition) rc)
						.getVirtualJoinTableNames());
			}
		} else {
			throw new IllegalArgumentException("wrong RouteCondition type:"
					+ rc.getClass().getName());
		}

		debugLog(log, new Object[] { "route condition sql parse end." });
	}

	/**
	 * ���ý��
	 * 
	 * @param sqlParserResult
	 * @param logicTableName
	 * @param isRealSqlParsed
	 * @param isAllowReverseOutput
	 * @param runtime
	 */
	private void setResult(SqlParserResult sqlParserResult,
			boolean isRealSqlParsed, PipelineRuntimeInfo runtime) {
		runtime.setSqlParserResult(sqlParserResult);
		runtime.setLogicTableNames(sqlParserResult.getTableName());
		runtime.setIsSqlParsed(isRealSqlParsed);
	}
}
