package com.taobao.tddl.client.dispatcher;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.taobao.tddl.client.RouteCondition;

/**
 * ����Դ�Ͷ�Ӧ�б��ѡ����������ͨ��sql��arg��ȡִ��Ŀ��
 * Ҳ����ͨ��rc��ȡ��ͬʱ������ͨ������ӿڻ�����е����ݿ�ͱ�
 * 
 * Result�ṹ���ڲ�ʵ���޹أ�ҵ�񷽿��Խ����޸� ����Ӱ�쵽TDDL�ڲ�ʵ�֡�
 * 
 * @author shenxun
 *
 */
public interface DatabaseChoicer {
	/**
	 * ��ȡ��ǰ���ݿ�ͱ�
	 * @param sql
	 * @param args
	 * @return
	 * @throws TDLCheckedExcption
	 */
	Result getDBAndTables(String sql, List<Object> args);
	
	/**
	 * ������SQL����ThreadLocal�����ָ������RouteCondition�����������Ŀ�ĵصĽӿ�
	 * @param rc
	 * @return
	 */
	Result getDBAndTables(RouteCondition rc);
	
	/**
	 * ��ȡȫ��ȫ����Ϣ
	 * @param logicTableName
	 * @return
	 */
	Result getAllDatabasesAndTables(String logicTableName);
	
	/**
	 * 2.4.4�¹���ȡ��ȫ��ȫ�����Ϣ
	 * @param logicTableName
	 * @return
	 */
    Map<String, Set<String>> getDbTopology(String logicTableName);
}
