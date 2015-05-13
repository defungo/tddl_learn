package com.taobao.tddl.client.jdbc.replication;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import com.taobao.tddl.client.jdbc.TDataSource;
import com.taobao.tddl.common.sync.BizTDDLContext;
import com.taobao.tddl.common.sync.SlaveInfo;
import com.taobao.tddl.common.sync.SlaveReplicater;

/**
 * �и��Ƹ����� 
 * @author linxuan
 */
public class ReplicationHelper {
	
	/**
	 * ��TDataSource�е�master4replication/slave4replication
	 * ��ʼ���������ýṹBizTDDLContext�е������������Դ
	 * @param tds Ӧ�õ�TDataSource����Դ
	 * @param logicTableName2TDDLContext: key���߼�������value����������
	 */
	public static void initReplicationContextByTDataSource(TDataSource tds,
			Map<String, BizTDDLContext> logicTableName2TDDLContext) {

		for (Map.Entry<String, BizTDDLContext> e : logicTableName2TDDLContext.entrySet()) {
			BizTDDLContext rplCtx = e.getValue();
			rplCtx.setMasterJdbcTemplate(new JdbcTemplate(tds));
			for (SlaveInfo slaveInfo : rplCtx.getSlaveInfos()) {
				if (slaveInfo.getName() == null) {
					slaveInfo.setName(e.getKey());
				}
				if (slaveInfo.getSlaveReplicater() != null) {
					continue;
				}
				if (slaveInfo.getSlaveReplicaterName() != null) {
					slaveInfo.setSlaveReplicater((SlaveReplicater) tds.getSpringContext().getBean(
							slaveInfo.getSlaveReplicaterName()));
					continue;
				}
				if (slaveInfo.getJdbcTemplate() != null) {
					continue;
				}
				
				if (slaveInfo.getDataSourceName() == null) {
					throw new IllegalStateException("2.4.1֮��SlaveInfo�� DataSourceName���Ա�������");
				} else {
					DataSource targetDS = tds.getReplicationTargetDataSources().get(slaveInfo.getDataSourceName());
					if (targetDS == null) {
						// ���ݾ�ʵ�֣�Ҫ��dataSourceNameΪkey�����Ƶ�Ŀ������õ���TDataSource�е�dataSourcePool��
						targetDS = tds.getDataSource(slaveInfo.getDataSourceName());
					}
					if (targetDS == null) {
						throw new IllegalArgumentException("[SlaveInfo.dataSourceName]Replication target DataSource"
								+ " could not found in TDataSource config:" + slaveInfo.getDataSourceName());
					}
					slaveInfo.setJdbcTemplate(new JdbcTemplate(targetDS));
				}
			}
		}
	}
}
