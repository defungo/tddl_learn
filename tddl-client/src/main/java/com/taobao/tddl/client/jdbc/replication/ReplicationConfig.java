package com.taobao.tddl.client.jdbc.replication;

import java.util.Map;

import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.taobao.tddl.client.jdbc.TDataSource;
import com.taobao.tddl.common.ConfigServerHelper;
import com.taobao.tddl.common.ConfigServerHelper.DataListener;
import com.taobao.tddl.common.config.DefaultTddlConfigParser;
import com.taobao.tddl.common.config.TddlConfigParser;
import com.taobao.tddl.common.sync.BizTDDLContext;

/**
 * �и��������Ļ���
 * �����и�����Ҫ���������ã���־�����á���θ��Ƶ�����
 * ʵʱ�и��ƺͲ�������������
 * 
 * @author linxuan
 *
 */
public class ReplicationConfig {
	//private EquityDbManager syncLogDb;
	private boolean isUseLocalConfig = false;
	private String appName;
	private Map<String, BizTDDLContext> logicTableName2TDDLContext;
	private String replicationConfigFile; //�±����ļ�����

	private TddlConfigParser<Map<String, BizTDDLContext>> configParser = new DefaultTddlConfigParser<Map<String, BizTDDLContext>>();
	private final DataListener replicationListener = new DataListener() {
		public void onDataReceive(Object data) {
			//��֧�ֶ�̬�޸�
		}

		public void onDataReceiveAtRegister(Object data) {
			if (data != null) {
				//TODO ��runtimeHolder������
				ReplicationConfig.this.logicTableName2TDDLContext = configParser.parseCongfig((String) data);
			}
		}
	};

	public void init(TDataSource tds) {
		if (!this.isUseLocalConfig) {
			this.logicTableName2TDDLContext = null;
			//�����и�������
			Object first = ConfigServerHelper.subscribeReplicationConfig(this.appName, replicationListener);
			if (first == null) {
				throw new IllegalStateException("û�н��յ��и�������");
			}
			if (this.logicTableName2TDDLContext == null) {
				throw new IllegalStateException("�����и�������ʧ�ܣ�" + first);
			}
		} else if (replicationConfigFile != null) {
			FileSystemXmlApplicationContext ctx = new FileSystemXmlApplicationContext(replicationConfigFile);
			this.logicTableName2TDDLContext = convert(ctx.getBean("root"));
		} else if (this.logicTableName2TDDLContext == null) {
			throw new IllegalArgumentException("logicTableName2TDDLContext����û������");
		}

		//��TDataSource��ʼ��logicTableName2TDDLContext
		ReplicationHelper.initReplicationContextByTDataSource(tds, this.logicTableName2TDDLContext);
	}

	@SuppressWarnings("unchecked")
	private <T> T convert(Object obj) {
		return (T) obj;
	}

	/**
	 * ���߼���getter/setter
	 */
	/*public EquityDbManager getSyncLogDb() {
		return syncLogDb;
	}

	public void setSyncLogDb(EquityDbManager syncLogDb) {
		this.syncLogDb = syncLogDb;
	}*/

	public void setLogicTableName2TDDLContext(Map<String, BizTDDLContext> logicTableName2TDDLContext) {
		this.logicTableName2TDDLContext = logicTableName2TDDLContext;
	}

	public Map<String, BizTDDLContext> getLogicTableName2TDDLContext() {
		return logicTableName2TDDLContext;
	}

	public void setUseLocalConfig(boolean isUseLocalConfig) {
		this.isUseLocalConfig = isUseLocalConfig;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public void setReplicationConfigFile(String replicationConfigFile) {
		this.replicationConfigFile = replicationConfigFile;
	}
}
