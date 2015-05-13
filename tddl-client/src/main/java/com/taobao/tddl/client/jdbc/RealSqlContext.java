package com.taobao.tddl.client.jdbc;

import java.util.Map;

import com.taobao.tddl.common.jdbc.ParameterContext;

/**
 * һ��ִ����ĳ�����ݿ��ϵ�sql+�������ܺ�
 * 
 * @author shenxun
 *
 */
public interface RealSqlContext {
	/**
	 * ��ȡ��ǰsql.�ǿ� �����
	 * @return
	 */
	public String getSql();
	
	/**
	 * ��ȡ��ǰsqlִ�еı�������Ҫ���ڼ�¼log.
	 * 
	 * �ǿգ�������ܻ�ã��򷵻� ""
	 * @return
	 */
	public String getRealTable();
	
	/**
	 * ��ȡ�뵱ǰִ��sql���׵Ĳ�������Ϊ�գ����Ϊ�ձ�ʾ��ǰsqlû�в�����
	 * 
	 * @return
	 */
	public Map<Integer, ParameterContext>  getArgument();
}
