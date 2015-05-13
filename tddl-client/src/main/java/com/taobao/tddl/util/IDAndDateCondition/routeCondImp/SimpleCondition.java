package com.taobao.tddl.util.IDAndDateCondition.routeCondImp;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.taobao.tddl.interact.bean.ComparativeMapChoicer;
import com.taobao.tddl.interact.rule.bean.DBType;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.sqlobjecttree.DMLCommon;
import com.taobao.tddl.sqlobjecttree.GroupFunctionType;
import com.taobao.tddl.sqlobjecttree.InExpressionObject;
import com.taobao.tddl.sqlobjecttree.OrderByEle;
import com.taobao.tddl.sqlobjecttree.SqlParserResult;

public class SimpleCondition implements RuleRouteCondition,ComparativeMapChoicer{

	private String virtualTableName;
	private Set<String> virtualTableForJoin;
	private DBType dbType;
	public static final int EQ=Comparative.Equivalent;
	private Map<String, Comparative> parameters = new HashMap<String, Comparative>();
	private Map<String, Integer> parametersIndexForBatch = new HashMap<String, Integer>();
	private ROUTE_TYPE routeType = ROUTE_TYPE.FLUSH_ON_EXECUTE;
    private List<OrderByEle> orderBys = Collections.emptyList();
    private List<OrderByEle> groupBys = Collections.emptyList();
    private int max = DMLCommon.DEFAULT_SKIP_MAX;
    private int skip = DMLCommon.DEFAULT_SKIP_MAX;
    GroupFunctionType groupFunctionType = GroupFunctionType.NORMAL;
    private List<String> distinctColumn=Collections.emptyList();
    private boolean hasHavingCondition=false;
    private List<InExpressionObject> inObjList=Collections.emptyList();

	public String getVirtualTableName() {
		return virtualTableName;
	}

	public Set<String> getVirtualTableForJoin(){
		return virtualTableForJoin;
	}

	public SqlParserResult getSqlParserResult() {
		//return new DummySqlParcerResult(this, virtualTableName);

		return new DummySqlParcerResult(this);
	}

	public ComparativeMapChoicer getCompMapChoicer() {
		return this;
	}
	public Map<String, Comparative> getColumnsMap(List<Object> arguments,
			Set<String> partnationSet) {
		Map<String, Comparative> retMap = new HashMap<String, Comparative>(parameters.size());
		for(String str : partnationSet){
			if(str != null){
				//��Ϊgroovy�Ǵ�Сд���еģ��������ֻ����ƥ���ʱ��תΪСд������map�е�ʱ����Ȼʹ��ԭ���Ĵ�Сд
				Comparative comp = parameters.get(str.toLowerCase());
				if(comp != null){
					retMap.put(str, comp);
				}
			}
		}
		return retMap;
	}

	@Override
	public Comparative getColumnComparative(List<Object> arguments, String partnationCol) {
		Comparative res = null;
		if(partnationCol != null){
			//��Ϊgroovy�Ǵ�Сд���еģ��������ֻ����ƥ���ʱ��תΪСд������map�е�ʱ����Ȼʹ��ԭ���Ĵ�Сд
			Comparative comp = parameters.get(partnationCol.toLowerCase());
			if(comp != null){
				//retMap.put(str, comp);
				res = comp;
			}
		}
		return res;
	}

	public static Comparative getComparative(int i,Comparable<?> c){
		return new Comparative(i,c);
	}

	/**
	 * �����������
	 *
	 * @param virtualTableName
	 *            �������
	 */
	public void setVirtualTableName(String virtualTableName) {
		if(virtualTableName == null){
			throw new IllegalArgumentException("�������߼�����");
		}
		this.virtualTableName = virtualTableName.toLowerCase();
	}

	public void setVirtualTableForJoin(Set<String> virtualTableForJoin){
		if(virtualTableForJoin == null||virtualTableForJoin.size()==0){
			throw new IllegalArgumentException("����������join���߼�����");
		}
		this.virtualTableForJoin=virtualTableForJoin;
	}

	public Map<String, Comparative> getParameters() {
		return parameters;
	}

	/**
	 * ���һ��Ĭ��Ϊ=�Ĳ�����
	 *
	 * @param str
	 *            ����������
	 * @param comp
	 *            ������ֵ��һ��Ϊ�������ͻ�ɱȽ�����
	 */
	public void put(String key, Comparable<?> parameter) {
		if(key == null){
			throw new IllegalArgumentException("keyΪnull");
		}
		if(parameter instanceof Comparative){
			parameters.put(key.toLowerCase(), (Comparative)parameter);
		}else{
			//(parameter instanceof Comparable<?>
			parameters.put(key.toLowerCase(), getComparative(EQ,parameter));
		}

	}

	/**
	 * @param key   column name
	 * @param index  start from 0
	 */
	public void putParamIndexForBatch(String key, Integer index) {
		if(key == null){
			throw new IllegalArgumentException("keyΪnull");
		}
		parametersIndexForBatch.put(key.toLowerCase(),index);
	}

	public DBType getDBType() {
		return dbType;
	}
	public void setDBType(DBType dbType) {
		this.dbType = dbType;
	}
	public List<OrderByEle> getOrderBys() {
		return orderBys;
	}
	public void setOrderBys(List<OrderByEle> orderBys) {
		this.orderBys = orderBys;
	}
	public List<OrderByEle> getGroupBys() {
		return groupBys;
	}
	public void setGroupBys(List<OrderByEle> groupBys) {
		this.groupBys = groupBys;
	}
	public int getMax() {
		return max;
	}
	public void setMax(int max) {
		this.max = max;
	}
	public int getSkip() {
		return skip;
	}
	public void setSkip(int skip) {
		this.skip = skip;
	}
	public GroupFunctionType getGroupFunctionType() {
		return groupFunctionType;
	}
	public void setGroupFunctionType(GroupFunctionType groupFunctionType) {
		this.groupFunctionType = groupFunctionType;
	}

	public List<String> getDistinctColumn() {
		return distinctColumn;
	}
	public void setDistinctColumn(List<String> distinctColumn) {
		this.distinctColumn = distinctColumn;
	}
	public void setRouteType(ROUTE_TYPE routeType) {
		this.routeType = routeType;
	}
	public ROUTE_TYPE getRouteType() {
		return routeType;
	}

	public void setHasHavingCondition(boolean hasHavingCondition) {
		this.hasHavingCondition = hasHavingCondition;
	}

	public boolean hasHavingCondition(){
		return hasHavingCondition;
	}

	public List<InExpressionObject> getInObjList() {
		return inObjList;
	}

	public void setInObjList(List<InExpressionObject> inObjList) {
		this.inObjList = inObjList;
	}

	public Map<String, Integer> getParametersIndexForBatch() {
		return parametersIndexForBatch;
	}
}
