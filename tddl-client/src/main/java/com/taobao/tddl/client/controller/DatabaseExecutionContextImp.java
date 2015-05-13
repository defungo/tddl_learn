package com.taobao.tddl.client.controller;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.taobao.tddl.interact.bean.Field;
import com.taobao.tddl.interact.bean.ReverseOutput;
import com.taobao.tddl.interact.bean.TargetDB;

public class DatabaseExecutionContextImp implements DatabaseExecutionContext
{
	public DatabaseExecutionContextImp()
	{
		tableNames = new LinkedList<Map<String, String>>();
	}

	/**
	 * �������TDatasource�����е�����
	 */
	private String dbIndex;
	
	private Map<String, Field> realTableFieldMap;

	/**
	 * ��������µķ��ϲ�ѯ�����ı����б�
	 */
	private final List/*���sql*/</*ÿһ��sql����Ҫ�滻�ı���*/Map<String/* logic table name */, String/* real table name */>> tableNames;
	
	/**
	 * ���������sql,���reverseOutput��Ϊfalse,�����ﲻ��Ϊnull. ����Ȼ����Ϊһ��empty list
	 */
	private List<ReverseOutput> outputSQL;

	public String getDbIndex()
	{
		return dbIndex;
	}

	public void setDbIndex(String dbIndex)
	{
		this.dbIndex = dbIndex;
	}

	public List<Map<String, String>> getTableNames()
	{
		return tableNames;
	}

	/**
	 * ���һ�� ������
	 * 
	 * Map<String Դ����, String Ŀ�����>
	 * @param pair
	 */
	public void addTablePair(Map<String, String> pair)
	{
		tableNames.add(pair);
	}

	public void addTablePair(String key,String value)
	{
		Map<String, String> pair = new HashMap<String, String>(1,1);
		pair.put(key, value);
		tableNames.add(pair);
	}
	
	public List<ReverseOutput> getOutputSQL()
	{
		return outputSQL;
	}

	public void setOutputSQL(List<ReverseOutput> outputSQL)
	{
		this.outputSQL = outputSQL;
	}

	public TargetDB getTargetDB()
	{
		TargetDB targetDB  = new TargetDB();
		targetDB.setDbIndex(dbIndex);
		for(Map<String, String> map : tableNames)
		{
			if(1 != map.size()){
				throw new IllegalArgumentException("����ģʽ��֧�ֶ�����0����:"+map.size());
			}
			targetDB.addOneTable(map.values().iterator().next());
		}
		targetDB.setOutputSQL(outputSQL);
		return targetDB;
	}

	public Map<String, Field> getRealTableFieldMap() {
		return realTableFieldMap;
	}

	public void setRealTableFieldMap(Map<String, Field> realTableFieldMap) {
		this.realTableFieldMap = realTableFieldMap;
	}
}
