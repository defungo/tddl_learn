//Copyright(c) Taobao.com
package com.taobao.tddl.client.pipeline.bootstrap;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.taobao.tddl.client.RouteCondition;
import com.taobao.tddl.client.RouteCondition.ROUTE_TYPE;
import com.taobao.tddl.client.ThreadLocalString;
import com.taobao.tddl.client.databus.DataBus;
import com.taobao.tddl.client.databus.PipelineContextDataBus;
import com.taobao.tddl.client.databus.PipelineRuntimeInfo;
import com.taobao.tddl.client.databus.StartInfo;
import com.taobao.tddl.client.dispatcher.DispatcherResult;
import com.taobao.tddl.client.dispatcher.SqlDispatcher;
import com.taobao.tddl.client.handler.AbstractHandler.FlowType;
import com.taobao.tddl.client.jdbc.ConnectionManager;
import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlan;
import com.taobao.tddl.client.pipeline.Pipeline;
import com.taobao.tddl.client.pipeline.PipelineFactory;
import com.taobao.tddl.client.util.ThreadLocalMap;
import com.taobao.tddl.common.jdbc.ParameterContext;
import com.taobao.tddl.common.jdbc.ParameterMethod;
import com.taobao.tddl.util.HintParser;
import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.DirectlyRouteCondition;
import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.SimpleCondition;

/**
 * @description ����������ת�������ӿ�ʵ����
 * 
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 2.4.3
 * @since 1.6
 * @date 2010-09-14����11:45:43
 */
public class PipelineBootstrap implements Bootstrap {
	protected PipelineFactory pipelineFactory;
	protected ConnectionManager connectionManager;

	public PipelineBootstrap(ConnectionManager connectionManager,
			PipelineFactory pipelineFactory) {
		this.pipelineFactory = pipelineFactory;
		this.connectionManager = connectionManager;
	}

	public ExecutionPlan bootstrap(StartInfo startInfo) throws SQLException {
		DataBus dataBus = null;
		//��ò�Ҫ�����
		RouteCondition rcFromHint = HintParser.convertHint2RouteCondition(startInfo);
		// ���hint��û��ָ���κ�ָʾ�Ե���Ϣ����ԭ���߼�
		// TDDL Hint��ThreadLocal��ָ����������TDDL Hint����
		if (rcFromHint == null) {
			DirectlyRouteCondition condition = pipelineFactory.sqlPreParse(startInfo.getSql());
			if (null == condition) {
				dataBus = this.bootstrap0(startInfo);
			} else {
				dataBus = this.bootstrap0Direct(startInfo, condition);
			}
		} else {
			//TDDL hint���ʹ�����Ƴ�����ֹsql�����ͺ�����������
			HintParser.removeTddlHintAndParameter(startInfo);
			//���hint����ָʾ����Ϣ������Ҫ����SQLԤ����
			dataBus = this.bootstrap0(startInfo,rcFromHint);
		}

		return getExecutionPlan(dataBus);
	}

	public void bootstrapForBatch(StartInfo startInfo,
			boolean needRowCopy, Map<String, List<String>> targetSqls,
			String selectKey) throws SQLException {
		this.bootstrap0ForBatch(startInfo,selectKey,false,
				targetSqls, null);
	}

	public void bootstrapForPrepareBatch(StartInfo startInfo, boolean needRowCopy,
			Map<String, Map<String, List<List<ParameterContext>>>> targetSqls,
			String selectKey) throws SQLException {
		this.bootstrap0ForBatch(startInfo,
				selectKey, true, null, targetSqls);
	}

	public DispatcherResult bootstrapForGetDBAndTabs(RouteCondition rc,
			SqlDispatcher sqlDispatcher) throws SQLException {
		DataBus dataBus = this.bootstrap0ForGetDBAndTabs(rc, null, null,
				sqlDispatcher);
		return getDispatcherResult(dataBus);
	}

	public DispatcherResult bootstrapForGetDBAndTabs(String sql,
			List<Object> args, SqlDispatcher sqlDispatcher) throws SQLException {
		DataBus dataBus = this.bootstrap0ForGetDBAndTabs(null, sql, args,
				sqlDispatcher);
		return getDispatcherResult(dataBus);
	}

	/**
	 * ��������ȡ��DispatcherResult
	 * 
	 * @param dataBus
	 * @return
	 */
	private DispatcherResult getDispatcherResult(DataBus dataBus) {
		PipelineRuntimeInfo runtime = (PipelineRuntimeInfo) dataBus
				.getPluginContext(PipelineRuntimeInfo.INFO_NAME);
		return runtime.getMetaData();
	}

	/**
	 * ��������ȡ��ExecutionPlan
	 * 
	 * @param dataBus
	 * @return
	 */
	private ExecutionPlan getExecutionPlan(DataBus dataBus) {
		PipelineRuntimeInfo runtime = (PipelineRuntimeInfo) dataBus
				.getPluginContext(PipelineRuntimeInfo.INFO_NAME);
		return runtime.getExecutionPlan();
	}

	/**
	 * �����Է���,�����޸Ĺ���Ĳ���
	 * 
	 * @param rc
	 * @param sql
	 * @param args
	 * @return
	 * @throws SQLException
	 */
	private DataBus bootstrap0ForGetDBAndTabs(RouteCondition rc, String sql,
			List<Object> args, SqlDispatcher sqlDispatcher) throws SQLException {
		StartInfo startInfo = new StartInfo();

		FlowType flowType = null;
		if (null != rc) {
			flowType = FlowType.DBANDTAB_RC;
			startInfo.setRc(rc);
		} else {
			flowType = FlowType.DBANDTAB_SQL;
		}

		DataBus dataBus = new PipelineContextDataBus();
		PipelineRuntimeInfo runtime = new PipelineRuntimeInfo();

		dataBus.registerPluginContext(PipelineRuntimeInfo.INFO_NAME, runtime);

		startInfo.setSql(sql);
		startInfo.setDbType(pipelineFactory.decideDBType(sql, sqlDispatcher));
		startInfo.setSqlArgs(args);
		runtime.setFlowType(flowType);
		runtime.setStartInfo(startInfo);
		runtime.setSqlDispatcher(sqlDispatcher);

		Pipeline pipeline = pipelineFactory.getPipeline();
		pipeline.startFlow(dataBus);
		return dataBus;
	}

	/**
	 * ר���ṩ������sqlʹ��
	 * 
	 * @param sql
	 * @param originalParameterSettings
	 * @param sqlType
	 * @param needRowCopy
	 * @param selectKey
	 * @param isPreparedBatch
	 * @param targetSqls
	 * @param prePareTargetSqls
	 * @return
	 * @throws SQLException
	 */
	private DataBus bootstrap0ForBatch(
			StartInfo startInfo,
			String selectKey,
			boolean isPreparedBatch,
			Map<String, List<String>> targetSqls,
			Map<String, Map<String, List<List<ParameterContext>>>> prePareTargetSqls)
			throws SQLException {
		// Batch֧��ThreadLocal,���ò���λ��.
		SimpleCondition rc = (SimpleCondition) getRouteContiongFromThreadLocal(ThreadLocalString.ROUTE_CONDITION);
		DirectlyRouteCondition condition = pipelineFactory
				.sqlPreParse(startInfo.getSql());
		FlowType flowType = null;
		if (rc != null) {
			List<Object> params = getSqlParameters(startInfo.getSqlParam());
			Map<String, Integer> paramIndexs = rc.getParametersIndexForBatch();

			for (Map.Entry<String, Integer> index : paramIndexs.entrySet()) {
				rc.put(index.getKey(),
						(Comparable<?>) params.get(index.getValue()));
			}
			flowType = FlowType.BATCH_NOSQLPARSER;
		} else if (condition != null) {
			flowType = FlowType.BATCH_DIRECT;
		} else {
			flowType = FlowType.BATCH;
		}

		DataBus dataBus = getPluginDataBus(startInfo,
				connectionManager.getAutoCommit(), flowType, selectKey, null,
				condition);

		PipelineRuntimeInfo runtime = (PipelineRuntimeInfo) dataBus
				.getPluginContext(PipelineRuntimeInfo.INFO_NAME);
		runtime.getStartInfo().setParameterBatch(isPreparedBatch);
		runtime.getStartInfo().setTargetSqls(prePareTargetSqls);
		runtime.getStartInfo().setRc(rc);
		runtime.getStartInfo().setTargetSqlsNoParameter(targetSqls);

		Pipeline pipeline = pipelineFactory.getPipeline();
		pipeline.startFlow(dataBus);
		return dataBus;
	}
	
	private List<Object> getSqlParameters(Map<Integer, ParameterContext> sqlParam) {
		if (sqlParam != null) {
			List<Object> parameters = new ArrayList<Object>();
			for (ParameterContext context : sqlParam.values()) {
				if (context.getParameterMethod() != ParameterMethod.setNull1
						&& context.getParameterMethod() != ParameterMethod.setNull2) {
					parameters.add(context.getArgs()[1]);
				} else {
					parameters.add(null);
				}
			}
			return parameters;
		} else {
			return Collections.emptyList();
		}
	}

	private DataBus bootstrap0(StartInfo startInfo) throws SQLException {
		return bootstrap0(startInfo, null);
	}

	/**
	 * ����ͨ��ѯ����ʹ��
	 * 
	 * @param sql
	 * @param originalParameterSettings
	 * @param sqlType
	 * @param needRowCopy
	 * @return
	 * @throws SQLException
	 */
	private DataBus bootstrap0(StartInfo startInfo, RouteCondition fromHintRc) throws SQLException {
		RouteCondition rc = null;
		DirectlyRouteCondition ruleCondition = null;
		DirectlyRouteCondition directlyRouteCondition = null;
		//���TDDL HINTΪ�գ���ô��ԭ����·
		if (null == fromHintRc) {
			rc = (RouteCondition) getRouteContiongFromThreadLocal(ThreadLocalString.ROUTE_CONDITION);
			ruleCondition = (DirectlyRouteCondition) getRouteContiongFromThreadLocal(ThreadLocalString.RULE_SELECTOR);
			directlyRouteCondition = (DirectlyRouteCondition) getRouteContiongFromThreadLocal(ThreadLocalString.DB_SELECTOR);
		} else {
			if (fromHintRc instanceof DirectlyRouteCondition) {
                //FIXME:����������ruleCondition����directlyRouteCondition,����Ĭ�ϴ���ΪdirectlyRouteCondition
				//Ҳ���ǲ�֧�ֶ��׹����ѡ��
				directlyRouteCondition=(DirectlyRouteCondition) fromHintRc;
			} else {
				rc = fromHintRc;
			}
		}
		
		FlowType flowType;
		String ruleId = null;
		if (directlyRouteCondition != null) {
			String dbRuleId = ((DirectlyRouteCondition) directlyRouteCondition)
					.getDbRuleID();
			if (connectionManager.containDBIndex(dbRuleId)) {
				flowType = FlowType.DIRECT; // ֱ��ִ��sql������������·��
			} else {
				throw new SQLException("�Ҳ���Ŀ��ִ�п�: " + dbRuleId);
			}
		} else if (ruleCondition != null) {
			ruleId = ((DirectlyRouteCondition) ruleCondition).getDbRuleID();
			if (rc != null) {
				flowType = FlowType.NOSQLPARSE; // ѡ����򣬲�������sql����
			} else {
				flowType = FlowType.DEFAULT; // ѡ����򣬲���Ĭ��ִ��
			}
		} else if (null != rc) {
			flowType = FlowType.NOSQLPARSE; // ��ѡ����򣬵�������sql����
		} else {
			flowType = FlowType.DEFAULT; // �Ȳ�ѡ�����Ҳ������sql������Ĭ��ִ��
		}

		DataBus dataBus = getPluginDataBus(startInfo,
				connectionManager.getAutoCommit(), flowType, ruleId,
				rc, directlyRouteCondition);
		Pipeline pipeline = pipelineFactory.getPipeline();

		pipeline.startFlow(dataBus);

		return dataBus;
	}

	/**
	 * ֱ��ִ��sql����Ҫ�ṩ��sqlԤ�������ֹ�����û�� ����ñ�Ĺ�����ôֱ����defaultDbIndex��ִ��SQL
	 * 
	 * @param sql
	 * @param originalParameterSettings
	 * @param sqlType
	 * @param condition
	 * @return
	 * @throws SQLException
	 */
	private DataBus bootstrap0Direct(StartInfo startInfo, DirectlyRouteCondition condition)
			throws SQLException {
		FlowType flowType = null;
		if (condition != null) {
			String dbRuleId = ((DirectlyRouteCondition) condition)
					.getDbRuleID();

			if (connectionManager.containDBIndex(dbRuleId)) {
				flowType = FlowType.DIRECT;
			} else {
				throw new SQLException("�Ҳ���Ŀ��ִ�п�: " + dbRuleId);
			}
		}

		DataBus dataBus = getPluginDataBus(startInfo,
				connectionManager.getAutoCommit(), flowType, null,
				null, condition);
		Pipeline pipeline = pipelineFactory.getPipeline();
		pipeline.startFlow(dataBus);

		return dataBus;
	}

	/**
	 * ��ȡһ���������ߣ�����������ע���Զ������ã���ע������ ÿ�γ�ʼ��һ���������ߣ�Ӧ�Զ��߳����⡣ add by junyu
	 * 
	 * @return
	 * @throws SQLException
	 */
	protected DataBus getPluginDataBus(StartInfo startInfo, boolean autoCommit,
		    FlowType flowType, String selectKey,
			RouteCondition rc, DirectlyRouteCondition directlyRouteCondition)
			throws SQLException {
		DataBus dataBus = new PipelineContextDataBus();
		PipelineRuntimeInfo runtime = new PipelineRuntimeInfo();

		dataBus.registerPluginContext(PipelineRuntimeInfo.INFO_NAME, runtime);
        if(startInfo==null){
		    startInfo = new StartInfo();
        }
		startInfo.setDbType(pipelineFactory.decideDBType(startInfo.getSql(),
				pipelineFactory.selectSqlDispatcher(selectKey)));
		startInfo.setAutoCommit(autoCommit);
		startInfo.setRc(rc);
		startInfo.setDirectlyRouteCondition(directlyRouteCondition);
		runtime.setStartInfo(startInfo);
		runtime.setFlowType(flowType);
		runtime.setSqlDispatcher(pipelineFactory.selectSqlDispatcher(selectKey));
		return dataBus;
	}

	/**
	 * ��threadLocal��ȡ��RouteCondition,���Ұ��������ThreadLocalMap�����ֵ
	 * 
	 * @param key
	 * @return
	 */
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
}
