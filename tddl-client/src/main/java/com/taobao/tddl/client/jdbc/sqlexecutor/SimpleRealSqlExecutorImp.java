package com.taobao.tddl.client.jdbc.sqlexecutor;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.taobao.tddl.client.jdbc.RealSqlContext;
import com.taobao.tddl.client.jdbc.TStatementImp;
import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlan;
import com.taobao.tddl.client.jdbc.sqlexecutor.parallel.ParallelRealSqlExecutor;
import com.taobao.tddl.client.jdbc.sqlexecutor.serial.SerialRealSqlExecutor;

/**
 * @author junyu
 * 
 */
public class SimpleRealSqlExecutorImp extends RealSqlExecutorImp {
	public SimpleRealSqlExecutorImp(ParallelRealSqlExecutor parallelExecutor,
			SerialRealSqlExecutor serialExecutor, TStatementImp tStatementImp,
			ExecutionPlan executionPlan) {
		super(parallelExecutor, serialExecutor, tStatementImp, executionPlan);
	}

	private boolean alwaysUseParallel = false;

	@Override
	public QueryReturn query() throws SQLException {
		// ˵����һ�ν���������������;�߳����ѯ
		if (null == queryReturnQueue && useParallel()) {
			alwaysUseParallel = true;
			QueryReturn qr = super.query();
			queryConnectionManage(qr);
			return qr;
		}

		// ����߲��У���ôһֱ�߲��У��л�Ϊ���к���β�ѯҲ�����겢�С�
		if (alwaysUseParallel) {
			QueryReturn qr = super.query();
			queryConnectionManage(qr);
			return qr;
		}

		if (null == queryReturnQueue) {
			queryReturnQueue = new ConcurrentLinkedQueue<QueryReturn>();
		}

		serialExecutor.serialQuery(queryReturnQueue, executionPlan,
				tStatementImp);

		if (queryReturnQueue != null && !queryReturnQueue.isEmpty()) {
			return queryReturnQueue.poll();
		} else {
			return null;
		}
	}

	private String needCloseConection = null;
	Map<String, Integer> conWithStatements = null;

	/**
	 * ʹ�ò��з�ʽ��������һ������һ������Ҫ�������ӹر����⡣
	 * 
	 * @param qr
	 */
	private void queryConnectionManage(QueryReturn qr) {
		if (null != needCloseConection) {
			// ����ʹ��
			// logger.debug("--------needCloseConection-------"+needCloseConection);
			// logger.debug("--------needCloseConection statement num-------"+conWithStatements.get(needCloseConection));
			parallelExecutor.tryCloseConnection(needCloseConection);
			needCloseConection = null;
		}

		if (null != qr) {
			if (null == conWithStatements) {
				conWithStatements = new HashMap<String, Integer>();
				Map<String, List<RealSqlContext>> map = executionPlan
						.getSqlMap();
				for (Map.Entry<String, List<RealSqlContext>> entry : map
						.entrySet()) {
					conWithStatements.put(entry.getKey(), entry.getValue()
							.size());
				}
			}

			// �����ǰds resultset����-1��С�ڵ���0,��ǰds resultsetȡ��
			int remain = conWithStatements.get(qr.getCurrentDBIndex()) - 1;
			conWithStatements.put(qr.getCurrentDBIndex(), remain);
			if (remain <= 0) {
				needCloseConection = qr.getCurrentDBIndex();
			}
		}
	}
}
