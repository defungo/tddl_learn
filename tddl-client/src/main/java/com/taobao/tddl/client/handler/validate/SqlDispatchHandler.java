//Copyright(c) Taobao.com
package com.taobao.tddl.client.handler.validate;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.client.controller.ControllerUtils;
import com.taobao.tddl.client.controller.DatabaseExecutionContext;
import com.taobao.tddl.client.controller.DispatcherResultImp;
import com.taobao.tddl.client.controller.OrderByMessagesImp;
import com.taobao.tddl.client.databus.DataBus;
import com.taobao.tddl.client.databus.PipelineRuntimeInfo;
import com.taobao.tddl.client.dispatcher.DispatcherResult;
import com.taobao.tddl.client.dispatcher.EXECUTE_PLAN;
import com.taobao.tddl.client.dispatcher.MultiLogicTableNames;
import com.taobao.tddl.client.handler.AbstractHandler;
import com.taobao.tddl.interact.bean.MatcherResult;
import com.taobao.tddl.interact.rule.bean.DBType;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.sqlobjecttree.OrderByEle;
import com.taobao.tddl.sqlobjecttree.SqlParserResult;

/**
 * @description ��handler��Ҫ���ܾ����м�����У��,�������ƶ����͵������
 *              GroupBy,Having,������DISTINCT֧��(��Ϊ���ܺ��ڴ氲ȫԭ��ֻ��
 *              ��������completeDistinct���ԲŻ���ȫ��Distinct���֧��)
 * 
 *              ������Է������,��ô���������,������Ҫ���ݸ���ʱ����version
 *              ���߱��version, limit m,n��m��n�ı任(����뵥���б��ʵĲ�ͬ), 
 *              �����滻����Ϊ.
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 2.4.4
 * @since 1.6
 * @date 2010-09-05����01:33:32
 */
@SuppressWarnings("unchecked")
public class SqlDispatchHandler extends AbstractHandler {
	public static final String HANDLER_NAME = "SqlDispatchHandler";
	private static final Log log = LogFactory.getLog(SqlDispatchHandler.class);

	public void handleDown(DataBus dataBus) throws SQLException {
		FlowType flowType = getPipeLineRuntimeInfo(dataBus).getFlowType();
		if (FlowType.DEFAULT == flowType || FlowType.NOSQLPARSE == flowType
				|| FlowType.BATCH == flowType
				|| FlowType.BATCH_NOSQLPARSER == flowType
				|| FlowType.DBANDTAB_RC == flowType
				|| FlowType.DBANDTAB_SQL == flowType) {
			dispatch(dataBus);
		}
	}

	protected void dispatch(DataBus dataBus) throws SQLException {
		PipelineRuntimeInfo runtime = super.getPipeLineRuntimeInfo(dataBus);
		MatcherResult matcherResult = runtime.getMatcherResult();
		List<DatabaseExecutionContext> databaseExecutionContexts = runtime
				.getDataBaseExecutionContext();

		SqlParserResult sqlParserResult = runtime.getSqlParserResult();
		List<Object> sqlParameters = runtime.getStartInfo().getSqlParameters();
		DBType dbType = runtime.getStartInfo().getDbType();
		boolean allowReverseOutput = runtime.isAllowReverseOutput();
		boolean isSqlParsed = runtime.getIsSqlParsed();
		boolean needRowCopy = runtime.isNeedRowCopy();
		List<String> uniqueColumns = runtime.getUniqueColumns();
		boolean completeDistinct = runtime.isCompleteDistinct();

		DispatcherResult metaData = null;

		if (null != matcherResult) {
			metaData = getDispatcherResult(databaseExecutionContexts,
					matcherResult, sqlParserResult, sqlParameters, dbType,
					uniqueColumns, needRowCopy, allowReverseOutput,
					isSqlParsed, completeDistinct);
		} else {
			metaData = getDispatcherResult(databaseExecutionContexts, null,
					sqlParserResult, sqlParameters, dbType,
					Collections.EMPTY_LIST, false, allowReverseOutput,
					isSqlParsed, completeDistinct);
		}

		setResult(metaData, runtime);

		debugLog(log, new Object[] { "sql dispatch end." });
	}

	/**
	 * ����ƥ�������������ո�TStatement�Ľ����ƴװ,��ͬ��matcher���Թ���
	 * 
	 * @param matcherResult
	 * @return
	 */
	protected DispatcherResult getDispatcherResult(
			List<DatabaseExecutionContext> databaseExecutionContexts,
			MatcherResult matcherResult, SqlParserResult sqlParserResult,
			List<Object> args, DBType dbType, List<String> uniqueColumnSet,
			boolean isNeedRowCopy, boolean isAllowReverseOutput,
			boolean isSqlParser, boolean completeDistinct) {

		DispatcherResultImp dispatcherResult = getTargetDatabaseMetaDataBydatabaseGroups(
				databaseExecutionContexts, sqlParserResult, args,
				isNeedRowCopy, isAllowReverseOutput);

		// ��Ȼ�ж�sql����������߼�Ӧ�÷ŵ����������Ϊ����û��Ҫ�����˹����Ժ�ͷ���TargetDBList����ഫ��һ��
		// �������һ�ξͿ�����

		ControllerUtils.buildExecutePlan(dispatcherResult,
				databaseExecutionContexts);

		// Group,Distinct,Having�߶����߶������¶�����ͨ��
		validGroupByFunction(sqlParserResult, dispatcherResult);
		validHavingByFunction(sqlParserResult, dispatcherResult);

		// ��ȫ��distinct֧�ֵĻ��Ͳ����������ߵ���������
		if (!completeDistinct) {
			validDistinctByFunction(sqlParserResult, dispatcherResult);
		}

		if (isSqlParser) {
			// ����sql parse���п������������
			ControllerUtils.buildReverseOutput(args, sqlParserResult,
					dispatcherResult, DBType.MYSQL.equals(dbType));
		}

		if (dispatcherResult.needRowCopy()) {

			// @Important ����ע��һ�����ܵ㵽���򣬷����������ݸ��Ƶ�sql���ظ�������ͬ������е�����
			buildUniqueKeyToBeReturn(sqlParserResult, args, uniqueColumnSet,
					dispatcherResult);

			if (matcherResult != null) {

				ControllerUtils.appendDatabaseSharedMetaData(
						matcherResult.getDatabaseComparativeMap(),
						dispatcherResult);
				ControllerUtils.appendTableSharedMetaData(
						matcherResult.getTableComparativeMap(),
						dispatcherResult);
				// @Important end
			}
		}

		return dispatcherResult;
	}

	protected DispatcherResultImp getTargetDatabaseMetaDataBydatabaseGroups(
			List<DatabaseExecutionContext> targetDatabases,
			SqlParserResult sqlParserResult, List<Object> arguments,
			boolean isNeedRowCopy, boolean isAllowReverseOutput) {
		MultiLogicTableNames logicTablename = new MultiLogicTableNames();
		logicTablename.setLogicTables(sqlParserResult.getTableName());
		// targetDatabase.set
		DispatcherResultImp dispatcherResultImp = new DispatcherResultImp(
				logicTablename, targetDatabases, isNeedRowCopy,
				isAllowReverseOutput, sqlParserResult.getSkip(arguments),
				sqlParserResult.getMax(arguments), new OrderByMessagesImp(
						sqlParserResult.getOrderByEles()),
				sqlParserResult.getGroupFuncType(),
				sqlParserResult.getDistinctColumn());
		return dispatcherResultImp;
	}

	/**
	 * �����groupby��������ִ�мƻ�Ϊ���ⵥ��򵥿��ޱ���޿��ޱ� ������ͨ��
	 * 
	 * @param sqlParserResult
	 * @param dispatcherResult
	 */
	protected void validGroupByFunction(SqlParserResult sqlParserResult,
			DispatcherResult dispatcherResult) {
		List<OrderByEle> groupByElement = sqlParserResult.getGroupByEles();
		if (groupByElement.size() != 0) {
			if (dispatcherResult.getDatabaseExecutePlan() == EXECUTE_PLAN.MULTIPLE) {
				throw new IllegalArgumentException("��������£�������ʹ��group by ����");
			}
			if (dispatcherResult.getTableExecutePlan() == EXECUTE_PLAN.MULTIPLE) {
				throw new IllegalArgumentException("��������£�������ʹ��group by����");
			}
		}
	}

	/**
	 * �߶����߶������ʹ��Distinct
	 * 
	 * @param sqlParserResult
	 * @param dispatcherResult
	 */
	protected void validDistinctByFunction(SqlParserResult sqlParserResult,
			DispatcherResult dispatcherResult) {
		List<String> dc = sqlParserResult.getDistinctColumn();
		if (dc != null && dc.size() != 0) {
			if (dispatcherResult.getDatabaseExecutePlan() == EXECUTE_PLAN.MULTIPLE) {
				throw new IllegalArgumentException("��������£�������ʹ��Distinct�ؼ���");
			}
			if (dispatcherResult.getTableExecutePlan() == EXECUTE_PLAN.MULTIPLE) {
				throw new IllegalArgumentException("��������£�������ʹ��Distinct�ؼ���");
			}
		}
	}

	/**
	 * �߶����߶������ʹ��Having
	 * 
	 * @param sqlParserResult
	 * @param dispatcherResult
	 */
	protected void validHavingByFunction(SqlParserResult sqlParserResult,
			DispatcherResult dispatcherResult) {
		boolean having = sqlParserResult.hasHavingCondition();
		if (having) {
			if (dispatcherResult.getDatabaseExecutePlan() == EXECUTE_PLAN.MULTIPLE) {
				throw new IllegalArgumentException("��������£�������ʹ��Having�ؼ���");
			}
			if (dispatcherResult.getTableExecutePlan() == EXECUTE_PLAN.MULTIPLE) {
				throw new IllegalArgumentException("��������£�������ʹ��Having�ؼ���");
			}
		}
	}

	protected void buildUniqueKeyToBeReturn(SqlParserResult sqlParserResult,
			List<Object> args, List<String> uniqueColumnSet,
			DispatcherResultImp dispatcherResult) {
		Set<String> tempSet = new HashSet<String>(1);
		for (String str : uniqueColumnSet) {
			tempSet.clear();
			tempSet.add(str);
			Map<String, Comparative> uniqueMap = sqlParserResult
					.getComparativeMapChoicer().getColumnsMap(args, tempSet);
			if (!uniqueMap.isEmpty()) {
				ControllerUtils.appendUniqueKeysMetaData(uniqueMap,
						dispatcherResult);
			}
		}
	}

	private void setResult(DispatcherResult metaData,
			PipelineRuntimeInfo runtime) {
		runtime.setMetaData(metaData);
	}
}
