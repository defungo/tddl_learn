package com.taobao.tddl.client.jdbc.sqlexecutor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.client.jdbc.RealSqlContext;
import com.taobao.tddl.client.jdbc.TStatementImp;
import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlan;
import com.taobao.tddl.client.jdbc.sqlexecutor.parallel.ParallelDiamondConfigManager;
import com.taobao.tddl.client.jdbc.sqlexecutor.parallel.ParallelRealSqlExecutor;
import com.taobao.tddl.client.jdbc.sqlexecutor.serial.SerialRealSqlExecutor;

/**
 * �����SQLִ������ÿ�������ѯ���߸��¶���ʵ����
 * 
 * @author junyu
 * 
 */
public class RealSqlExecutorImp implements RealSqlExecutor {
	private static final Log logger = LogFactory
			.getLog(RealSqlExecutorImp.class);
	protected final ParallelRealSqlExecutor parallelExecutor;
	protected final SerialRealSqlExecutor serialExecutor;
	protected final TStatementImp tStatementImp;
	protected final ExecutionPlan executionPlan;

	protected ConcurrentLinkedQueue<UpdateReturn> updateReturnQueue = null;
	protected ConcurrentLinkedQueue<QueryReturn> queryReturnQueue = null;

	public RealSqlExecutorImp(ParallelRealSqlExecutor parallelExecutor,
			SerialRealSqlExecutor serialExecutor, TStatementImp tStatementImp,
			ExecutionPlan executionPlan) {
		this.parallelExecutor = parallelExecutor;
		this.serialExecutor = serialExecutor;
		this.tStatementImp = tStatementImp;
		this.executionPlan = executionPlan;
	}

	@SuppressWarnings("rawtypes")
	public QueryReturn query() throws SQLException {
		if (null == queryReturnQueue) {
			queryReturnQueue = new ConcurrentLinkedQueue<QueryReturn>();
			boolean useParallel = useParallel();
			if (useParallel && null != this.parallelExecutor) {
				int dbSize = getDbSize(executionPlan);
				CountDownLatch cdl = new CountDownLatch(dbSize);
				List<Future> futures = new ArrayList<Future>(dbSize);
				try {
					parallelExecutor.parallelQuery(queryReturnQueue,
							executionPlan, tStatementImp, cdl, futures);
					cdl.await();
				} catch (RejectedExecutionException e1) {
					logger.error("some task rejected,this query failed!", e1);
					parallelQueryExceptionHandle(futures);
					throw new SQLException("[RealSqlExecutorImp exception caught]some task rejected,this query failed!");
				} catch (InterruptedException e) {
					logger.error("parrallel query error!", e);
					parallelQueryExceptionHandle(futures);
					throw new SQLException("[RealSqlExecutorImp exception caught]parrallel query error!");
				} catch (Exception e) {
					logger.error("some error happen!", e);
					parallelQueryExceptionHandle(futures);
					throw new SQLException("[RealSqlExecutorImp exception caught]unknow error happen!");
				}
			}

			if (!useParallel && null != this.serialExecutor) {
				serialExecutor.serialQuery(queryReturnQueue, executionPlan,
						tStatementImp);
			}
		}

		if (null != queryReturnQueue && !queryReturnQueue.isEmpty()) {
			QueryReturn qr = queryReturnQueue.poll();
			return qr;
		} else {
			return null;
		}
	}

	@SuppressWarnings("rawtypes")
	public UpdateReturn update() throws SQLException {
		if (null == updateReturnQueue) {
			boolean useParallel = useParallel();
			updateReturnQueue = new ConcurrentLinkedQueue<UpdateReturn>();

			if (useParallel && null != this.parallelExecutor) {
				int dbSize = getDbSize(executionPlan);
				CountDownLatch cdl = new CountDownLatch(dbSize);
				List<Future> futures = new ArrayList<Future>(dbSize);
				try {
					parallelExecutor.parallelUpdate(updateReturnQueue,
							executionPlan, tStatementImp, cdl, futures);
					cdl.await();
				} catch (RejectedExecutionException e1) {
					logger.error("some task rejected,this query failed!", e1);
					parallelUpdateExceptionHandle(futures);
					throw new SQLException("[RealSqlExecutorImp exception caught]some task rejected,this update failed!");
				} catch (InterruptedException e) {
					logger.error("parrallel update error!", e);
					parallelUpdateExceptionHandle(futures);
					throw new SQLException("[RealSqlExecutorImp exception caught]parrallel update error!");
				} catch (Exception e) {
					logger.error("main thread some error happen!", e);
					parallelUpdateExceptionHandle(futures);
					throw new SQLException("[RealSqlExecutorImp exception caught]unknow error happen!");
				}
			}

			if (!useParallel && null != this.serialExecutor) {
				serialExecutor.serialUpdate(updateReturnQueue, executionPlan,
						tStatementImp);
			}
		}

		if (null != updateReturnQueue && !updateReturnQueue.isEmpty()) {
			UpdateReturn ur = updateReturnQueue.poll();
			return ur;
		} else {
			return null;
		}
	}

	/**
	 * ȷ���Ƿ�ʹ�ò��в�ѯ
	 * 
	 * @param executionPlan
	 * @return
	 */
	protected boolean useParallel() {
		if (!ParallelDiamondConfigManager.isUseParallel()) {
			return false;
		} else if (executionPlan.isUseParallel()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * ȡ����Ҫ����������ִ��
	 * 
	 * @param executionPlan
	 * @return
	 */
	private int getDbSize(ExecutionPlan executionPlan) {
		Map<String, List<RealSqlContext>> sqlMap = executionPlan.getSqlMap();
		return sqlMap.size();
	}

	/**
	 * ��ҪΪ�˷�ֹ��ѯ��һ��ֱ�ӷ�����ѯ
	 * ��������쳣ʱΪ��ȷ����ȫ��Ҳ��Ҫ����
	 * ����������Ӷ�ȷ����ȷ�Ĺر�����Դ
	 * 
	 * @throws SQLException
	 */
	public void clearQueryResource(){
		if (null != this.queryReturnQueue && !this.queryReturnQueue.isEmpty()) {
			QueryReturn qr = null;
			//ÿ��try catch
			while (null != (qr = queryReturnQueue.poll())) {
				if (qr.getResultset() != null) {
					try {
						qr.getResultset().close();
//						//������
//						logger.info(Thread.currentThread()+"resultset close success!");
					} catch (SQLException e) {
						logger.error("resultset close error!",e);
					}
				}

				if (qr.getStatement() != null) {
					try {
						qr.getStatement().close();
//						//������
//						logger.info(Thread.currentThread()+"statement close success!");
					} catch (SQLException e) {
						logger.error("statement close error!",e);
					}
				}

				if (qr.getCurrentDBIndex() != null) {
					// �����������ĸ�����OK�ģ���Ϊ���ߵ�connectionManagerһ��
//					//������
//					logger.info(Thread.currentThread()+"try close connection!");
					parallelExecutor.tryCloseConnection(qr.getCurrentDBIndex());
				}
			}
		}
	}

	/**
	 * ���в�ѯ������Դ
	 * 
	 * @param futures
	 * @throws SQLException
	 */
	@SuppressWarnings("rawtypes")
	private void parallelQueryExceptionHandle(List<Future> futures)
			throws SQLException {
		logger.warn("start to cancel all future!");
		for (Future future : futures) {
			if (null != future) {
				future.cancel(true);
			}
		}
        
		logger.warn("start to collect query resources!");
		clearQueryResource();
	}

	/**
	 * ���и��»�����Դ
	 * 
	 * @param futures
	 * @throws SQLException
	 */
	@SuppressWarnings("rawtypes")
	private void parallelUpdateExceptionHandle(List<Future> futures)
			throws SQLException {
		logger.warn("start to cancel all future!");
		for (Future future : futures) {
			if (null != future) {
				future.cancel(true);
			}
		}
	}
}
