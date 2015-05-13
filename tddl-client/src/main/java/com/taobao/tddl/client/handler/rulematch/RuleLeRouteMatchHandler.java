//Copyright(c) Taobao.com
package com.taobao.tddl.client.handler.rulematch;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.common.lang.StringUtil;
import com.taobao.tddl.client.controller.DatabaseExecutionContext;
import com.taobao.tddl.client.controller.DatabaseExecutionContextImp;
import com.taobao.tddl.client.databus.DataBus;
import com.taobao.tddl.client.databus.PipelineRuntimeInfo;
import com.taobao.tddl.client.handler.AbstractHandler;
import com.taobao.tddl.interact.bean.ComparativeMapChoicer;
import com.taobao.tddl.interact.bean.Field;
import com.taobao.tddl.interact.bean.MatcherResult;
import com.taobao.tddl.interact.bean.TargetDB;
import com.taobao.tddl.interact.rule.VirtualTable;
import com.taobao.tddl.interact.rule.VirtualTableRoot;
import com.taobao.tddl.interact.rule.bean.SqlType;
import com.taobao.tddl.rule.le.TddlRuleInner;
import com.taobao.tddl.rule.le.exception.ResultCompareDiffException;
import com.taobao.tddl.sqlobjecttree.SqlParserResult;

/**
 * @description todo
 * 
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 2.4.4
 * @since 1.6
 * @date 2010-11-15����02:23:42
 */
public class RuleLeRouteMatchHandler extends AbstractHandler {
	public static final String HANDLER_NAME = "RuleLeRouteMatchHandler";
	private final Log log = LogFactory.getLog(RuleLeRouteMatchHandler.class);
	private TddlRuleInner tddlRule=null;
   
	public RuleLeRouteMatchHandler(TddlRuleInner tddlRule){
		this.tddlRule=tddlRule;
	}
	
	/**
	 * RouteMatchHandler���NOSQLPARSE��DEFAULT���͵�ִ�н��д���
	 */
	public void handleDown(DataBus dataBus) throws SQLException {
		FlowType flowType = getPipeLineRuntimeInfo(dataBus).getFlowType();
		if (FlowType.DEFAULT == flowType || FlowType.NOSQLPARSE == flowType
				|| FlowType.BATCH == flowType
				|| FlowType.BATCH_NOSQLPARSER == flowType
				|| FlowType.DBANDTAB_RC == flowType
				|| FlowType.DBANDTAB_SQL == flowType) {
			try {
				match(dataBus);
			} catch (ResultCompareDiffException e) {
			    throw new SQLException(e);
			}
		}
	}

	/**
	 * ����·�����岿�֣���Ҫ�Ĳ��������ǽ�SQLPARSE�õ��ķֿ�ֱ��ֶ�����Ӧ�� ֵ��Ϊ������������м�������������table
	 * rule��������ľ�����Ҫ�ߵ� ������db rule�������������Ҫ�ߵĿ�(���׺)��
	 * 
	 * @param dataBus
	 * @throws ResultCompareDiffException 
	 */
	protected void match(DataBus dataBus) throws ResultCompareDiffException {
		PipelineRuntimeInfo runtime = super.getPipeLineRuntimeInfo(dataBus);

		SqlParserResult spr=runtime.getSqlParserResult();
		ComparativeMapChoicer choicer = spr.getComparativeMapChoicer();
		List<Object> sqlParameters = runtime.getStartInfo().getSqlParameters();
		SqlType sqlType=runtime.getStartInfo().getSqlType();
		Set<String> logicTableNames = runtime.getLogicTableNames();
		boolean isSqlParse = runtime.getIsSqlParsed();
		VirtualTableRoot root = runtime.getVirtualTableRoot();
		boolean needIdInGroup = root.isNeedIdInGroup();
		boolean completeDistinct = root.isCompleteDistinct();

		boolean isAllowReverseOutput = false;

		if (logicTableNames.size() == 1) {
			String logicTableName = logicTableNames.iterator().next();
			VirtualTable rule = root.getVirtualTable(StringUtil
					.toLowerCase(logicTableName));

			if (rule == null) {
				throw new IllegalArgumentException("δ���ҵ���Ӧ����,�߼���:"
						+ logicTableName);
			}

			// ֻ��sql�������������Ĳ����������
			if (isSqlParse) {
				isAllowReverseOutput = rule.isAllowReverseOutput();
			} else {
				isAllowReverseOutput = false;
			}
            
			MatcherResult matcherResult=null;
			if(spr.getInExpressionObjectList().size()>0&&needIdInGroup){
			    matcherResult = tddlRule.routeMVerAndCompare(sqlType, logicTableName,  choicer, sqlParameters, true);
			}else{
  			    matcherResult = tddlRule.routeMVerAndCompare(sqlType,logicTableName,  choicer, sqlParameters, false);
			}
			
			List<TargetDB> targetDBs = matcherResult.getCalculationResult();
			// ����һ��ת��Ĺ��̣����⿪��
			List<DatabaseExecutionContext> databaseExecutionContexts = convertToDatabaseExecutionContext(
					logicTableName, targetDBs);

			setResult(databaseExecutionContexts, matcherResult, null,
					isAllowReverseOutput, rule.isNeedRowCopy(),
					rule.getUniqueKeys(), needIdInGroup, completeDistinct,
					runtime);

		} else {
			/*
			 * ������һ�����ھ��崦����Զ��join�Ͷ��Զ��join�Ĵ���ѡ��֧�� ��ֱ��������ͬ�ı�Ĺ������ͨ������
			 * 
			 * @add by shenxun
			 */
			SqlParserResult sqlParserResult = runtime.getSqlParserResult();
			Map<String, String> alias = runtime.getAlias();

			int leftIndex = 0;
			int rightIndex = 1;

			List<List<TargetDB>> targetDBList = new ArrayList<List<TargetDB>>(
					logicTableNames.size());
			String[] logicTableArray = logicTableNames.toArray(new String[0]);
			List<DatabaseExecutionContext> databaseExecutionContexts = new LinkedList<DatabaseExecutionContext>();
			for (String logicTableName : logicTableArray) {
				VirtualTable rule = root.getVirtualTable(StringUtil
						.toLowerCase(logicTableName));

				if (rule == null) {
					if (log.isDebugEnabled()) {
						log.debug("can't find table by " + logicTableName
								+ " ,this logic table may dont need calc");
					}
				} else {
					if (isSqlParse && rule.isAllowReverseOutput()) {// ֻ����false
						// ��Ϊtrue,�������һ������Ҫ�����Ǿ��Ƕ���Ҫ���������
						isAllowReverseOutput = rule.isAllowReverseOutput();
					}
					
					MatcherResult matcherResult=tddlRule.routeMVerAndCompare(sqlType, logicTableName, sqlParserResult.getComparativeMapChoicer(), sqlParameters, true);
					targetDBList.add(matcherResult.getCalculationResult());
				}
			}

			// ������1����ôҪ���ǵѿ�������������
			if (targetDBList.size() > 2) {
				throw new IllegalArgumentException("��ʱ��֧���������߹����join");
			}
			List<TargetDB> left = targetDBList.get(leftIndex);
			List<TargetDB> right = targetDBList.get(rightIndex);
			/**
			 * size�������ȣ��϶��ǷǶԳ�join,ֱ�Ӷ���ȥ
			 */

			if (left.size() != right.size()) {
				throw new IllegalArgumentException("tddl Ŀǰֻ֧�ֶ��Ե�join");
			}

			if (left.size() == 1) {
				TargetDB leftTarget = left.get(0);
				TargetDB rightTarget = right.get(0);
				databaseExecutionContexts.add(buildOneDatabaseJoin(leftIndex,
						rightIndex, logicTableArray, leftTarget, rightTarget,
						alias));
			} else if (left.size() == 0) {
				throw new IllegalArgumentException("should not be here");
			} else {

				int count = 0;

				for (TargetDB leftTargetDB : left) {
					for (TargetDB rightTargetDB : right) {
						if (leftTargetDB.getDbIndex().equals(
								rightTargetDB.getDbIndex())) {
							databaseExecutionContexts.add(buildOneDatabaseJoin(
									leftIndex, rightIndex, logicTableArray,
									leftTargetDB, rightTargetDB, alias));
							count++;
						}
					}
				}
				if (count != left.size()) {
					throw new IllegalArgumentException("��ĸ�����ƥ��");
				}
			}

			setResult(databaseExecutionContexts, null, targetDBList,
					isAllowReverseOutput, false, null, needIdInGroup,
					completeDistinct, runtime);
		}
		debugLog(log, new Object[] { "rule match end." });
	}

	/**
	 * 
	 * @param logicTableName
	 * @param targetDBs
	 * @return
	 */
	private List<DatabaseExecutionContext> convertToDatabaseExecutionContext(
			String logicTableName, List<TargetDB> targetDBs) {
		List<DatabaseExecutionContext> databaseExecutionContexts = new ArrayList<DatabaseExecutionContext>(
				targetDBs.size());
		for (TargetDB targetDB : targetDBs) {
			DatabaseExecutionContextImp dbec = new DatabaseExecutionContextImp();
			Set<String> tableSet = targetDB.getTableNames();
			buildOneToOneJoin(targetDB.getDbIndex(), dbec, logicTableName,
					tableSet);
			dbec.setRealTableFieldMap(targetDB.getTableNameMap());
			databaseExecutionContexts.add(dbec);
		}
		return databaseExecutionContexts;
	}

	/**
	 * 
	 * @param dbec
	 * @param one
	 * @param manySet
	 */
	private void buildOneToManyJoin(String dbIndex,
			DatabaseExecutionContextImp dbec, String oneLogicTable, String one,
			String manyLogicTable, Set<String> manySet) {

		for (String many : manySet) {
			Map<String, String> tablePair = new HashMap<String, String>(1, 1);
			tablePair.put(manyLogicTable, many);
			tablePair.put(oneLogicTable, one);
			dbec.addTablePair(tablePair);
		}
		dbec.setDbIndex(dbIndex);
	}

	/**
	 * 
	 * @param dbec
	 * @param one
	 * @param manySet
	 */
	private void buildOneToOneJoin(String dbIndex,
			DatabaseExecutionContextImp dbec, String oneLogicTable,
			Set<String> targetTables) {
		for (String one : targetTables) {
			Map<String, String> tablePair = new HashMap<String, String>(1, 1);
			tablePair.put(oneLogicTable, one);
			dbec.addTablePair(tablePair);
		}
		dbec.setDbIndex(dbIndex);
	}

	/**
	 * 
	 * @param leftIndex
	 * @param rightIndex
	 * @param logicTableArray
	 * @param leftTarget
	 * @param rightTarget
	 * @param alias
	 * @return
	 */
	private DatabaseExecutionContext buildOneDatabaseJoin(int leftIndex,
			int rightIndex, String[] logicTableArray, TargetDB leftTarget,
			TargetDB rightTarget, Map<String, String> alias) {
		// ͨ�����㣬��������ֻ��һ����
		DatabaseExecutionContextImp dbec = new DatabaseExecutionContextImp();

		if (leftTarget.getDbIndex().equals(rightTarget.getDbIndex())) {// dbIndex��ͬ����ʾ��ͬһ����

			Map<String, Field> leftTableNameMap = leftTarget.getTableNameMap();
			Map<String, Field> rightTableNameMap = rightTarget
					.getTableNameMap();
			if (leftTableNameMap.size() == 1) {// ��ߵ���1���������ұ߲��ȣ�һ�Զ�ѿ�����
				String one = leftTableNameMap.keySet().iterator().next();
				String dbIndex = leftTarget.getDbIndex();
				String oneLogicTable = logicTableArray[0];
				String manyLogicTable = logicTableArray[1];
				Set<String> manySet = rightTableNameMap.keySet();
				buildOneToManyJoin(dbIndex, dbec, oneLogicTable, one,
						manyLogicTable, manySet);
			} else if (rightTableNameMap.size() == 1) {// �ұߵ���1����������߲��ȵ����
				String one = rightTableNameMap.keySet().iterator().next();
				String dbIndex = rightTarget.getDbIndex();
				String oneLogicTable = logicTableArray[1];
				String manyLogicTable = logicTableArray[0];
				Set<String> manySet = leftTableNameMap.keySet();
				buildOneToManyJoin(dbIndex, dbec, oneLogicTable, one,
						manyLogicTable, manySet);
			} else {// ��������1 ,��ô������Ҫ�ѿ�������Ҳ���ܲ���Ҫ
				// ��������Զ��join
				if (leftTableNameMap.size() == rightTableNameMap.size()) {// ��ȣ����ܿ���ֱ�Ӷ�Ӧ
					for (Entry<String/* table name */, Field> entry : leftTableNameMap
							.entrySet()) {

						String leftTableName = entry.getKey();
						int leftIndexNumberInt = getIndexNumberInt(leftTableName);
						// ��δ����������:�Ƚ�������+����
						// ��map,����ҵõ���ͬ�ı���Ƚϲ������������Ҳ��ȫ��ͬ
						// ��ʾ�����ұ��Ƕ�Ӧ�ġ�
						// ��ô��ʱ��Ϳ���ƴ��һ��join�滻�õ�map.�������������׳���ͬ���쳣
						dbec.setDbIndex(leftTarget.getDbIndex());
						for (Entry<String/* table name */, Field> rightEntry : rightTableNameMap
								.entrySet()) {
							// �����Ǹ�hack.
							// ����Ϊ��������logicTableName+_0000������ģʽ��ɵģ�����ֻ��Ҫ�ȽϺ������ֵ����
							String rightTableName = rightEntry.getKey();
							int rightIndexNumberInt = getIndexNumberInt(rightTableName);
							if (rightIndexNumberInt == leftIndexNumberInt) {// ��ʾ��ͬһ����
								Field rightField = rightTableNameMap.get(entry
										.getKey());
								if (rightField == null) {// ���ı������Ҳ�û�г��֣���ʾ����ͬ
									throw new IllegalArgumentException(
											"���join��������֧�֡�"
													+ "ֻ���ڶ��������ȫ��ͬ�ĳ����£���������ж��M*N join");
								}
								Field leftField = entry.getValue();
								if (!rightField.equals(leftField, alias)) {
									throw new IllegalArgumentException(
											"���ǹ�����ȫ��ͬ�����ݱ�֮����е�join����֧�֡�"
													+ "ֻ���ڶ��������ȫ��ͬ�ĳ����£���������ж��M*N join");
								} else {// ��ͬһ�����ڣ����Ҿ�����Ĳ���Ҳ��ͬ
									String leftLogicTable = logicTableArray[leftIndex];
									String rightLogicTable = logicTableArray[rightIndex];

									Map<String, String> tablePair = new HashMap<String, String>(
											1);
									tablePair
											.put(leftLogicTable, leftTableName);
									tablePair.put(rightLogicTable,
											rightTableName);
									dbec.addTablePair(tablePair);
								}
							}
						}
					}

				} else {
					// ������ ��ֱ���׳�
					throw new IllegalArgumentException(
							"�����������ȣ�tddl Ŀǰ��֧�ֶ�Զ�ѿ�����join");
				}
			}

		} else {// dbIndex��ͬ���ڲ�ͬ����
			throw new IllegalArgumentException("tddl �������ڶ����ִ��join��ѯ�����ݿ��޷�֧��");
		}
		return dbec;
	}

	/**
	 * 
	 * @param indexNumber
	 * @return
	 */
	private int getIndexNumberInt(String indexNumber) {
		int indexNumberInt;
		indexNumber = getIndexNumber(indexNumber);
		try {
			indexNumberInt = Integer.valueOf(indexNumber);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("ת����ʽ����:" + indexNumber);
		}
		return indexNumberInt;
	}

	/**
	 * 
	 * @param indexNumber
	 * @return
	 */
	private String getIndexNumber(String indexNumber) {
		int lastIndex = StringUtil.lastIndexOf(indexNumber, "_");
		indexNumber = StringUtil.substring(indexNumber, lastIndex + 1);
		return indexNumber;
	}

	/**
	 * ���ý������Ҫ��������һ��MatcherResult,�ṩ������Handlerʹ��
	 * 
	 * @param matcherResult
	 * @param runtime
	 */
	private void setResult(
			List<DatabaseExecutionContext> dataBaseExecutionContext,
			MatcherResult matcherResult, List<List<TargetDB>> targetDBList,
			boolean isAllowReverseOutput, boolean needRowCopy,
			List<String> uniqueColumns, boolean needIdInGroup,
			boolean completeDistinct, PipelineRuntimeInfo runtime) {
		runtime.setMatcherResult(matcherResult);
		runtime.setAllowReverseOutput(isAllowReverseOutput);
		runtime.setTargetDBList(targetDBList);
		runtime.setDataBaseExecutionContext(dataBaseExecutionContext);
		runtime.setNeedRowCopy(needRowCopy);
		runtime.setUniqueColumns(uniqueColumns);
		runtime.setNeedIdInGroup(needIdInGroup);
		runtime.setCompleteDistinct(completeDistinct);
	}
}
