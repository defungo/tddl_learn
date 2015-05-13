package com.taobao.tddl.client.jdbc.replication;

/**
 * �и��ƿ���
 * 
 * @author linxuan
 */
public interface ReplicationSwitcher {
	public enum PropKey {
		level, //�и��Ƽ�������Լ�
		insertSyncLogMode, //������־���ģʽ
		replicationThreadPoolSize, //�и����̳߳ش�С
		insertSyncLogThreadPoolSize, //�첽������־����̳߳ش�С
	}

	public enum Level {
		ALL_ON, //ȫ�����и��ƣ� ͬ��������־�⣬Ӧ�ö�ͬ�����첽ʵʱ�и���
		INSERT_LOG, //ֻͬ��������־�⣬������Ӧ�ö�ʵʱ�и��ơ�ֻ������������������ͬ��
		ALL_OFF, //ȫ���ر��и���,������־������
	}

	public enum InsertSyncLogMode {
		normal, // ���ڵķ�ʽ��ͬ��������־�⣬ʧ���׳��ض��쳣���ɿ��ģ���֤����ʧ����
		logfileonly, //��������־�⣬ֻͬ����¼������log�ļ�(����store4j)(level=INSERT_LOG����ѡ���
		streaking, //�㱼����������־�⣬Ҳ��д����log�ļ�����ȫ����ʵʱ�и��ơ�����һ�����ɶ����dump��֤(level=INSERT_LOG����ѡ���
		/**
		 * ͬ����¼������log�ļ�(����store4j)����Ϊ����log��
		 * 1. ÿ�����¶���log��������¼��ֻҪ��֤log�ļ������첽�̳߳�queue��С�ĸ�����־���ɡ�
		 *    ������Ӧ�������󣬸���queue��С�����ã�ȡ���һ�θ���log���в����ָ�
		 * 2. ���̳߳�queue��ʱ�����̴߳����׳��쳣ʱ����¼��־���������ļ��С������˹��ָ�
		 */
		asynchronous,
	}

	Level level();

	InsertSyncLogMode insertSyncLogMode();

	void addReplicationConfigAware(ReplicationConfigAware replicationConfigAware);

	interface ReplicationConfigAware {
		void setReplicationThreadPoolSize(int threadPoolSize);

		int getReplicationThreadPoolSize();

		void setInsertSyncLogThreadPoolSize(int threadPoolSize);

		int getInsertSyncLogThreadPoolSize();
	}

	public class ReplicationConfigAwareAdaptor implements ReplicationConfigAware {
		private int insertSyncLogThreadPoolSize;
		private int replicationThreadPoolSize;

		public void setInsertSyncLogThreadPoolSize(int threadPoolSize) {
			this.insertSyncLogThreadPoolSize = threadPoolSize;
		}

		public void setReplicationThreadPoolSize(int threadPoolSize) {
			this.replicationThreadPoolSize = threadPoolSize;
		}

		public int getInsertSyncLogThreadPoolSize() {
			return insertSyncLogThreadPoolSize;
		}

		public int getReplicationThreadPoolSize() {
			return replicationThreadPoolSize;
		}
	}
}
