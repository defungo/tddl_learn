package com.taobao.tddl.parser;

import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import com.taobao.tddl.common.util.GoogleConcurrentLruCache;
import com.taobao.tddl.interact.rule.bean.SqlType;
import com.taobao.tddl.sqlobjecttree.DMLCommon;

/*
 * @author guangxia
 * @since 1.0, 2009-9-15 ����10:37:20
 */
public class ParserCache {
	private static final ParserCache instance = new ParserCache();
	public final int capacity;
//	private final Map<String, ItemValue> map;
	private final GoogleConcurrentLruCache<String, ItemValue> map;

	private ParserCache() {
		int size = 389;
		String propSize = System.getProperty("com.taobao.tddl.parser.cachesize");
		if (propSize != null) {
			try {
				size = Integer.parseInt(propSize);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		capacity = size;
//		map = new BoundedConcurrentHashMap<String, ItemValue>(capacity);
		map = new GoogleConcurrentLruCache<String, ItemValue>(capacity);
	}

	public static final ParserCache instance() {
		return instance;
	}

	private final ReentrantLock lock = new ReentrantLock();

	public int size() {
		return map.size();
	}

	protected static class ItemValue {

		/**
		 * ���ݵ�CRUD����
		 */
		private AtomicReference<SqlType> sqlType = new AtomicReference<SqlType>();

		/**
		 * ��ȥvirtualTableName���������sql�ֶ�
		 */
		private AtomicReference<List<Object>> tableNameReplacement = new AtomicReference<List<Object>>();

		/**
		 * ���������sql
		 */
		private AtomicReference<FutureTask<DMLCommon>> futureDMLCommon = new AtomicReference<FutureTask<DMLCommon>>();

		public SqlType getSqlType() {
			return sqlType.get();
		}

		public SqlType setSqlTypeIfAbsent(SqlType sqlTypeinput) {
			//���ԭֵΪnull���ԭ�ӵ�������ֵ��ȥ�����ҷ�����ֵ
			if (sqlType.compareAndSet(null, sqlTypeinput)) {
				return sqlTypeinput;
			} else {
				//��������ֵ�Ѿ���Ϊnull�����ȡ��ֵ
				return sqlType.get();
			}
		}

		public List<Object> getTableNameReplacement() {
			return tableNameReplacement.get();
		}

		public List<Object> setTableNameReplacementIfAbsent(List<Object> tableNameReplacementList) {
			//���ԭֵΪnull���ԭ�ӵ�������ֵ��ȥ�����ҷ�����ֵ
			if (tableNameReplacement.compareAndSet(null, tableNameReplacementList)) {
				return tableNameReplacementList;
			} else {
				//��������ֵ�Ѿ���Ϊnull�����ȡ��ֵ
				return tableNameReplacement.get();
			}

		}

		public FutureTask<DMLCommon> getFutureDMLCommon() {
			return futureDMLCommon.get();
		}

		public FutureTask<DMLCommon> setFutureDMLCommonIfAbsent(FutureTask<DMLCommon> future) {
			//���ԭֵΪnull���ԭ�ӵ�������ֵ��ȥ�����ҷ�����ֵ
			if (futureDMLCommon.compareAndSet(null, future)) {
				return future;
			} else {
				//��������ֵ�Ѿ���Ϊnull�����ȡ��ֵ
				return futureDMLCommon.get();
			}
		}

	}

	protected ItemValue get(String sql) {
		return map.get(sql);
	}

	public SqlType getSqlType(String sql) {
		ItemValue itemValue = get(sql);
		if (itemValue != null) {
			return itemValue.getSqlType();
		} else {
			return null;
		}
	}

	public SqlType setSqlTypeIfAbsent(String sql, SqlType sqlType) {
		ItemValue itemValue = get(sql);
		SqlType returnSqlType = null;
		if (itemValue == null) {
			//��ȫû�е����������������£��϶�����Ϊ��û���ֳ����������ĳ��sql
			lock.lock();
			try {
				// ˫���lock
				itemValue = get(sql);
				if (itemValue == null) {

					itemValue = new ParserCache.ItemValue();

					put(sql, itemValue);
				}
			} finally {

				lock.unlock();
			}
			//cas ����ItemValue�е�SqlType����
			returnSqlType = itemValue.setSqlTypeIfAbsent(sqlType);

		} else if (itemValue.getSqlType()== null) {
			//cas ����ItemValue�е�SqlType����
			returnSqlType = itemValue.setSqlTypeIfAbsent(sqlType);

		} else {
			returnSqlType = itemValue.getSqlType();
		}

		return returnSqlType;
	}

	public FutureTask<DMLCommon> getFutureTask(String sql) {
		ItemValue itemValue = get(sql);
		if (itemValue != null) {
			return itemValue.getFutureDMLCommon();
		} else {
			return null;
		}

	}

	public List<Object> getTableNameReplacement(String sql) {
		ItemValue itemValue = get(sql);
		if (itemValue != null) {
			return itemValue.getTableNameReplacement();
		} else {
			return null;
		}
	}

	public List<Object> setTableNameReplacementIfAbsent(String sql, List<Object> tablenameReplacement) {
		ItemValue itemValue = get(sql);
		List<Object> returnList = null;
		if (itemValue == null) {
			//��ȫû�е����������������£��϶�����Ϊ��û���ֳ����������ĳ��sql
			lock.lock();
			try {
				// ˫���lock
				itemValue = get(sql);
				if (itemValue == null) {

					itemValue = new ParserCache.ItemValue();

					put(sql, itemValue);
				}
			} finally {

				lock.unlock();
			}
			//cas ����ItemValue�е�TableNameReplacement����
			returnList = itemValue.setTableNameReplacementIfAbsent(tablenameReplacement);

		} else if (itemValue.getTableNameReplacement() == null) {
			//cas ����ItemValue�е�TableNameReplacement����
			returnList = itemValue.setTableNameReplacementIfAbsent(tablenameReplacement);

		} else {
			returnList = itemValue.getTableNameReplacement();
		}

		return returnList;

	}

	public FutureTask<DMLCommon> setFutureTaskIfAbsent(String sql, FutureTask<DMLCommon> future) {
		ItemValue itemValue = get(sql);
		FutureTask<DMLCommon> returnFutureTask = null;
		if (itemValue == null) {
			//��ȫû�е����������������£��϶�����Ϊ��û���ֳ����������ĳ��sql
			lock.lock();
			try {
				// ˫���lock
				itemValue = get(sql);
				if (itemValue == null) {

					itemValue = new ParserCache.ItemValue();

					put(sql, itemValue);
				}
			} finally {

				lock.unlock();
			}
			//cas ����ItemValue�е�DMLCommon����
			returnFutureTask = itemValue.setFutureDMLCommonIfAbsent(future);

		} else if (itemValue.getFutureDMLCommon() == null) {
			//cas ����ItemValue�е�DMLCommon����
			returnFutureTask = itemValue.setFutureDMLCommonIfAbsent(future);
		} else {
			returnFutureTask = itemValue.getFutureDMLCommon();
		}

		return returnFutureTask;

	}

	protected void put(String sql, ItemValue itemValue) {
		map.put(sql, itemValue);
	}
}
