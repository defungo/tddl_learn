package com.taobao.tddl.client.jdbc.resultset.newImp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.taobao.tddl.client.jdbc.ConnectionManager;
import com.taobao.tddl.client.jdbc.TStatementImp;
import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlan;
import com.taobao.tddl.client.jdbc.resultset.helper.ComparatorRealizer;
import com.taobao.tddl.client.jdbc.sqlexecutor.RealSqlExecutor;
/**
 * ��ʱ�������ResultSet,�������������ܷ���
 * 
 * @author junyu
 *
 */
public class DistinctTResultSet extends BaseTResultSet {
	// ֻ֧�ֵ���distinct
	private List<String> distinctColumns;
	// �Ѿ���������,���ڴ�����ķ���
	private List<List<Object>> result = new ArrayList<List<Object>>(
			this.maxSize);
	//��ֹ�ڴ汬��
	private int maxSize = 100000;
	// �Ƚ���
	private Map<String, Comparator<Object>> compMap = null;
	// �ж�������Ƿ�ȡ��
	private int rsIndex = 0;

	public DistinctTResultSet(TStatementImp tStatementImp,
			ConnectionManager connectionManager, ExecutionPlan executionPlan,
			RealSqlExecutor realSqlExecutor) throws SQLException {
		super(tStatementImp, connectionManager, executionPlan, realSqlExecutor);
	}

	public DistinctTResultSet(TStatementImp tStatementImp,
			ConnectionManager connectionManager, ExecutionPlan executionPlan,
			RealSqlExecutor realSqlExecutor, boolean init) throws SQLException {
		super(tStatementImp, connectionManager, executionPlan, realSqlExecutor,
				init);
	}

	@Override
	protected boolean internNext() throws SQLException {
		ResultSet rs = actualResultSets.get(rsIndex);
		super.currentResultSet = rs;
		if (rs.next()) {
			if (null == compMap) {
				compMap = new HashMap<String, Comparator<Object>>();
				for (String distinctColumn : distinctColumns) {
					Object obj = rs.getObject(distinctColumn);
					Class<?> sortType = obj.getClass();
					compMap.put(distinctColumn,
							ComparatorRealizer.getObjectComparator(sortType));
				}
			}

			boolean inPreSearch = true;
			List<Object> record = new ArrayList<Object>(distinctColumns.size());
			// һ���ֶ�һ���ֶαȽ�,����DISTINCT����,DISTINCT�����ֶ�ֵ��ͬ,��ô����ͬ
			for (int i = 0; i < distinctColumns.size(); i++) {
				Object obj = rs.getObject(distinctColumns.get(i));
				if (!this.contains(i, distinctColumns.get(i), obj)) {
					inPreSearch = false;
				}
				record.add(obj);
			}

			if (!inPreSearch) {
				if (result.size() < maxSize) {
					result.add(record);
				} else {
					throw new SQLException("[DISTINCT]��ѯ��������������������ֵ"+maxSize+",��ѯʧ��!");
				}
				return true;
			} else {
				return internNext();
			}
		} else {
			rsIndex++;
			if (actualResultSets.size() < (rsIndex + 1)) {
				return false;
			}

			return internNext();
		}
	}

	/**
	 * �ж���ǰ�����������Ƿ�������Ѽ���������,����ظ� ��ֱ��nextȡ��һ��.ֱ��ȡ��
	 * 
	 * @param obj
	 * @return
	 */
	private boolean contains(int index, String distinctColumn, Object obj) {
		Comparator<Object> comp = compMap.get(distinctColumn);
		for (List<Object> re : result) {
			// һ��������ͬ��ֱ�ӷ���true
			if (comp.compare(re.get(index), obj) == 0) {
				return true;
			}
		}
		return false;
	}

	public void setDistinctColumn(List<String> distinctColumns) {
		this.distinctColumns = distinctColumns;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
		this.result = new ArrayList<List<Object>>(this.maxSize);
	}
}
