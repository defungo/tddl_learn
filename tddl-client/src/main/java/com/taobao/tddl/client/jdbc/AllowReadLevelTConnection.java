package com.taobao.tddl.client.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map.Entry;

import javax.sql.DataSource;

import com.taobao.tddl.client.pipeline.PipelineFactory;

/**
 * ��������ӣ���һ������Ϊ�����ӣ���������Ϊ�������� ����������������񣬸�����������������������������ȡ ���ֻ�������ӣ���ô���븴��������
 * 
 * ������һ�������Ͳ��ᶪʧ�����뽫��ǰ���ӳ��׹رգ����߽�autoCommit ��false->true��Ȼ�����tryClose�����ر�������
 * ���������Ӳ�����ʧ��
 * 
 * @author shenxun
 * 
 */
public class AllowReadLevelTConnection extends TConnectionImp {
	public AllowReadLevelTConnection(
			boolean enableProfileRealDBAndTables,PipelineFactory pipelineFactory) {
		super(enableProfileRealDBAndTables,pipelineFactory);
	}

	protected String transactionKey;

	@Override
	public Connection getConnection(String dbIndex, boolean goSlave)
			throws SQLException {

		Connection conn = connectionMap.get(dbIndex);

		if (conn == null) {
			DataSource datasource = dsMap.get(dbIndex);
			if (datasource == null) {
				throw new SQLException(
						"can't find datasource by your dbIndex :" + dbIndex);
			}
			// ��ǰdbIndexû�б���������ʹ�ã���ʼ��dsGroupImp,
			if (isAutoCommit) {
				conn = datasource.getConnection();
				conn.setAutoCommit(isAutoCommit);

				connectionMap.put(dbIndex, conn);
			} else {
				// ����״̬��
				boolean needSetAutoCommit = validThrowSQLException(dbIndex,
						goSlave);
				conn = datasource.getConnection();
				if (needSetAutoCommit) {
					// �����Ҫ�½�����
					conn.setAutoCommit(isAutoCommit);
				}
//				else
				//���ﲻ��ʾ������Ϊtrue��
//				{
//					conn.setAutoCommit(true);
//				}
				connectionMap.put(dbIndex, conn);
			}
			return conn;
		} else {
			if (!isAutoCommit) {
				// ���������ж������������������ж������ô��ȡ���������������������
				// ������Ҫ����д�����select for update�������׳��쳣
				validThrowSQLException(dbIndex, goSlave);
			}
			// else{
			// //����������У�������������ӱ��رյ��ˡ���ô�ͻ��ߵ����ѡ��,ʵ�ʳ�����
			// ������֣���Ϊֻ��tryClose�������������������ӹر�
			// }
			return conn;
		}
	}

	@Override
	protected List<SQLException> setAutoCommitTrue2False(boolean autoCommit,
			List<SQLException> sqlExceptions) throws SQLException {
		validTransactionCondition(false);
		boolean firstIn = true;
		for (Entry<String, Connection> entry : connectionMap
				.entrySet()) {
			if(isTransactionConnection(entry.getKey())){
				if(firstIn){
					
					firstIn = false;
					this.transactionKey = entry.getKey();
				}
				sqlExceptions = setAutoCommitAndPutSQLExceptionToList(autoCommit,
						sqlExceptions, entry);
			}
		}
		return sqlExceptions;
	}
	
	@Override
	protected List<SQLException> setAutoCommitFalse2True(boolean autoCommit,
			List<SQLException> sqlExceptions) {
		try {
			return super.setAutoCommitFalse2True(autoCommit, sqlExceptions);
		} finally{
			//�������������
			this.transactionKey = null;
		}
	}

	@Override
	protected boolean isTransactionConnection(String dbIndex) {
		if (dbIndex == null) {
			return true;
		}
		return dbIndex.equals(transactionKey);
	}

	@Override
	public void tryClose(String dbIndex) throws SQLException {
		Connection conn = connectionMap.get(dbIndex);
		if (conn == null) {
			// �����ǰdsGroupû����map�ڣ���ô�򵥵ķ���
			// ����һ�����͵ĳ�������setAutoCommit(false->true)�Ĺ����У�ҲҪ��ʾ�Ĺر�
			// ���쳣״̬��ҲҪ�رգ����Ի��Ǵ�log�رհɡ�
			log.warn("should not be here ");
			return;
		}
		if (isAutoCommit && openedStatements.size() <= 1) {
			// ������״̬��,���Ҵ򿪵�statementֻ��һ����
			try {
				//����Ƿ�����״̬������transactionKey��Ϊ�գ�����transactionKey��dbIndex key��ͬ
				if(transactionKey != null&&transactionKey.equals(dbIndex)){
					log.warn("should not be here! transaction Key is not null !"+transactionKey);
					transactionKey = null;
				}
				// ���е�ǰ���õ�ǰ���£���ʾ�ⲿ�Ѿ�û���ٳ��е�ǰ�����ˡ��ر����ӡ�
				conn.close();
			} finally {
				// �Ƴ���ǰ����Դ
				connectionMap.remove(dbIndex);
			}
			// todo:���ﻹ�и������Ż��ĵط��������openedStatements.size
			// >1��ʱ�򣬱�������statements���������statement.isResultSetClosed��Ϊtrue������Թر�����
		}
	}

	protected boolean validThrowSQLException(String dbIndex, boolean isGoSlave)
			throws SQLException {
		if (transactionKey == null) {
			// �����û��ָ����transactionkey,��ô��ǰ�������Ӿ���ΪĬ�ϵ�transaction���Ӵ���
			if(!isGoSlave)
			{
				transactionKey = dbIndex;
				return true;
			}
			else
			{
				return false;
			}
		} else {
			if (!transactionKey.equals(dbIndex) && !isGoSlave) {
				// �����ǰkey����Ĭ�ϵ�����key,��������Ҫд���������ģ�Ҳ�͵��ڳ��˷������Զ������������ݿ���ʲ���)
				throw new SQLException("������д�뵽�����ͬ�����ݿ�ڵ��У�");
			}
			return false;
		}
	}
}
