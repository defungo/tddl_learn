//Copyright(c) Taobao.com
package com.taobao.tddl.client.handler.executionplan;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.common.lang.StringUtil;
import com.taobao.tddl.client.controller.DatabaseExecutionContext;
import com.taobao.tddl.client.databus.DataBus;
import com.taobao.tddl.client.databus.PipelineRuntimeInfo;
import com.taobao.tddl.client.dispatcher.DispatcherResult;
import com.taobao.tddl.client.dispatcher.SqlDispatcher;
import com.taobao.tddl.client.handler.AbstractHandler;
import com.taobao.tddl.client.jdbc.RealSqlContext;
import com.taobao.tddl.client.jdbc.RealSqlContextImp;
import com.taobao.tddl.client.jdbc.SqlExecuteEvent;
import com.taobao.tddl.client.jdbc.SqlExecuteEventUtil;
import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlan;
import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlanImp;
import com.taobao.tddl.common.jdbc.ParameterContext;
import com.taobao.tddl.common.jdbc.ParameterMethod;
import com.taobao.tddl.interact.bean.Field;
import com.taobao.tddl.interact.bean.ReverseOutput;
import com.taobao.tddl.interact.rule.bean.SqlType;
import com.taobao.tddl.sqlobjecttree.DMLCommon;
import com.taobao.tddl.sqlobjecttree.InExpressionObject;
import com.taobao.tddl.sqlobjecttree.SqlParserResult;

/**
 * @description �������յ�ִ�мƻ�,��Ҫ���Ĺ���������RealSqlContext Map(����Щ��ִ����Щsql),
 *              ����isGoSlave(��������ж���׼֮һ),Limit M,N����(skip,max),�ۺϺ���ʵ��, order
 *              byʵ��,���ݸ���������(setEvent())������.
 * 
 *              tips: 1.����RealSqlContext�������κͲ�����2����ʽ,
 *              2.�����Ƿ���������Ծ����Ƿ�ʹ��sql�����ķ������, ������������,��ô���б����滻
 *              3.����needIdInGroup���þ����Ƿ��id in��ʽ��sql���� �����Ż�,���id
 *              in�����϶��ɢid��sql�нϺõ����� ����.
 * 
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 2.4.3
 * @since 1.6
 * @date 2010-12-6����03:40:50
 */
public class ExecutionPlanHandler extends AbstractHandler {
	public static final String HANDLER_NAME = "ExecutionPlanHandler";
	private static final Log log = LogFactory
			.getLog(ExecutionPlanHandler.class);

	public void handleDown(DataBus dataBus) throws SQLException {
		if (FlowType.DEFAULT == getPipeLineRuntimeInfo(dataBus).getFlowType()
				|| FlowType.NOSQLPARSE == getPipeLineRuntimeInfo(dataBus)
						.getFlowType()) {
			makeUp(dataBus);
		}
	}

	/**
	 * ����ExecutionPlan����.
	 * 
	 * @param dataBus
	 * @throws SQLException
	 */
	private void makeUp(DataBus dataBus) throws SQLException {
		PipelineRuntimeInfo runtime = super.getPipeLineRuntimeInfo(dataBus);
		List<String> virtualJoinTableNames = runtime.getVirtualJoinTableNames();
		DispatcherResult metaData = runtime.getMetaData();
		boolean isAutoCommit = runtime.getStartInfo().isAutoCommit();
		Map<Integer, ParameterContext> parameterSettings = runtime
				.getStartInfo().getSqlParam();
		SqlDispatcher sqlDispatcher = runtime.getSqlDispatcher();
		String originalSql = runtime.getStartInfo().getSql();
		SqlType sqlType = runtime.getStartInfo().getSqlType();
		boolean needRowCopy = runtime.isNeedRowCopy();
		SqlParserResult spr = runtime.getSqlParserResult();
		boolean needIdInGroup = runtime.isNeedIdInGroup();

		/**
		 * �����SqlParserHandler�Л]���O��virtualJoinTableNames ��ô���������û�õġ�
		 */
		metaData.setVirtualJoinTableNames(virtualJoinTableNames);

		// Ŀ���ͱ�bean
		List<DatabaseExecutionContext> targets = metaData
				.getDataBaseExecutionContexts();

		ExecutionPlanImp executionPlan = new ExecutionPlanImp();

		// FIXME:�Ȳ�֧��mapping rule
		if (targets == null || targets.isEmpty()) {
			throw new SQLException("�Ҳ���Ŀ��⣬��������");
		} else {
			buildExecutionContext(originalSql, executionPlan, sqlType,
					metaData, targets, sqlDispatcher, parameterSettings,
					isAutoCommit, needRowCopy, spr, needIdInGroup);
		}

		executionPlan.setUseParallel(getUseParallelFromThreadLocal());

		setResult(executionPlan, runtime);

		debugLog(log, new Object[] { "sql dispatch end." });
	}

	/**
	 * �������յ�ִ�мƻ�
	 * 
	 * @param originalSql
	 * @param executionPlanImp
	 * @param sqlType
	 * @param metaData
	 * @param targets
	 * @param sqlDispatcher
	 * @param parameterSettings
	 * @param isAutoCommit
	 * @param needRowCopy
	 * @throws SQLException
	 */
	private void buildExecutionContext(String originalSql,
			ExecutionPlanImp executionPlanImp, SqlType sqlType,
			DispatcherResult metaData, List<DatabaseExecutionContext> targets,
			SqlDispatcher sqlDispatcher,
			Map<Integer, ParameterContext> parameterSettings,
			boolean isAutoCommit, boolean needRowCopy, SqlParserResult spr,
			boolean needIdInGroup) throws SQLException {
		int size = targets.size();

		Map<String/* dbIndex */, List<RealSqlContext>> sqlMap = new HashMap<String, List<RealSqlContext>>(
				size);

		// ƴװ���صĽ��
		for (DatabaseExecutionContext target : targets) {
			// ���ݿ�dbSelectorId
			String dbSelectorId = target.getDbIndex();
			List<Map<String, String>> actualTables = target.getTableNames();

			printLog(dbSelectorId, actualTables);

			// valid
			if (dbSelectorId == null || dbSelectorId.length() < 1) {
				throw new SQLException("invalid dbSelectorId:" + dbSelectorId);
			}

			if (actualTables == null || actualTables.isEmpty()) {
				throw new SQLException("�Ҳ���Ŀ���");
			}

			List<RealSqlContext> sqlContext = null;
			
			// ���List��֤��Ϊnull,���in������°�ԭʼ�ķ�ʽ��(һ�㲻�����),
			// ����id in�����Ż�Ĭ�ϲ�ʹ��,��Ҫʹ�ÿ�����ShardRule bean������
			// needIdInGroup����
			if (needIdInGroup && spr.getInExpressionObjectList().size() == 1) {
				debugLog(log, new Object[] { "use id in group!columnName:",
						spr.getInExpressionObjectList().get(0).columnName });
				sqlContext = buildDBRealSqlContextWithInReplace(originalSql,
						metaData, parameterSettings, target, spr
								.getInExpressionObjectList().get(0));
			} else {
				sqlContext = buildDBRealSqlContext(originalSql, metaData,
						parameterSettings, target);
			}
			// put dbIndex ->sql
			sqlMap.put(dbSelectorId, sqlContext);
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
		executionPlanImp.setVirtualTableName(metaData.getVirtualTableName());
		executionPlanImp.setEvents(createEvent(metaData, sqlType, originalSql,
				needRowCopy));
		// ������Ҫע���
		// boolean needRetry = (sqlDispatcher == writeDispatcher?false:true);
		// modified by shenxun��ȥ���������жϣ�isAutoCommit����Ҫ�����жϣ��������Բ�ͬ��sql�����ʵ��ж�
		boolean goSlave = SqlType.SELECT.equals(sqlType);
		executionPlanImp.setGoSlave(goSlave);
		executionPlanImp.setOriginalSql(originalSql);
		executionPlanImp.setDistinctColumns(metaData.getDistinctColumns());
	}

	/**
	 * ��������
	 * 
	 * @param originalSql
	 * @param metaData
	 * @param parameterSettings
	 * @param target
	 * @param actualTables
	 * @param sqlContext
	 * @throws SQLException
	 */
	private List<RealSqlContext> buildDBRealSqlContext(String originalSql,
			DispatcherResult metaData,
			Map<Integer, ParameterContext> parameterSettings,
			DatabaseExecutionContext target) throws SQLException {
		List<RealSqlContext> sqlContext = new ArrayList<RealSqlContext>(target
				.getTableNames().size());
		List<Map<String/* logic table */, String/* actual table */>> actualTables = target
				.getTableNames();

		if (actualTables == null || actualTables.isEmpty()) {
			throw new SQLException("�Ҳ���Ŀ���");
		}

		// ѭ����������
		if (!metaData.allowReverseOutput()) {
			for (Map<String, String> tab : actualTables) {
				RealSqlContextImp realSqlContext = new RealSqlContextImp();
				sqlContext.add(realSqlContext);
				String sql = replcaeMultiTableName(originalSql, tab);
				
				// realSqlContext.setSql();
				// ���metaData(Ҳ����DispatcherResult)������join��������ô���滻��;
				// sql = replaceJoinTableName(metaData.getVirtualTableName()
				// .toString(), metaData.getVirtualJoinTableNames(), tab,
				// sql,log);
				realSqlContext.setRealTable(tab.values().toString());
				realSqlContext.setSql(sql);
				realSqlContext.setArgument(parameterSettings);
				// ��ӡ�����ջ�ִ�е�sql�Ͳ���,�����������
				debugLog(log, new Object[] {
						"use table replace,one of final to be executed sql is:", sql,
						";final parameter is:", parameterSettings });
			}
		} else {
			List<ReverseOutput> sqlInfos = target.getOutputSQL();
			if (sqlInfos == null || sqlInfos.isEmpty()) {
				throw new SQLException("�Ҳ���Ŀ���");
			}

			// �滻
			// TODO: ������Ҫ�ع����������滻�����Ĺ���˳��������Ŀǰû����
			Map<Integer, Object> changedParameters = sqlInfos.get(0)
					.getParams();
			changeParameters(changedParameters, parameterSettings);

			for (ReverseOutput sqlInfo : sqlInfos) {
				RealSqlContextImp realSqlContext = new RealSqlContextImp();
				sqlContext.add(realSqlContext);
				realSqlContext.setSql(sqlInfo.getSql());
				realSqlContext.setRealTable(sqlInfo.getTable());
				realSqlContext.setArgument(parameterSettings);
				debugLog(log, new Object[] {
						"use reverse output,one of final to be executed sql is:", sqlInfo.getSql(),
						";final parameter is:", parameterSettings });
			}

			// ��Ϊ����SQL�󶨲�����һ��������ֻҪȡ��һ����
		}
		return sqlContext;
	}

	/**
	 * ����id in���鹦�ܵ�RealSqlContext��������
	 * 
	 * @param originalSql
	 * @param metaData
	 * @param parameterSettings
	 * @param target
	 * @param in
	 * @return
	 * @throws SQLException
	 */
	private List<RealSqlContext> buildDBRealSqlContextWithInReplace(
			String originalSql, DispatcherResult metaData,
			Map<Integer, ParameterContext> parameterSettings,
			DatabaseExecutionContext target, InExpressionObject in)
			throws SQLException {
		List<RealSqlContext> sqlContext = new ArrayList<RealSqlContext>(target
				.getTableNames().size());
		List<Map<String/* logic table */, String/* actual table */>> actualTables = target
				.getTableNames();

		if (actualTables == null || actualTables.isEmpty()) {
			throw new SQLException("�Ҳ���Ŀ���");
		}

		// ѭ����������
		if (!metaData.allowReverseOutput()) {
			for (Map<String, String> tab : actualTables) {
				RealSqlContextImp realSqlContext = new RealSqlContextImp();
				sqlContext.add(realSqlContext);
				String sql = replcaeMultiTableName(originalSql, tab);
				Map<Integer, ParameterContext> replacedParameterSettings = parameterSettings;
				//ֻ��prepared statement��ʽ���д���
				if (in.bindVarIndexs != null) {
					sql = changePrepareStatementSql(sql,
							StringUtil.substringBetween(
									tab.values().toString(), "[", "]"),
							target.getRealTableFieldMap(), in);

					replacedParameterSettings = changeParameterContext(
							parameterSettings, StringUtil.substringBetween(tab
									.values().toString(), "[", "]"),
							target.getRealTableFieldMap(), in);
				}
				// statement��ʽ��������
				realSqlContext.setRealTable(tab.values().toString());
				realSqlContext.setSql(sql);
				realSqlContext.setArgument(replacedParameterSettings);
				// ��ӡ�����ջ�ִ�е�sql�Ͳ���,�����������
				debugLog(log, new Object[] {
						"use table replace,one of final to be executed sql is:", sql,
						";final parameter is:", replacedParameterSettings });
			}
		} else {
			List<ReverseOutput> sqlInfos = target.getOutputSQL();
			if (sqlInfos == null || sqlInfos.isEmpty()) {
				throw new SQLException("�Ҳ���Ŀ���");
			}

			// �滻
			// TODO: ������Ҫ�ع����������滻�����Ĺ���˳��������Ŀǰû����
			Map<Integer, Object> changedParameters = sqlInfos.get(0)
					.getParams();
			changeParameters(changedParameters, parameterSettings);

			for (ReverseOutput sqlInfo : sqlInfos) {
				Map<Integer, ParameterContext> replacedParameterSettings = parameterSettings;
				String sql = sqlInfo.getSql();
				//ֻ��prepared statement��ʽ���д���
				if (in.bindVarIndexs != null) {
					sql = changePrepareStatementSql(sqlInfo.getSql(),
							getReverseOutPutRealTable(sqlInfo.getTable()),
							target.getRealTableFieldMap(), in);

					replacedParameterSettings = changeParameterContext(
							parameterSettings,
							getReverseOutPutRealTable(sqlInfo.getTable()),
							target.getRealTableFieldMap(), in);
				}
				// statement��ʽ��������
				RealSqlContextImp realSqlContext = new RealSqlContextImp();
				sqlContext.add(realSqlContext);
				realSqlContext.setSql(sql);
				realSqlContext.setRealTable(sqlInfo.getTable());
				realSqlContext.setArgument(replacedParameterSettings);
				// ��ӡ�����ջ�ִ�е�sql�Ͳ���,�����������
				debugLog(log, new Object[] {
						"use reverse output,one of final to be executed sql is:", sql,
						";final parameter is:", replacedParameterSettings });
			}
		}
		return sqlContext;
	}

	private String getReverseOutPutRealTable(String tableMapStr) {
		String str = StringUtil.substringBetween(tableMapStr, "{", "}");
		return StringUtil.split(str, "=")[1];
	}

	/**
	 * ֻ��prepareStatement��ʽ��sql�з������ַ�ʽ. ������ܻ�����������.
	 */
	private static String patternStr = "in\\s*\\((\\s*\\?\\s*)?(,\\s*\\?\\s*)*\\)\\s*";
	private static Pattern inpattern = Pattern.compile(patternStr);

	/**
	 * id in ��sql���,���ݲ�������
	 * 
	 * @param sql
	 * @param realTable
	 * @param filedMap
	 * @param in
	 * @return
	 * @throws SQLException
	 */
	private String changePrepareStatementSql(String sql, String realTable,
			Map<String, Field> filedMap, InExpressionObject in)
			throws SQLException {
		Field f = filedMap.get(realTable);
		if (null == f || f.equals(Field.EMPTY_FIELD)) {
			return sql;
		}
		Set<Object> sourceValues = f.sourceKeys.get(in.columnName.toUpperCase());
		if (null == sourceValues) {
			return sql;
		}

		String[] sqlPieces = inpattern.split(sql.toLowerCase());
		StringBuilder replacedSql = new StringBuilder();
		if (null != sqlPieces && sqlPieces.length == 1) {
			appendPrepareStatementSql(replacedSql, sqlPieces,
					sourceValues.size());
		} else if (null != sqlPieces && sqlPieces.length == 2) {
			appendPrepareStatementSql(replacedSql, sqlPieces,
					sourceValues.size());
			replacedSql.append(sqlPieces[1]);
		} else {
			// �����滻����,��ֻ�ܷ���ԭʼsql��.
			return sql;
		}

		return replacedSql.toString();
	}

	/**
	 * ƴ��preparestatement��ʽ��sql
	 * 
	 * @param sb
	 * @param sqlPieces
	 * @param key
	 * @param size
	 */
	private void appendPrepareStatementSql(StringBuilder sb,
			String[] sqlPieces, int size) {
		sb.append(sqlPieces[0]);
		sb.append(" in (");
		for (int i = 0; i < size; i++) {
			if (i == (size - 1)) {
				sb.append("?");
			} else {
				sb.append("?,");
			}
		}
		sb.append(") ");
	}

	/**
	 * id in�����,ȥ��ԭ������Ҫ��parameter,���ұ�����
	 * 
	 * @param parameterSettings
	 * @param realTable
	 * @param filedMap
	 * @param in
	 * @return
	 * @throws SQLException
	 */
	private Map<Integer, ParameterContext> changeParameterContext(
			Map<Integer, ParameterContext> parameterSettings, String realTable,
			Map<String, Field> filedMap, InExpressionObject in)
			throws SQLException {
		Field f = filedMap.get(realTable);
		if (null == f || f.equals(Field.EMPTY_FIELD)) {
			return parameterSettings;
		}
		Set<Object> sourceValues = f.sourceKeys.get(in.columnName.toUpperCase());
		if (null == sourceValues) {
			return parameterSettings;
		}
		List<Integer> bindVarIndexs = in.bindVarIndexs;
		Map<Integer, ParameterContext> re = new HashMap<Integer, ParameterContext>();
		SortedMap<Integer, ParameterContext> tempMap = new TreeMap<Integer, ParameterContext>();

		/*
		 * ��parameterSettings�ҳ�sourceValues��صĲ������� select * from tab where
		 * gmt_create < ? and used_times=? and pk in (?,?,?,?) and name=?;����Ϊ
		 * "2010-10-10",100,1,2,3,4,"junyu",������ű��pkֵΪ 2,4,bindVarIndexsΪ:2,3,4,5
		 * ��ô��һ�����Ǵ�parameterSettings���ҳ�2,4����������һ��֮��,tempMap����<4,pc(2)><6,pc(4)>
		 */
		int count = 0;
		for (Integer var : bindVarIndexs) {
			ParameterContext pc = parameterSettings.get(var + 1);
			Object obj = pc.getArgs()[1];
			for (Object s : sourceValues) {
				if (s.equals(obj)) {
					tempMap.put(bindVarIndexs.get(count) + 1, pc);
					count++;
					break;
				}
			}
		}

		/*
		 * ��һ�����ǽ�parameterSettings�в����� id in�Ĳ����ŵ� tempMap��
		 * ��ΪtempMap�ǰ�key�����sortedMap,
		 * ������һ��֮��,tempMap����<1,pc("2010-10-10")><2,pc(
		 * 100)><4,pc(2)><6,pc(4)><7,pc("junyu")>
		 */
		for (Map.Entry<Integer, ParameterContext> pc : parameterSettings
				.entrySet()) {
			if (!bindVarIndexs.contains(pc.getKey() - 1)) {
				tempMap.put(pc.getKey(), pc.getValue());
			}
		}

		/*
		 * ��Ϊ���ǲ��ܶ�ԭʼ�� parameterSettings����Ĳ���(��������Ҫʹ��),�������ǽ���Ҫ�Ĳ������������ȸ���,����map
		 * key���б�ɼ��Ϊ1��,
		 * ��������<1,pc("2010-10-10")><2,pc(100)><3,pc(2)><4,pc(4)><5,pc("junyu")>
		 * ��ʱǰ���Ѿ��任��ϵ�sqlΪ select * from tab where gmt_create < ? and
		 * used_times=? and pk in (?,?) and name=?; �Ӷ����id in����
		 */
		int tempMapSize = tempMap.size();
		for (int i = 0; i < tempMapSize; i++) {
			Integer ind = tempMap.firstKey();
			ParameterContext pc = tempMap.get(ind);
			ParameterContext ele = new ParameterContext();
			ele.setParameterMethod(pc.getParameterMethod());
			ele.setArgs(new Object[2]);
			ele.getArgs()[0] = i + 1;
			ele.getArgs()[1] = pc.getArgs()[1];
			re.put(i + 1, ele);
			tempMap.remove(ind);
		}

		return re;
	}

	private void changeParameters(Map<Integer, Object> changedParameters,
			Map<Integer, ParameterContext> parameterSettings) {
		for (Map.Entry<Integer, Object> entry : changedParameters.entrySet()) {
			// ע�⣺SQL�����Ǳ߰󶨲�����0��ʼ�����������Ҫ��1��
			ParameterContext context = parameterSettings
					.get(entry.getKey() + 1);
			if (context.getParameterMethod() != ParameterMethod.setNull1
					&& context.getParameterMethod() != ParameterMethod.setNull2) {
				context.getArgs()[1] = entry.getValue();
			}
		}
	}

	/**
	 * update in�����⣺
	 */
	protected final List<SqlExecuteEvent> createEvent(
			DispatcherResult metaData, SqlType sqlType, String originalSql,
			boolean needRowCopy) throws SQLException {
		if ((sqlType == SqlType.INSERT || sqlType == SqlType.UPDATE)
				&& needRowCopy && metaData.needRowCopy()) {
			return SqlExecuteEventUtil.createEvent(metaData, sqlType,
					originalSql);
		} else {
			return null;
		}
	}

	/**
	 * ��ӡlog
	 * 
	 * @param dbIndex
	 * @param actualTables
	 */
	private void printLog(String dbIndex, List<Map<String, String>> actualTables) {
		if (log.isDebugEnabled()) {
			log.debug("pool: " + dbIndex);

			StringBuilder buffer = new StringBuilder("actualTables: [");
			boolean firstElement = true;
			for (Map<String, String> tab : actualTables) {
				if (!firstElement) {
					buffer.append(", ");
				} else {
					firstElement = false;
				}

				buffer.append(tab);
			}
			buffer.append("]");

			log.debug(buffer.toString());
		}
	}

	private void setResult(ExecutionPlan executionPlan,
			PipelineRuntimeInfo runtime) {
		runtime.setExecutionPlan(executionPlan);
	}
	
	public static void main(String[] args){
		ExecutionPlanHandler ep=new ExecutionPlanHandler();
		String sql = "select /*+ INDEX(T, IDX_BILL_BILLING_STATUS)*/ ID, USER_ID, NICK,"
			+ "SOURCE, TRADE_NO, RATING_BILL_ID, EVENT_ID, CHARGE_ITEM_ID,"
			+ "BOOK_ITEM_ID, REL_RECEIVE_PAY, SUB_PROD_ID, FROM_DATE, END_DATE,"
			+ " BILL_CYCLE, FEE_TYPE,"
			+ "PAY_TIME, NOTIFY_TIME, PAYMENT_STATUS, SERV_ID, SERV_PROVIDE, TAOBAO_ALIPAY_ID,"
			+ "ALIPAY_ID, ALIPAY_EMAIL, IS_NEED, IS_FINISHED, GMT_CREATE, GMT_MODIFIED,"
			+ "ORDER_END_TIME,SERV_CODE,PROD_ID,PTRADE_ID,TRADE_ID,ACCUSE_ID,BILL_TIME,BILL_TYPE,ALI_PAY_TIME,RATE_RECEIVE_PAY,ALIPAY_COMMITE_FEE,"
			+ " ACCOUNT_BOOK_ID,ACCOUNT_BOOK_DETAIL_ID,STATUS,HASH_CODE,GROUP_ID,VERSION,SP_ID,SP_TYPE,AFTER_TAX,TAX,WRITEOFF_TIME,SC_RECORD_ID,STATICS_STATUS,"
			+ "FIN_AMOUNT,BAL_TYPE,FIN_AFTER_TAX,FIN_TAX,BIZ_TYPE,ITEM_INST"
			+ "from BILL_BILLING T WHERE PAYMENT_STATUS in (0,2) and IS_FINISHED = 0 and STATUS != 3 and STATUS != 13"
			+ " and SOURCE = 2 and BIZ_TYPE = 3";
		
		System.out.println(ep.replaceTableName(sql, "BILL_BILLING","bill_billing_05", log));
	}
}
