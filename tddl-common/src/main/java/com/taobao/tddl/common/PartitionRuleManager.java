package com.taobao.tddl.common;

/**
 * �ֿ�������
 *
 * @author nianbing
 */
public interface PartitionRuleManager {
	/**
	 * �����������ݿ����ͱ����õ�����
	 *
	 * @param masterName �������ݿ���
	 * @param tableName ����
	 * @return �������������û�����ã��򷵻�null��
	 */
	String getPrimaryKey(String masterName, String tableName);

	/**
	 * �����������ݿ����ͱ����õ��ֿ��
	 *
	 * @param masterName �������ݿ���
	 * @param tableName ����
	 * @return ���طֿ�������û�����ã��򷵻�null��
	 */
	String getPartitionKey(String masterName, String tableName);

	/**
	 * ���ݷֿ���򷵻طֿ��б�
	 *
	 * @param masterName �������ݿ���
	 * @param tableName ����
	 * @param value �ֿ��ֵ
	 * @return ���طֿ��б�����ֿ��ֵ��ƥ���κηֿ�����򷵻�null��
	 */
	String[] getSlaves(String masterName, String tableName, Object value);
}
