package com.taobao.tddl.client.jdbc.replication;

import java.sql.SQLException;
import java.util.List;

import com.taobao.tddl.client.jdbc.SqlExecuteEvent;

/**
 * ���ƹ��̸��ؼ��¼��Ļص�������
 * 
 * @author linxuan
 *
 */
public interface ReplicationCallbackHandler {
	/**
	 * ������־��ʧ��ʱ�ص�
	 * @param event
	 * @param exceptions
	 */
	void insertSyncLogFailed(SqlExecuteEvent event, List<SQLException> exceptions) throws SQLException;
	
	//...�����¼�
}
