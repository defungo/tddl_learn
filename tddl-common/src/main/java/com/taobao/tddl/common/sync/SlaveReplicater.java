package com.taobao.tddl.common.sync;

import java.util.Map;

/**
 * ������ݸ��ƣ�ͬ�����ӿڡ��ص��ӿ�
 * 
 * @author linxuan
 * 
 */
public interface SlaveReplicater {
	
	/**
	 * ���������ɹ������
	 * @param masterRow ���������һ�����ݡ�key��������value����ֵ
	 * @param slave ��ӦTDataSource.replicationConfigFileָ��ĸ��������ļ�(����tddl-replication.xml)�е�slaveInfo������Ϣ
	 */
	void insertSlaveRow(Map<String, Object> masterRow, SlaveInfo slave);

	/**
	 * ��������³ɹ������
	 * @param masterRow ���������һ�����ݡ�key��������value����ֵ
	 * @param slave ��ӦTDataSource.replicationConfigFileָ��ĸ��������ļ�(����tddl-replication.xml)�е�slaveInfo������Ϣ
	 */
	void updateSlaveRow(Map<String, Object> masterRow, SlaveInfo slave);
}
