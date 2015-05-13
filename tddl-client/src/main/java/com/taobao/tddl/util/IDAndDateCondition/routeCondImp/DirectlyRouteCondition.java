package com.taobao.tddl.util.IDAndDateCondition.routeCondImp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.taobao.tddl.client.RouteCondition;
import com.taobao.tddl.client.controller.OrderByMessages;
import com.taobao.tddl.client.controller.OrderByMessagesImp;
import com.taobao.tddl.interact.rule.bean.DBType;
import com.taobao.tddl.sqlobjecttree.DMLCommon;
import com.taobao.tddl.sqlobjecttree.GroupFunctionType;


public class DirectlyRouteCondition implements RouteCondition{
	/**
	 * Ĭ�J��mysql
	 */
	protected DBType dbType = DBType.MYSQL;
	protected int skip = DMLCommon.DEFAULT_SKIP_MAX;
	protected String suffix ;
	boolean isSuffixModel = false;

	protected int max = DMLCommon.DEFAULT_SKIP_MAX;
	/**
	 * Ĭ��Ϊ��
	 */
	@SuppressWarnings("unchecked")
	protected OrderByMessages orderByMessages = new OrderByMessagesImp(Collections.EMPTY_LIST);
	/**
	 * Ĭ��Ϊ��
	 */
	protected GroupFunctionType groupFunctionType = GroupFunctionType.NORMAL;
	protected ROUTE_TYPE routeType = ROUTE_TYPE.FLUSH_ON_EXECUTE;
	/**
	 * Ŀ����id
	 */
	protected Set<String> tables = new HashSet<String>(2);
	protected String virtualTableName;
	/**
	 * Ŀ����id
	 */
	protected String dbRuleID;
	public Set<String> getTables() {
			return tables;
	}

	public void setDBType(DBType dbType) {
		this.dbType = dbType;
	}
	
	public DBType getDBType() {
		return dbType;
	}

	public void setTables(Set<String> tables) {
		this.tables = tables;
	}
	
	public void addATable(String table){
		tables.add(table);
	}
	public String getVirtualTableName() {
		return virtualTableName;
	}
	/**
	 * �������
	 * @param virtualTableName
	 */
	public void setVirtualTableName(String virtualTableName){
		this.virtualTableName=virtualTableName;
	}
	public String getDbRuleID() {
		return dbRuleID;
	}
//	/**
//	 * �����id����db idͬ����
//	 * 
//	 * �ڽ����жϵ�ʱ�򣬻��Ȳ鿴�����ļ����Ƿ��ж�Ӧ�Ĺ���
//	 * 
//	 * ���û�У�����������ݿ�map���Ƿ��ж�Ӧ�ģ���û���򱨴�
//	 * 
//	 * @param dbRuleID
//	 */
//	public void setDbRuleID(String dbRuleID) {
//		this.dbRuleID = dbRuleID;
//	}

	/**
	 * �����id����db idͬ����
	 * 
	 * �ڽ����жϵ�ʱ�򣬻��Ȳ鿴�����ļ����Ƿ��ж�Ӧ�Ĺ���
	 * 
	 * ���û�У�����������ݿ�map���Ƿ��ж�Ӧ�ģ���û���򱨴�
	 * 
	 * @param dbId
	 */
	public void setDBId(String dbId){
		this.dbRuleID = dbId;
	}
	
	public ROUTE_TYPE getRouteType() {
		return routeType;
	}
	
	/**
	 * ��ȡskipֵ
	 * ��ΪTDDL�������ݿ⣬������������һ���ٶ���
	 * �����к���Ϊskip�������У������Ǹ���Զ��������ġ�
	 * 
	 * ���Ƕ����Ҳ����ˡ�
	 * @return
	 */
	public int getSkip(){
		return skip;
	}

	/**
	 * ��ȡmaxֵ��
	 * ��ΪTDDL�������ݿ⣬������������һ���ٶ���
	 * �����к���Ϊmax�������У������Ǹ���Զ��������ġ�
	 * 
	 * ���Ƕ����Ҳ����ˡ�
	 * @return
	 */
	public int getMax(){
		return max;
	}

	/**
	 * ��ȡorder by ��Ϣ
	 * @return
	 */
	public OrderByMessages getOrderByMessages(){
		return orderByMessages;
	}

	/**
	 * ��ȡ��ǰsql��select | columns | from
	 * ��columns������
	 * ���Ϊmax min count�ȣ���ô���ͻ�����Ӧ�仯
	 * ͬʱ���group function�����������ֶλ��ã�������᷵��NORMAL
	 * @return
	 */
	public GroupFunctionType getGroupFunctionType(){
		return groupFunctionType;
	}
	public String getSuffix() {
		return suffix;
	}
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}
	
	public Map<String/*db index key */, List<Map<String/* original table */, String/* targetTable */>>> getShardTableMap() {
	
		List<Map<String/* original table */, String/* targetTable */>> tableList = new ArrayList<Map<String, String>>(
				1);
		for (String targetTable : tables) {
			Map<String/* original table */, String/* target table */> table = new HashMap<String, String>(
					tables.size());
			table.put(virtualTableName, targetTable);
			if(!table.isEmpty()){
				tableList.add(table);
			}
		}

		Map<String/* key */, List<Map<String/* original table */, String/* targetTable */>>> shardTableMap = new HashMap<String, List<Map<String, String>>>(
				2);
		shardTableMap.put(dbRuleID, tableList);
		return shardTableMap;
	}
	public boolean isSuffixModel() {
		return isSuffixModel;
	}
	public void setSuffixModel(boolean isSuffixModel) {
		this.isSuffixModel = isSuffixModel;
	}

	public DBType getDbType() {
		return dbType;
	}

	public void setDbType(DBType dbType) {
		this.dbType = dbType;
	}

	public void setSkip(int skip) {
		this.skip = skip;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public void setOrderByMessages(OrderByMessages orderByMessages) {
		this.orderByMessages = orderByMessages;
	}

	public void setGroupFunctionType(GroupFunctionType groupFunctionType) {
		this.groupFunctionType = groupFunctionType;
	}

	public void setRouteType(ROUTE_TYPE routeType) {
		this.routeType = routeType;
	}
	
}
