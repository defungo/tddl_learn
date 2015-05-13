package com.taobao.tddl.sqlobjecttree;

import java.util.Map;
import java.util.Set;

import com.taobao.tddl.common.sqlobjecttree.SQLFragment;


/**
 * ����ʵ��ǿ��������ǿ�ƺ��������Ľӿ�
 * 
 * ��ʾһ����
 * 
 * @author shenxun
 * 
 */
public interface TableName extends SQLFragment{
	public void setAlias(String alias);
	/**
	 * �������б��������ڲ���ת��Ϊlower case
	 * @return
	 */
	public Set<String> getTableName();
	public String getAlias();
	/**
	 * �������ͱ����ŵ�һ��map�У� ����key���Զ�תΪ��д
	 * @param map
	 */
	public void appendAliasToSQLMap(Map<String, SQLFragment> map);
	public void setJoinClause(JoinClause joinClause);
}