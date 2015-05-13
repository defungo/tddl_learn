package com.taobao.tddl.client.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * �û�����һ�Զ�����ӹ�ϵ��
 * 
 * ��Ҫ�Ǵ�����������⣬Manager��������ü���������ǰ�����Ƿ�Ӧ�ñ��رյ���
 * 
 * ��û�������ʱ����Ҫ��ģ����˭����˭�رա�
 * 
 * ���������ʱ��ģ�ͱ�Ϊ������������Ӧ�ر�����ֻȥ������
 * 
 * @author shenxun
 *
 */
public interface ConnectionManager {
	/**
	 * �Ƿ����Զ��ύ
	 * 
	 * @return
	 */
//	�˷�����ConnectionҪ��Manager˵��һ��autoCommitʱ����Ϊ
	public boolean getAutoCommit() throws SQLException;

//	�˷�����ConnectionҪ��Manager˵��һ��autoCommitʱ����Ϊvoid setAutoCommit(boolean isAutoCommit) throws SQLException;
	
	/**
	 * ���Ի�ȡһ������
	 * ����: 
	 * 
	 * ������:
	 * 
	 * ���goMaster == true ������Դ����һ����
	 * 		���״���
	 * 
	 * ����ڷ�����
	 * 		�����״���
	 * 
	 * ��ÿ�γ��Դ��»�ȡ���ӵ�ʱ�򣬶�������ʾ�Ľ���������RetryableDatasourceGrooup.autocommit()
	 * Ϊָ����ֵ��
	 * 
	 * ���ɵĵط��ǣ�����������У���ѯ�����⣬�Ƿ�Ӧ����������ѯ�أ�
	 * 
	 * @param dbIndex
	 * @return
	 */
	Connection getConnection(String dbIndex,boolean goSlave) throws SQLException;

	/**
	 * ����رյ�ǰ����
	 * 
	 * ������״̬��������ҽ���һ��TStatement����������ر��������ӡ�����ж��TStatement����������ʲô������.
	 * �����и�ǰ��������Ҳ����һ��TStatementֻ���ܶ�Ӧһ��TResultSet.��˵�TStatement�����Ӧ��TResultSet��ֻ�п�����һ������
	 * ����tryClose������
	 * 
	 * ����״̬�����ر����ӡ�
	 * 
	 * ���������ʼ�����ü������뷨����Ϊģ����Ը��ӣ����������ĸ���Ӱ���ǣ�
	 * 		�ڷ�����״̬�£�����һ��TStatement��ʱ���رղ���ʱ
	 * ��
	 * 
	 * ���ô˷�����TStatement��TResultSet ����ȡ����쳣��ò��Ҳûʲô�ð취��logһ��
	 * 			
	 * @param dbIndex
	 * @throws SQLException ����ر�ʱ�������쳣��ֱ���׳� ��Ҫ����ʾ��cacheס
	 */
	void tryClose(String dbIndex) throws SQLException;
	
	/**
	 * ��ȡ���������
	 * 
	 * @return
	 */
	Connection getProxyConnection();
	
	/**
	 * �Ƴ���ǰstatement
	 * 
	 * @param statement
	 */
	void removeCurrentStatement(Statement statement);
	
	boolean containDBIndex(String dbIndex);
}
