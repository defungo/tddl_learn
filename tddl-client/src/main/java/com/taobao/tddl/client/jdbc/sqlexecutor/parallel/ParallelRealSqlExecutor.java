package com.taobao.tddl.client.jdbc.sqlexecutor.parallel;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.client.jdbc.ConnectionManager;
import com.taobao.tddl.client.jdbc.RealSqlContext;
import com.taobao.tddl.client.jdbc.TStatementImp;
import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlan;
import com.taobao.tddl.client.jdbc.sqlexecutor.QueryReturn;
import com.taobao.tddl.client.jdbc.sqlexecutor.RealSqlExecutorCommon;
import com.taobao.tddl.client.jdbc.sqlexecutor.UpdateReturn;

/**
 * ����sqlִ�����������̳߳�ģʽ����ִ�в�ͬ��sql,�Ӷ�
 * ����������ݿ�IO
 * <br>
 * ʹ�ûص���������õ�������У����ĳ���߳�ִ�з����쳣��
 * �ж����̣߳�ʹ���߳��˳�countdownlatch.await(),����
 * ���쳣����ȡ���˴����������sqlִ�� ,���������������
 * ��ִ����Ϸ��صĽ��,�ر����ӡ�������߼���Ҫ�Ż���
 * ȡ��������ʼ�վ����з��գ����Ӹ����ԣ�
 * 
 * @author junyu
 * 
 */
public class ParallelRealSqlExecutor extends RealSqlExecutorCommon {
	private static final Log logger = LogFactory
			.getLog(ParallelRealSqlExecutor.class);

	public ParallelRealSqlExecutor(ConnectionManager connectionManager) {
		super(connectionManager);
	}

	/**
	 * ���в�ѯ��ʹ�ù̶��̳߳ؽ��в�ѯ������
	 * 
	 * @param queryReturnList
	 * @param executionPlan
	 * @param tStatementImp
	 * @param latch
	 */
	@SuppressWarnings("rawtypes")
	public void parallelQuery(
			ConcurrentLinkedQueue<QueryReturn> queryReturnQueue,
			final ExecutionPlan executionPlan, TStatementImp tStatementImp,
			final CountDownLatch latch, List<Future> futures) {

		setSpecialProperty(tStatementImp, executionPlan);
		
		/**
		 * �����ص���
		 */
		final ExecuteCompleteListener<QueryReturn> ec = new ExecuteCompleteListener<QueryReturn>(
				queryReturnQueue);

		final boolean isPrepareStatement = this
				.isPreparedStatement(tStatementImp);

		final Thread mainThread = Thread.currentThread();
		Map<String, List<RealSqlContext>> sqlMap = executionPlan.getSqlMap();
		for (final Entry<String, List<RealSqlContext>> dbEntry : sqlMap
				.entrySet()) {
			Future future = null;
			futures.add(future);
			future = ParallelDiamondConfigManager.submit(dbEntry.getKey(),
					new Runnable() {
						public void run() {
							String dbSelectorId = dbEntry.getKey();
							
							try {
								/**
								 * ��һ�β�ѯ֮ǰ������µ�ǰ�߳���û�б���Ϊ interrupted
								 */
								checkThreadState();

								Connection connection = connectionManager
										.getConnection(dbSelectorId,
												executionPlan.isGoSlave());

								List<RealSqlContext> sqlList = dbEntry
										.getValue();

								for (RealSqlContext sql : sqlList) {
									QueryReturn qr = null;

									/**
									 * ÿһ�β�ѯ֮ǰ������µ�ǰ�߳���û�б���Ϊ interrupted
									 */
									checkThreadState();

									long start = System.currentTimeMillis();

									if (isPrepareStatement) {
										qr = executeQueryIntervalPST(
												connection, sql);
									} else {
										qr = executeQueryIntervalST(connection,
												sql);
									}

									long during = System.currentTimeMillis()
											- start;
									profileRealDatabaseAndTables(dbSelectorId,
											sql, during);
									
                                    qr.setCurrentDBIndex(dbSelectorId);
									ec.addResult(qr);
								}
							} catch (SQLException e) {
								logger.error(
										"Parallel Query SQLException Happen!",
										e);
						        
								// ��countdownlatch��ӦinterruptException;
								if (!mainThread.isInterrupted()) {
									mainThread.interrupt();
								}
							} catch(Exception e){
								logger.error(
										"Parallel Query Unknow Exception Happen!",
										e);
								
								// ��countdownlatch��ӦinterruptException;
								if (!mainThread.isInterrupted()) {
									mainThread.interrupt();
								}
							}

							/**
							 * ֪ͨ���߳���ɲ�ѯ
							 */
							latch.countDown();
						}
					});

		}
	}
	
	public void tryCloseConnection(String dbIndex){
		tryCloseConnection(null,dbIndex);
	}

	/**
	 * ���и��£�ʹ�ù̶��̳߳ؽ���update����
	 * 
	 * @param updateReturnList
	 * @param executionPlan
	 * @param tStatementImp
	 * @param latch
	 */
	@SuppressWarnings("rawtypes")
	public void parallelUpdate(
			ConcurrentLinkedQueue<UpdateReturn> updateReturnQueue,
			final ExecutionPlan executionPlan,
			final TStatementImp tStatementImp, final CountDownLatch latch,
			List<Future> futures) {

		setSpecialProperty(tStatementImp, executionPlan);
		
		/**
		 * �����ص���
		 */
		final ExecuteCompleteListener<UpdateReturn> ec = new ExecuteCompleteListener<UpdateReturn>(
				updateReturnQueue);

		final boolean isPrepareStatement = this
				.isPreparedStatement(tStatementImp);

		final Thread mainThread = Thread.currentThread();
		Map<String, List<RealSqlContext>> sqlMap = executionPlan.getSqlMap();
		for (final Entry<String, List<RealSqlContext>> dbEntry : sqlMap
				.entrySet()) {
			Future future = null;
			futures.add(future);
			future = ParallelDiamondConfigManager.submit(dbEntry.getKey(),
					new Runnable() {
						public void run() {
							UpdateReturn ur = null;
							if (isPrepareStatement) {
								ur = executeUpdateIntervalPST(executionPlan,
										dbEntry);
							} else {
								ur = executeUpdateIntervalST(executionPlan,
										dbEntry);
							}

							if (null != ur.getExceptions()
									&& !ur.getExceptions().isEmpty()) {
								logger
										.error("Parallel Update SQLException Happen!");

								if (!mainThread.isInterrupted()) {
									mainThread.interrupt();
								}
							}
							
							ec.addResult(ur);

							/**
							 * ֪ͨ���̸߳��²������
							 */
							latch.countDown();
						}
					});
		}
	}

	/**
	 * �ڲ��ص��࣬�����߳��������ʱ��ɽ������
	 * 
	 * @author junyu
	 * 
	 * @param <T>
	 */
	private class ExecuteCompleteListener<T> {
		private ConcurrentLinkedQueue<T> re = null;

		public ExecuteCompleteListener(ConcurrentLinkedQueue<T> re) {
			this.re = re;
		}

		public void addResult(T ele) {
			re.add(ele);
		}
	}
}
