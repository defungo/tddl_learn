package com.taobao.tddl.rule.ruleengine.rule;

import java.util.Map;

import javax.sql.DataSource;

/**
 * TODO:С��tair�п��ܵĲ�������
 * @author shenxun
 *
 */
public interface MapCache {
	/**
	 * ����
	 * @param nameSpace ��Ӧ������Ҳ��Ӧcache�е�namespace
	 * @param values
	 * @return affect rows
	 */
	public int put(String nameSpace ,Map<String, Object> values);
	/**
	 * �ó�
	 * @param nameSpace ��Ӧ������Ҳ��Ӧcache�е�namespace
	 * @param key ��Ӧkey
	 * @param column ��value��sql�е�������key�������Ǻ�dba����Լ���Ϳ��Խ����
	 * @return
	 */
	public Object get(String nameSpace ,Object key,String column);
	public void setTargetDatasource(DataSource targetDatasource) ;
}
