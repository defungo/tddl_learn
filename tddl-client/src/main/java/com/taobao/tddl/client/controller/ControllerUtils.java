package com.taobao.tddl.client.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.taobao.tddl.client.dispatcher.DispatcherResult;
import com.taobao.tddl.client.dispatcher.EXECUTE_PLAN;
import com.taobao.tddl.interact.bean.ReverseOutput;
import com.taobao.tddl.interact.bean.TargetDB;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.sqlobjecttree.DMLCommon;
import com.taobao.tddl.sqlobjecttree.SqlAndTableAtParser;
import com.taobao.tddl.sqlobjecttree.SqlParserResult;
import com.taobao.tddl.sqlobjecttree.Update;
import com.taobao.tddl.sqlobjecttree.outputhandlerimpl.HandlerContainer;

public class ControllerUtils
{
	/**
	 * ��ӷֿ�������Ϣ�������Ψһ���ͷֿ����keyһ�£���ֿ���ڲ����ظ�����Ψһ���Ѿ��е�key=value�� 
	 * 
	 * @param mapList
	 * @param retMeta
	 */
	public static void appendDatabaseSharedMetaData(
			Map<String, Comparative> mapList, DispatcherResultImp retMeta)
	{
		if (mapList == null)
		{
			return;
		}
		String uniqueColumnKey = getUniqueKey(retMeta);
		for (Entry<String, Comparative> oneValue : mapList.entrySet())
		{
			String sharedKey = toLowerCaseIgnoreNull(oneValue.getKey());
			// ����ֿ����Ψһ���ظ�����ֿ���ڲ��ظ�����Ψһ��
			if (!sharedKey.equals(uniqueColumnKey))
			{
				// ��ǰ�˴����ȫ����Сд
				ColumnMetaData colMeta = new ColumnMetaData(sharedKey,
						oneValue.getValue());
				retMeta.addSplitDB(colMeta);
			}
		}
	}

	public static String toLowerCaseIgnoreNull(String tobeDone)
	{
		if (tobeDone != null)
		{
			return tobeDone.toLowerCase();
		}
		return null;
	}

	/**
	 * ���ﷵ��null���û��������Ϊ������TStatement���Ѿ����˶�pkΪnull���жϣ�Ϊ�˱�֤����һ����
	 * 
	 * @param retMeta
	 * @return
	 */
	protected static String getUniqueKey(DispatcherResultImp retMeta)
	{
		ColumnMetaData uniqueKey = retMeta.getPrimaryKey();
		if (uniqueKey == null)
		{
			return null;
		}
		return uniqueKey.key;
	}

	protected static Set<String> getDatabaseSharedingKeys(
			DispatcherResultImp retMeta)
	{
		List<ColumnMetaData> dbExpression = retMeta.getSplitDB();
		if (dbExpression == null || dbExpression.size() == 0)
		{
			return Collections.emptySet();
		}

		Set<String> dbkeys = new HashSet<String>(dbExpression.size());
		for (ColumnMetaData col : dbExpression)
		{
			dbkeys.add(col.key);
		}
		return dbkeys;
	}

	/**
	 * ����ִ�мƻ�
	 * 
	 * ���б��ִ�мƻ�������ж��������Ķ����ĸ�����ͬ����ô���ձ�����������Ǹ�ֵΪ׼��
	 * ������db1~5����ĸ����ֱ�Ϊ0,0,0,0,1:��ô���صı�ִ�мƻ�ΪSINGLE
	 * ������ĸ����ֱ�Ϊ0,1,2,3,4,5����ô���ر��ִ�мƻ�ΪMULTIPLE.
	 * 
	 * @param dispatcherResult
	 * @param targetDBList
	 */
	public static void buildExecutePlan(DispatcherResult dispatcherResult,
			List<DatabaseExecutionContext> databaseExecutionContexts)
	{
		if (databaseExecutionContexts == null)
		{
			throw new IllegalArgumentException("targetDBList is null");
		}
		int size = databaseExecutionContexts.size();
		switch (size)
		{
		case 0:
			dispatcherResult.setDatabaseExecutePlan(EXECUTE_PLAN.NONE);
			dispatcherResult.setTableExecutePlan(EXECUTE_PLAN.NONE);
			break;
		case 1:
			DatabaseExecutionContext targetDB = databaseExecutionContexts.get(0);
			List<Map<String, String>>  set = targetDB.getTableNames();
			dispatcherResult.setTableExecutePlan(buildTableExecutePlan(set,
					null));
			// �����Ϊnone����ô��ҲΪnone.�����Ϊnone����ô��Ϊsingle
			if (dispatcherResult.getTableExecutePlan() != EXECUTE_PLAN.NONE)
			{
				dispatcherResult.setDatabaseExecutePlan(EXECUTE_PLAN.SINGLE);
			} else
			{
				dispatcherResult.setDatabaseExecutePlan(EXECUTE_PLAN.NONE);
			}
			break;
		default:
			EXECUTE_PLAN currentExeutePlan = EXECUTE_PLAN.NONE;
			for (DatabaseExecutionContext oneDB : databaseExecutionContexts)
			{
				currentExeutePlan = buildTableExecutePlan(
						oneDB.getTableNames(), currentExeutePlan);
			}
			dispatcherResult.setTableExecutePlan(currentExeutePlan);
			if (dispatcherResult.getTableExecutePlan() != EXECUTE_PLAN.NONE)
			{
				dispatcherResult.setDatabaseExecutePlan(EXECUTE_PLAN.MULTIPLE);
			} else
			{
				dispatcherResult.setDatabaseExecutePlan(EXECUTE_PLAN.NONE);
			}
			break;
		}
	}

	private static EXECUTE_PLAN buildTableExecutePlan(List<Map<String, String>>  tableSet,
			EXECUTE_PLAN currentExecutePlan)
	{
		if (currentExecutePlan == null)
		{
			currentExecutePlan = EXECUTE_PLAN.NONE;
		}
		EXECUTE_PLAN tempExecutePlan = null;
		if (tableSet == null)
		{
			throw new IllegalStateException("targetTab is null");
		}
		int tableSize = tableSet.size();
		// ������Ϊ����
		switch (tableSize)
		{
		case 0:
			tempExecutePlan = EXECUTE_PLAN.NONE;
			break;
		case 1:
			tempExecutePlan = EXECUTE_PLAN.SINGLE;
			break;
		default:
			tempExecutePlan = EXECUTE_PLAN.MULTIPLE;
		}
		return tempExecutePlan.value() > currentExecutePlan.value() ? tempExecutePlan
				: currentExecutePlan;
	}

	/**
	 * ��ӷֱ�������Ϣ�������Ψһ���ͷֱ����keyһ�£���ֱ���ڲ����ظ�����Ψһ���Ѿ��е�key=value��
	 * ͬʱ������ֿ�������е�key��Ҳ��������ڷֱ����
	 * 
	 * @param mapList
	 * @param retMeta
	 *            TODO:test
	 */
	public static void appendTableSharedMetaData(
			Map<String, Comparative> mapList, DispatcherResultImp retMeta)
	{
		if (mapList == null)
		{
			return;
		}
		for (Entry<String, Comparative> oneValue : mapList.entrySet())
		{

			String uniqueColumnKey = getUniqueKey(retMeta);
			Set<String> dbSharedingKeys = getDatabaseSharedingKeys(retMeta);

			String sharedKey = toLowerCaseIgnoreNull(oneValue.getKey());
			// ����ֱ����Ψһ����ֿ���ظ�����ֱ���ڲ��ظ�����Ψһ���ͷֿ��
			if (!sharedKey.equals(uniqueColumnKey)
					&& !dbSharedingKeys.contains(sharedKey))
			{
				ColumnMetaData colMeta = new ColumnMetaData(sharedKey,
						oneValue.getValue());
				retMeta.addSplitTab(colMeta);
			}
		}
	}

	public static void appendUniqueKeysMetaData(
			Map<String, Comparative> mapList, DispatcherResultImp retMeta)
	{
		if (mapList == null)
		{
			return;
		}
		for (Entry<String, Comparative> oneValue : mapList.entrySet())
		{
			String key = toLowerCaseIgnoreNull(oneValue.getKey());
			ColumnMetaData colMeta = new ColumnMetaData(key,
					oneValue.getValue());
			retMeta.setUniqueKey(colMeta);
		}
	}

	/**
	 * �����߼����targetDBת��ΪdatabaseExecutionContext
	 * 
	 * @param targetDBs
	 * @return
	 */
	public static List<DatabaseExecutionContext> convertSingleTableTargetDBToDBExecutionContext(String logicTable ,List<TargetDB> targetDBs){
		List<DatabaseExecutionContext> databaseExecutionContexts = new ArrayList<DatabaseExecutionContext>(targetDBs.size());
		for(TargetDB targetDB : targetDBs)
		{
			DatabaseExecutionContextImp context = new DatabaseExecutionContextImp();
			context.setDbIndex(targetDB.getDbIndex());
			Set<String> tableNames = targetDB.getTableNames();

			for(String realTable : tableNames)
			{
				Map<String, String> tablePair = new HashMap<String, String>(1,1);
				tablePair.put(logicTable,realTable);
				context.addTablePair(tablePair);
			}
			databaseExecutionContexts.add(context);
		}
		return null;	
	}
	
	/**
	 * �������������ص�context���������Ŀǰ��Ҫ�ǽ����������
	 * 
	 * :1.���sql�д����˷��ϱ����滻pattern���ֶΣ����Ҳ��뱻�滻���� 2.���sql�а����˿���limit m,n�Ĳ�����
	 * 3.update+���ݸ��Ƶ�����£���Ϊ�ֿ��version�ֶ�Ĭ�ϵ��������null.
	 * ����where������Ҫ����ifnull����nvl����֤��ԭ��ΪNull�Ĳ�����ԭΪ0��
	 * ����������¶���Ҫ���з��������Ҳ��ͨ����������������sql.
	 * 
	 * ����������Ϊ�����������Ҳ�����������˲����з���
	 * 
	 * @param args
	 * @param dmlc
	 * @param max
	 * @param skip
	 * @param retMeta
	 * @param isMySQL
	 * @param needRowCopy
	 */
	public static void buildReverseOutput(List<Object> args,
			SqlParserResult dmlc, DispatcherResult retMeta,
			boolean isMySQL)
	{
		int max = retMeta.getMax();
		int skip = retMeta.getSkip();
		boolean needRowCopy = retMeta.needRowCopy();
		List<SqlAndTableAtParser> sqls = null;
		List<DatabaseExecutionContext> databaseExecutionContexts = retMeta.getDataBaseExecutionContexts();
		for (DatabaseExecutionContext databaseExecutionContext : databaseExecutionContexts)
		{

			// ���Ŀ�����ݿ�Ϊһ�����п����ǵ��ⵥ��򵥿���
			HandlerContainer handler = new HandlerContainer();
			// �ȴ����������
			if (needRowCopy && dmlc instanceof Update)
			{
				if (isMySQL)
				{
					handler.changeMySQLUpdateVersion();
				} else
				{
					handler.changeOracleUpdateVersion();
				}

				retMeta.needAllowReverseOutput(true);
			}
			// ���skip max ��Ϊ�գ������Ƕ���ѯ
			if (skip != DMLCommon.DEFAULT_SKIP_MAX
					&& max != DMLCommon.DEFAULT_SKIP_MAX)
			{
				EXECUTE_PLAN dbExecutionPlan = retMeta.getDatabaseExecutePlan();
				EXECUTE_PLAN tabExecutionPlan = retMeta.getTableExecutePlan();
				// �ж��Ƿ��Ǹ�����ѯ
				if (dbExecutionPlan.equals(EXECUTE_PLAN.MULTIPLE)
						||tabExecutionPlan.equals(EXECUTE_PLAN.MULTIPLE))
				{ // ����ѯ������skip max��Ϊ��
					handler.changeRange(0, max);
					retMeta.needAllowReverseOutput(true);
				}
			}
			// �������������ֻ��Ҫ�ж��Ƿ���Ҫ��������������Ҫ�����������index
			if (retMeta.allowReverseOutput())
			{
				handler.changeIndex();
				handler.changeTable();

				sqls = dmlc.getSqlReadyToRun(databaseExecutionContext.getTableNames(), args,
						handler);
				List<ReverseOutput> reverse = new ArrayList<ReverseOutput>(
						sqls.size());
				for (SqlAndTableAtParser sql : sqls)
				{
					ReverseOutput out = new ReverseOutput();
					out.setParams(sql.modifiedMap);
					out.setSql(sql.sql);
					out.setTable(sql.table.toString());
					reverse.add(out);
				}
				databaseExecutionContext.setOutputSQL(reverse);
			}
		}
	}
}
