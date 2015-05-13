package com.taobao.tddl.client.jdbc;

import java.sql.SQLException;

import com.taobao.tddl.client.pipeline.PipelineFactory;

public class AllowAllLevelTconnection extends AllowReadLevelTConnection {
	public AllowAllLevelTconnection(
			boolean enableProfileRealDBAndTables,PipelineFactory pipelineFactory) {
		super(enableProfileRealDBAndTables,pipelineFactory);
	}

	protected boolean validThrowSQLException(String dbIndex, boolean isGoSlave)
			throws SQLException {
		if (transactionKey == null) {
			// �����û��ָ����transactionkey,��ô��ǰ�������Ӿ���ΪĬ�ϵ�transaction���Ӵ���
			transactionKey = dbIndex;
			return true;
		} else {
			//����д�뵽������ݽڵ��У��������ﲻ�׳��쳣��
			return false;
		}
	}
}
