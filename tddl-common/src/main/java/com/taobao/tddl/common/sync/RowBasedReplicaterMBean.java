package com.taobao.tddl.common.sync;


/**
 * RowBasedReplicater JMX MBean�ӿ�
 * 
 * @author linxuan
 */
public interface RowBasedReplicaterMBean {
	/**
	 * �������еĴ�С
	 */
	int getReplicationQueueSize();

	int getDeleteSyncLogQueueSize();

	int getUpdateSyncLogQueueSize();


	/**
	 * �����̳߳ص����������
	 */
	long getCompletedReplicationCount();

	/**
	 * ɾ����־�̳߳������������ÿ������������ɾ���� 
	 */
	long getCompletedDeleteSyncLogCount();

	/**
	 * ������־�̳߳������������ÿ���������������µ� 
	 */
	long getCompletedUpdateSyncLogCount();

	
	/**
	 * ��̬��غ͵���bucketSize
	 */
	int getDeleteBatchSize();

	void setDeleteBatchSize(int bucketSize);

	int getUpdateBatchSize();

	void setUpdateBatchSize(int bucketSize);
}
