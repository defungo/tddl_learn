package com.taobao.tddl.client.controller;

import java.util.List;
import java.util.Map;

import com.taobao.tddl.interact.bean.Field;
import com.taobao.tddl.interact.bean.ReverseOutput;
import com.taobao.tddl.interact.bean.TargetDB;

public interface DatabaseExecutionContext
{
	/** ����targetDB 
	 * 
	 * @deprecated �����Է������Ժ�����̭��
	 * @return
	 */
	TargetDB getTargetDB();
	
	/**
	 * ��ȡ�����Ϳ���pair��list
	 * 
	 * List{��Ӧһ��sql�б�}<Map<String{��Ӧ�߼���}, String{��Ӧ��ʵ��}>>
	 * @return
	 */
	List<Map<String/*logicTable*/, String/*targetTable*/>> getTableNames();
	
	/**
	 * ��ȡ��ǰ���index
	 * @return
	 */
	String getDbIndex();
	
	/**
	 * �趨�������sql
	 * @param outputSQL
	 */
	void setOutputSQL(List<ReverseOutput> outputSQL);
	
	/**
	 * ��ȡ�������sql
	 * @return
	 */
	public List<ReverseOutput> getOutputSQL();
	
	public Map<String, Field> getRealTableFieldMap();
}
