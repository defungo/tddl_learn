package com.taobao.tddl.common;

import com.taobao.tddl.client.ThreadLocalString;
import com.taobao.tddl.client.util.ThreadLocalMap;

/**
 * �ṩ������ʹ��GroupDataSource���û�ָ������Դ�Լ����ִ����Ϣ
 * 
 * @author junyu
 * 
 */
public class GroupDataSourceRouteHelper {
	/**
	 * ��һ������Դ��ѡ��һ��ָ������ϵ�����Դִ��SQL��
	 * 
	 * �磺groupKey=ExampleGroup ��Ӧ��contentΪ db1:rw,db2:r,db3:r
	 * 
	 * <pre>
	 * RouteHelper.executeByGroupDataSourceIndex(2);
	 * jdbcTemplate.queryForList(sql);
	 * </pre>
	 * 
	 * ���ղ�ѯ�϶����ڵ���������Դ��ִ�У�db3�� ע�⣬ָ��db�Ķ�д������Ҫ����Ҫ���粻���� ָ��ֻ������Դ�Ͻ���д�����������״�
	 * 
	 * @author junyu
	 * @param dataSourceIndex
	 *            ��ָ��Group�У�����Ҫִ�е�db���
	 */
	public static void executeByGroupDataSourceIndex(int dataSourceIndex) {
		ThreadLocalMap.put(ThreadLocalString.DATASOURCE_INDEX, dataSourceIndex);
	}

	public static void executeByGroupDataSourceIndex(int dataSourceIndex,
			boolean failRetry) {
		ThreadLocalMap.put(ThreadLocalString.DATASOURCE_INDEX, dataSourceIndex);
		ThreadLocalMap.put(ThreadLocalString.RETRY_IF_SET_DS_INDEX, failRetry);
	}

	/**
	 * Ϊ�˱�֤һ���߳�ִ�ж����������ɻ���(�������������������)��
	 * ���ÿ��ҵ�񷽷���try-finally������finally�е��ø÷������index:
	 * 
	 * try{ GroupDataSourceRouteHelper.executeByGroupDataSourceIndex(0);
	 * xxxDao.bizOperationxxx(); }finally{
	 * GroupDataSourceRouteHelper.removeGroupDataSourceIndex(); }
	 * 
	 */
	public static void removeGroupDataSourceIndex() {
		ThreadLocalMap.remove(ThreadLocalString.DATASOURCE_INDEX);
	}
}
