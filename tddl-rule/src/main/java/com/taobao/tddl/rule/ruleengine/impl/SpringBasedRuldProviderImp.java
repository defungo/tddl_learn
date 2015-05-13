//package com.taobao.tddl.rule.ruleengine.impl;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import com.taobao.tddl.common.exception.checked.TDLCheckedExcption;
//import com.taobao.tddl.common.sqljep.function.Comparative;
//import com.taobao.tddl.rule.DBType;
//import com.taobao.tddl.rule.LogicTableRule;
//import com.taobao.tddl.rule.bean.Database;
//import com.taobao.tddl.rule.bean.LogicTable;
//import com.taobao.tddl.rule.bean.RuleChain;
//import com.taobao.tddl.rule.bean.TDDLRoot;
//import com.taobao.tddl.rule.bean.Table;
//import com.taobao.tddl.rule.ruleengine.DBRuleProvider;
//import com.taobao.tddl.rule.ruleengine.cache.SharedValueElement;
//import com.taobao.tddl.rule.ruleengine.cache.VariableProbes;
//import com.taobao.tddl.rule.ruleengine.entities.abstractentities.SharedElement;
//import com.taobao.tddl.rule.ruleengine.entities.retvalue.PartitionElement;
//import com.taobao.tddl.rule.ruleengine.entities.retvalue.TargetDB;
//import com.taobao.tddl.rule.ruleengine.entities.retvalue.TargetDBMetaData;
//import com.taobao.tddl.rule.ruleengine.rule.CalculationContext;
//
///**
// * spring Ϊ���еĹ�������
// * 
// * @author shenxun
// * 
// */
//public class SpringBasedRuldProviderImp implements DBRuleProvider {
//	public static final int NO_INDEX = -1;
//	TDDLRoot root;
//
//	private CalculationContext buildCalculationContext(int databaseRuleIndex,
//			int tableRuleIndex,
//			Map<String, SharedValueElement> databaseSharedVariablesMap,
//			Map<String, SharedValueElement> tableSharedVariablesMap) {
//		CalculationContext calculationContext = new CalculationContext();
//		calculationContext.databaseRuleIndex = databaseRuleIndex;
//		calculationContext.databaseSharedValueElementsMap = databaseSharedVariablesMap;
//		calculationContext.tableRuleIndex = tableRuleIndex;
//		calculationContext.tableSharedValueElementsMap = tableSharedVariablesMap;
//		return calculationContext;
//	}
//
//	private TargetDBMetaData buildTargetDBMetaData(
//			Map<String, Comparative> colMap, LogicTable logicTable,
//			int databaseIndex, int tableIndex) {
//		Set<RuleChain> ruleChainSet = logicTable.getRuleChain();
//		
//		Map<String, SharedValueElement> databaseSharedVariablesMap = getSharedValueElementMap(
//				colMap, databaseIndex, variableProbes,true);
//		Map<String, SharedValueElement> tableSharedVariablesMap = getSharedValueElementMap(
//				colMap, tableIndex, variableProbes,false);
//	
//		CalculationContext calculationContext = buildCalculationContext(
//				databaseIndex, tableIndex, databaseSharedVariablesMap,
//				tableSharedVariablesMap);
//		LogicTable clonedLogicTable = logicTable.calculate(calculationContext);
//		TargetDBMetaData returnTargetDBMeta = getTargetDatabaseMetaDataBydatabaseGroups(clonedLogicTable);
//		return returnTargetDBMeta;
//	}
//
//	public TargetDBMetaData getDBAndTabs(String virtualTabName,
//			Map<String, Comparative> colMap) throws TDLCheckedExcption {
//		LogicTable logicTable = root.getLogicTableMap(virtualTabName);
//		if(logicTable == null){
//			throw new IllegalArgumentException("���ܸ���ָ���߼������ҵ���Ӧ��ִ�й����߼�����" +
//					"��:"+virtualTabName);
//		}
//		PartitionElement partitionElement =logicTable.getPartitionElement();
//		
//		List<Set<String>> listSetStrings = partitionElement.getDb();
//		int databaseIndex = match(listSetStrings, colMap);
//		
//		listSetStrings = partitionElement.getTab();
//		int tableIndex = match(listSetStrings, colMap);
//		
//		TargetDBMetaData returnTargetDBMeta = buildTargetDBMetaData(colMap,
//				logicTable, databaseIndex, tableIndex);
//		return returnTargetDBMeta;
//	}
//
//	public TargetDBMetaData getDBAndTabs(String logicTableName,
//			Map<String, Comparative> colMap, int databaseRuleIndex,
//			int tableRuleIndex) throws TDLCheckedExcption {
//		LogicTable logicTable = root.getLogicTableMap(logicTableName);
//		if(logicTable == null){
//			throw new IllegalArgumentException("���ܸ���ָ���߼������ҵ���Ӧ��ִ�й����߼�����" +
//					"��:"+logicTableName);
//		}
//		TargetDBMetaData returnTargetDBMeta = buildTargetDBMetaData(colMap,
//				logicTable, databaseRuleIndex, tableRuleIndex);
//		return returnTargetDBMeta;
//	}
//	public TargetDBMetaData getDBAndTabs(String virtualTableName,
//			String databaseGroupsID, Set<String> tables)
//			throws TDLCheckedExcption {
//		throw new IllegalArgumentException();
//	}
//
//	public DBType getDBType() {
//		return (DBType)root.getDBType();
//	}
//
//	public PartitionElement getPartitionColumns(String logicTableName) {
//		LogicTableRule logicTable = root.getLogicTableMap(logicTableName);
//		return logicTable.getPartitionElement();
//	}
//
//	/* (non-Javadoc)
//	 * @see com.taobao.tddl.rule.ruleengine.RouteRuleSet#getPartitionElement(java.lang.String)
//	 *�½ӿ�Ҫ���ʵ�� ��adapter
//	 */
//	public PartitionElement getPartitionElement(String logicTableName) {
//		// TODO �Ժ������Ľӿ�Ϊ����һ��set
//		return getPartitionColumns(logicTableName);
//	}
//
//	public TDDLRoot getRoot() {
//		return root;
//	}
//
//
//
//	private Map<String, SharedValueElement> getSharedValueElementMap(
//			Map<String, Comparative> colMap, int ruleIndex,
//			VariableProbes variableProbes,boolean isDatabseRule) {
//		Map<String, SharedValueElement> sharedVariablesMap = null;
//		if(ruleIndex == NO_INDEX){
//			sharedVariablesMap = Collections.emptyMap();
//		}else{
//			if(isDatabseRule){
//				sharedVariablesMap = variableProbes.
//				getDatabseSharedVariablesMap(ruleIndex, colMap);
//			}else{
//				sharedVariablesMap = variableProbes
//				.getTableSharedVariablesMap(ruleIndex, colMap);
//			}
//		}
//		return sharedVariablesMap;
//	}
//
//	@SuppressWarnings("unchecked")
//	protected TargetDBMetaData getTargetDatabaseMetaDataBydatabaseGroups(
//			LogicTable clonedLogicTable) {
//		//����ӵ�Ŀ���
//		List<TargetDB> targetDatabases = new ArrayList<TargetDB>();
//		Map<String,Database> beingSelectedDatabases = (Map<String, Database>) clonedLogicTable
//				.getSubSharedElements();
//		for (Database database : beingSelectedDatabases.values()) {
//			TargetDB db = new TargetDB();
//			db.setDbIndex( database.getDataSourceKey());
//			Set<String> tempTableNameSet = new HashSet<String>();
//			Map<String, SharedElement> tablesMap =  database.getTables();
//			Collection<? extends SharedElement> tableSharedElement =tablesMap.values();
//			for(SharedElement shearedElement : tableSharedElement){
//				Table table = (Table)shearedElement;
//				tempTableNameSet.add(table.getTableName());
//			}
//			db.setTableNames(tempTableNameSet);
//			targetDatabases.add(db);
//		}
//		
//		// targetDatabase.set
//		TargetDBMetaData targetDatabasemetaData = new TargetDBMetaData(
//				clonedLogicTable.getLogicTableName(), targetDatabases, clonedLogicTable
//						.isNeedRowCopy(), clonedLogicTable.isAllowReverseOutput());
//		return targetDatabasemetaData;
//	}
//
//	public int match(List<Set<String>> ruleArgs,Map<String,Comparative> colMap){
//		int returnIndex = 0;
//		boolean isMatch = false;
//		for(Set<String> setStrings:ruleArgs){
//			for(String key:setStrings){
//				if(!colMap.containsKey(key)){
//					//�����contain�����ʾ��ǰindex���������㣬��ô��������ǰ��setѭ����
//					isMatch = false;
//					break;
//				}else{
//					//�����������isMatchΪtrue.Ȼ����������һ������
//					isMatch = true;
//				}
//			}
//			//���е�ǰList index�µĲ����������ϣ����ߵ�ǰindex�µĹ�������Ĳ����������㡣
//			if(isMatch){
//				//�����������
//				return returnIndex;
//			}else{
//				//����������㣬��ôindex������
//				returnIndex++;
//			}
//		}
//		//���û��һ���������㣬��ôӦ�÷���Ĭ��
//		return NO_INDEX;
//	}
//
//	public void setRoot(TDDLRoot root) {
//		this.root = root;
//	}
//
//}
