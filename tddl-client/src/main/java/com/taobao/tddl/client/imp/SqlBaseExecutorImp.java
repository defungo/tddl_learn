package com.taobao.tddl.client.imp;

import java.util.List;

import org.springframework.orm.ibatis.SqlMapClientTemplate;

import com.taobao.tddl.client.RouteCondition;
import com.taobao.tddl.client.SqlBaseExecutor;
import com.taobao.tddl.client.ThreadLocalString;
import com.taobao.tddl.client.util.ThreadLocalMap;

public class SqlBaseExecutorImp implements SqlBaseExecutor {
//	private static final Logger log = Logger.getLogger(SqlBaseExecutorImp.class);
	int notifyTime=0;
	private SqlMapClientTemplate sqlMapClientTemplate;
//	private DBRuleProvider dbrouteProvider = null;

	public SqlMapClientTemplate getSqlMapClientTemplate() {
		return sqlMapClientTemplate;
	}

	public void setSqlMapClientTemplate(
			SqlMapClientTemplate sqlMapClientTemplate) {
		this.sqlMapClientTemplate = sqlMapClientTemplate;
	}

	public Object insert(String statementID, Object parameterObject,
			RouteCondition rc)  {
		ThreadLocalMap.put(ThreadLocalString.ROUTE_CONDITION, rc);
		return sqlMapClientTemplate.insert(statementID, parameterObject);
	}

	public List<Object> queryForList(String statementID,
			Object parameterObject, RouteCondition rc)
			 {
		return queryForList(statementID, parameterObject, false, rc);
	}

	@SuppressWarnings("unchecked")
	public List<Object> queryForList(String statementID,
			Object parameterObject, boolean isExistQuit, RouteCondition rc)
			 {
		ThreadLocalMap.put(ThreadLocalString.ROUTE_CONDITION, rc);
		ThreadLocalMap.put(ThreadLocalString.IS_EXIST_QUITE, isExistQuit);
		return sqlMapClientTemplate.queryForList(statementID, parameterObject);
	}

	public List<Object> queryForMergeSortList(String statementID,
			Object parameterObject, RouteCondition rc)
			 {
		return this.queryForMergeSortTables(statementID, parameterObject,
			 rc);
	}

	public List<Object> queryForMergeSortTables(String statementID,
			Object parameterObject, RouteCondition rc) {
		return queryForList(statementID, parameterObject, false, rc);
	}

//	/**
//	 * ��ȡ�����
//	 * 
//	 * @param statementID
//	 * @param parameterObject
//	 * @param poolMap
//	 * @param tempSortList
//	 * @param retObjs
//	 * @param param
//	 * @param skip
//	 * @param count
//	 * @param max
//	 * @param range
//	 * @throws MergeSortTableCountTooBigException
//	 */
//	@SuppressWarnings("unchecked")
//	private void getResults(String statementID, Object parameterObject,
//			Map<TableNameObj, Set<String[]>> poolMap,
//			List<TableNameObj> tempSortList, List<Object> retObjs, Map param,
//			long skip, long count, long max, long range,MergeSortTablesRetVal rev)
//			throws MergeSortTableCountTooBigException {
//		Set<String[]> tableVsPoolSet;
//		long time=0;
//		boolean needCalculation = false;
//		// ����б��е�ÿһ��
//		for (TableNameObj tab : tempSortList) {
//			tableVsPoolSet = poolMap.get(tab);
//
//			putTableMergeConditionToThreadLocalMap(rev.getVTabName(), tableVsPoolSet, tab);
//			param = addSkipMax2ParamMap(parameterObject, skip, max);
//			// ȡ���ݡ�
//			time=System.currentTimeMillis();
//			List tempList = sqlMapClientTemplate.queryForList(statementID,
//					param);
//			long tempTime=System.currentTimeMillis()-time;
//			if(tempTime>=notifyTime){
//				log.warn("run queryForList once,statementID is "+statementID+" " +
//						"param size :[" );
//			
//				log.warn(param.size()+"],elapsed time is "+tempTime);
//			}
//			log.info("run queryForList once,elapsed time is "+tempTime);
//			// ���δȡ��
//			if (tempList == null || tempList.size() == 0) {
//				needCalculation = true;
//				log.debug("can't at least one element,need calculation this db");
//			} else {
//				retObjs.addAll(tempList);
//				max = max - tempList.size();
//				needCalculation = false;
//			}
//			/*
//			 * ���һ�����Ӧ�˶�����⣬���ʾ����β�ѯ���漰�˶���ѯ����ʱ��֧��
//			 */
//			if (tableVsPoolSet.size() != 1) {
//				throw new TDLRunTimeException("Ŀǰ����֧�ֶ�����ѯ����ָ��Ψһ�����ݿ�");
//			} else {
//				// �����ύ���������
//	
//				if (retObjs.size() >= range) {
//					break;
//				}
//				if (needCalculation) {
//					putTableMergeConditionToThreadLocalMap(rev.getVTabName(), tableVsPoolSet, tab);
//					Object obj = sqlMapClientTemplate.queryForObject(
//							rev.getCountStatementId(), param);
//					if (obj instanceof Integer) {
//						count = ((Integer) obj).longValue();
//					} else if (obj instanceof Long) {
//						count = ((Long) obj).longValue();
//					} else if (obj instanceof BigDecimal) {
//						count = ((BigDecimal) obj).longValue();
//					} else {
//						throw new TDLRunTimeException(
//								"count��ѯ�������integer,bigDecimal��long,�޷������ۼ�");
//					}
//
//					/*
//					 * ������и����⣬��������������ܿ������£�
//					 * �п��ܳ�����Ȼȡ����ʱδδȡ���������´�countʱȴȡ������skipֵ��countֵ�����
//					 * �����������countֵ���ܻ�����max. ��������޷��жϡ� ��Ԥ�Ʋ��ᾭ��������
//					 */
//					if (skip >= count) {
//						skip = skip - count;
//						max = max - count;
//					} else {
//						// ����skipֵ������������maxֵ�Ļ���max=range
//						max =range;
//						skip=0;
//						
//					}
////					param = addSkipMax2ParamMap(parameterObject, skip, max);
//				}
//
//			}
//
//		}
//	}
//
//	private void putTableMergeConditionToThreadLocalMap(String vTabName,
//			Set<String[]> tableVsPoolSet, TableNameObj tab) {
//		ThreadLocalMap.put(ThreadLocalString.TABLE_MERGE_SORT_VIRTUAL_TABLE_NAME, vTabName);
//		ThreadLocalMap.put(
//				ThreadLocalString.TABLE_MERGE_SORT_TABLENAME,
//				tab.tabName);
//		ThreadLocalMap.put(ThreadLocalString.TABLE_MERGE_SORT_POOL,
//		tableVsPoolSet.toArray()[0]);
//	}
//
//	/**
//	 * �������
//	 * 
//	 * @param rev
//	 * @param poolMap
//	 * @return
//	 */
//	private List<TableNameObj> sortTables(MergeSortTablesRetVal rev,
//			Map<TableNameObj, Set<String[]>> poolMap) {
//		List<TableNameObj> tempSortList = new ArrayList<TableNameObj>(poolMap
//				.keySet());
//		if (rev.isAsc()) {
//			Collections.sort(tempSortList, new Comparator<TableNameObj>() {
//
//				public int compare(TableNameObj o1, TableNameObj o2) {
//
//					return o1.value - o2.value;
//				}
//
//			});
//		} else {
//			Collections.sort(tempSortList, new Comparator<TableNameObj>() {
//
//				public int compare(TableNameObj o1, TableNameObj o2) {
//
//					return o2.value - o1.value;
//				}
//
//			});
//		}
//		return tempSortList;
//	}
//
//	/**
//	 * ��������->���ص�key -value��ϵ
//	 * 
//	 * @param dbs
//	 * @param poolMap
//	 */
//	private void buildPoolMapBetweenTableNameVsReadPool(List<TargetDBs> dbs,
//			Map<TableNameObj, Set<String[]>> poolMap) {
//		long time=0;
//		if(log.isDebugEnabled()){
//			time=System.currentTimeMillis();
//		}
//		for (TargetDBs tdb : dbs) {
//			Set<TableNameObj> tabNames = tdb.getTableNames();
//			for (TableNameObj tabName : tabNames) {
//				if (poolMap.containsKey(tabName)) {
//					Set<String[]> temp = poolMap.get(tabName);
//					temp.add(tdb.getReadPool());
//					poolMap.put(tabName, temp);
//				} else {
//					Set<String[]> tempSet = new HashSet<String[]>();
//					// poolMap.put(str, tempSet);
//					tempSet.add(tdb.getReadPool());
//		
//					poolMap.put(tabName, tempSet);
//				}
//			}
//		}
//		if(log.isDebugEnabled()){
//			log.debug("buildPoolMapBetweenTableNameVsReadPoll,elapsed times is "+(System.currentTimeMillis()-time));
//		}
//	}

	/**
	 * ���skip��max�������ڣ�д����start��end,ͬʱǿ��Ҫ��parameterObject����ΪMap
	 * 
	 * @param parameterObject
	 * @param skip
	 * @param max
	 * @return
	 */
/*	private Map addSkipMax2ParamMap(Object parameterObject, long skip, long max) {
		Map param = null;
		if (parameterObject instanceof Map) {
			param = (Map) parameterObject;
			param.put("start", Long.valueOf(skip));
			param.put("end", Long.valueOf(max));
		} else {
			throw new RuntimeException("����MapΪ�������룬�����޷���̬���start��end���з�ҳ");
		}
		return param;
	}*/

	public Object queryForObject(String statementID, Object parameterObject,
			RouteCondition rc)  {
		return queryForObject(statementID, parameterObject, false, rc);
	}

	public Object queryForObject(String statementID, Object param,
			boolean isExistsQuit, RouteCondition rc)  {

		ThreadLocalMap.put(ThreadLocalString.ROUTE_CONDITION, rc);
		ThreadLocalMap.put(ThreadLocalString.IS_EXIST_QUITE, isExistsQuit);
		return sqlMapClientTemplate.queryForObject(statementID, param);
	}

	public int update(String statementID, Object parameterObject,
			RouteCondition rc)  {
		ThreadLocalMap.put(ThreadLocalString.ROUTE_CONDITION, rc);
		return sqlMapClientTemplate.update(statementID, parameterObject);
	}

	public Object insert(String statementID, Object parameterObject) {
		return this.insert(statementID, parameterObject, null);
	}

	public List<Object> queryForList(String statementID, Object parameterObject) {
		 return this.queryForList(statementID, parameterObject, null);
	}

	public List<Object> queryForList(String statementID,
			Object parameterObject, boolean isExistQuit) {
		return queryForList(statementID, parameterObject, isExistQuit, null);
	}

	public List<Object> queryForMergeSortList(String statementID,
			Object parameterObject) {
		return queryForMergeSortList(statementID, parameterObject, null);
	}

	public List<Object> queryForMergeSortTables(String statementID,
			Object parameterObject) {
		return queryForMergeSortTables(statementID, parameterObject, null);
	}

	public Object queryForObject(String statementID, Object parameterObject) {
		 return queryForObject(statementID, parameterObject, null);
	}

	public Object queryForObject(String statementID, Object param,
			boolean isExistsQuit) {
		return queryForObject(statementID, param, isExistsQuit, null);
	}

	public int update(String statementID, Object parameterObject) {
		return update(statementID, parameterObject,null);
	}

}
