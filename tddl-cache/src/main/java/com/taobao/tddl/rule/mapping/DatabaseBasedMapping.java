package com.taobao.tddl.rule.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

/**
 * ���ݿ�Ϊ���е�ӳ����� ������</br> <blockquote>
 * 
 * <pre>
 * TargetColumn      SourceColumn 
 * ------------ <---  --------------- 
 *    ?              SourceValue
 * </pre>
 * 
 * </blockquote>
 * 
 * sourceColumn �Ǵ�advancedParameters���á�</br> sourceValue �Ǵ�sql�л�� targetColumn
 * �Ǵ�targetRule�з������ ��?
 * 
 * @author shenxun
 * 
 */
public class DatabaseBasedMapping {
	static final Log logger = LogFactory.getLog(DatabaseBasedMapping.class);
	Map<String/* target key */, TypeHandlerEntry> typeHandlerMap;

	public enum TARGET_VALUE_TYPE {
		INTEGER, LONG, STRING;
	}

	private String sourceColumn;

	protected String[] columns;

	private final Log log = LogFactory.getLog(DatabaseBasedMapping.class);
	/**
	 * datasource
	 */
	private DataSource routeDatasource;
	/**
	 * ���� select column from [tableName] where ...
	 */
	private String routeTable;

	/**
	 * jdbc��װ
	 */
	private JdbcTemplate jdbcTemplate = null;

	/**
	 * ����sourceValueȥ��ѯ·�ɱ�����·�ɱ�������ΪsourceKey���е�ֵ��sourceValue��ȵļ�¼,
	 * ����¼������ΪtargetKey����ֵ����
	 * 
	 * @param targetKey
	 *            ָ��Ҫ��ȡ����·�ɱ��е���һ��
	 * @param sourceKey
	 *            ָ����·�ɱ��е���һ����Ϊkey��
	 * @param sourceValue
	 *            ԭʼֵ��
	 * @return
	 */
	protected Object get(String targetKey, String sourceKey, Object sourceValue) {

		Object value = getFromDatabase(routeTable, targetKey, sourceKey,
				sourceValue);

		return value;
	}

	/**
	 * ����sourceValueȥ��ѯ·�ɱ�·�ɱ�����Ϊkey������sourceValue�Ƚϣ��õ�������¼,
	 * ����¼������ΪtargetKey����ֵ����
	 * 
	 * @param targetKey
	 *            ָ��Ҫ��ȡ����·�ɱ��е���һ��
	 * @param sourceValue
	 *            ԭʼֵ��
	 * @return
	 */
	public Object get(String targetKey, Object sourceValue) {
		if (sourceColumn == null) {
			throw new IllegalArgumentException(
					"sourceColumn should not be null;");
		}
		return get(targetKey, sourceColumn, sourceValue);
	}

	protected String getColumns() {
		if (columns == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		boolean firstElement = true;
		for (String col : columns) {
			if (firstElement) {
				firstElement = false;
				sb.append(col);
			} else {
				sb.append(",");
				sb.append(col);
			}
		}
		return sb.toString();
	}

	/**
	 * ӳ����ֵ
	 * 
	 * @param namespace
	 * @param key
	 * @return
	 */
	protected Object getFromDatabase(String routeTable,
			final String targetColumn, final String originalColumn,
			final Object originalValue) {
		// ��ȡselect sql
		Object resultValue = null;
		resultValue = null;
		Map<String, Object> target = getResultMap(originalColumn,
				originalValue, targetColumn);
		resultValue = target.get(targetColumn);
		return resultValue;
	}

	private void throwRuntimeSqlExceptionWrapper(DataAccessException e) {
		// modified by
		// shenxun:ԭ���Ϲ��������ǲ�Ӧ���׳�sqlException�ġ�����������fix���׳����ض��쳣��־��sqlException
		// ��getExecutionContext��ʱ��Ӧ�����Բ��񲢻�ΪcomminuticationException�׳���
		Throwable throwable = e.getCause();
		if (throwable instanceof SQLException) {
			throw new RuntimeException((SQLException) throwable);
		} else {
			log.error("��SQLException ������Ԥ��,", e);
		}
	}

	/*
	 * private Object[] getInsertSql(String tableName, Map<String, Object>
	 * values, StringBuilder sb) { StringBuilder columnBuilder = new
	 * StringBuilder(); StringBuilder valuesBuilder = new StringBuilder();
	 * Object[] args = new Object[values.size()]; int index = 0; boolean
	 * isFirstElement = true; for (Entry<String, Object> entry :
	 * values.entrySet()) { if (!isFirstElement) { columnBuilder.append(",");
	 * valuesBuilder.append(","); } isFirstElement = false;
	 * columnBuilder.append(entry.getKey()); valuesBuilder.append("?");
	 * args[index] = entry.getValue(); index++; }
	 * sb.append("insert into ").append(tableName).append(" (").append(
	 * columnBuilder.toString()).append(") values (").append(
	 * valuesBuilder.toString()).append(")"); return args; }
	 */

	@SuppressWarnings("unchecked")
	protected Map<String, Object> getResultMap(final String originalColumn,
			final Object originalValue, String targetColumn) {
		String sql = getSelectKeySql(routeTable, originalColumn, targetColumn);
		Object[] args = new Object[] { originalValue };
		Map<String, Object> target = null;
		try {
			target = (Map<String, Object>) jdbcTemplate.query(sql, args,
					new ResultSetExtractor() {
						public Object extractData(ResultSet rs)
								throws SQLException, DataAccessException {

							Map<String, Object> value = new HashMap<String, Object>(
									columns.length);
							if (rs.next()) {
								int i = 1;
								for (String col : columns) {
									Object obj = rs.getObject(i);
									TypeHandlerEntry typeHandlerEntry =	typeHandlerMap.get(col);
									Object requestValue = typeHandlerEntry.typeHandler.getRequestValue(obj);
									value.put(col, requestValue);
									i++;
								}
							}
							return value;
						}
					});
		} catch (DataAccessException e) {
			throwRuntimeSqlExceptionWrapper(e);
		}

		return target;
	}

	public String getRouteTable() {
		return routeTable;
	}

	/**
	 * @param tableName
	 * @param originalColumn
	 * @param targetColumn
	 * @return
	 */
	String getSelectKeySql(String tableName, String originalColumn,
			String targetColumn) {

		StringBuilder sb = new StringBuilder();
		String columns = getColumns();
		sb.append("select ").append(columns).append(" from ").append(tableName)
				.append(" where ").append(originalColumn).append(" = ?");
		String sql = sb.toString();
		return sql;
	}

	public DataSource getRouteDatasource() {
		return routeDatasource;
	}

	public void initInternal() {
		if (routeDatasource == null) {
			throw new IllegalArgumentException("δָ��datasource");
		}
		if (columns == null) {
			throw new IllegalArgumentException("δָ��columns");
		}

		if (this.routeTable == null) {
			throw new IllegalArgumentException("δָ��routeTable");
		}
		log.debug("put ds to jdbc template");
		jdbcTemplate = new JdbcTemplate(routeDatasource);

	}

	/*
	 * protected int put(Map<String, Object> values) { StringBuilder
	 * insertSqlStringBuilder = new StringBuilder(); Object[] args =
	 * getInsertSql(routeTable, values, insertSqlStringBuilder); return
	 * jdbcTemplate.update(insertSqlStringBuilder.toString(), args);
	 * 
	 * }
	 */

	/**
	 * ����sql�� select [columns] from table
	 * ��ֻ��databaseMapping��ʱ����Բ��ã���tair+database Mapping �б�����
	 * 
	 * @param columns
	 */
	public void setColumns(String columns) {
		if (columns == null) {
			throw new IllegalArgumentException("columns is null");
		}

		String[] columnsArray = columns.split(",");
		List<String> cols = new ArrayList<String>(columnsArray.length);
		int index = 0;
		typeHandlerMap = new HashMap<String, TypeHandlerEntry>(
				columnsArray.length);
		for (String col : columnsArray) {
			// ����column|type
			String[] columnsAndType = col.split("\\|");
			if (columnsAndType.length != 2) {
				throw new IllegalArgumentException("һ��column ������Ӧ��type����Ϊ��������");
			}
			// ���columns��columns List��
			cols.add(columnsAndType[0]);

			String type = columnsAndType[1];
			if (type == null || type.equals("")) {
				throw new IllegalArgumentException("type ����null");
			}
			type = type.toLowerCase();
			TypeHandlerEntry entry = new TypeHandlerEntry();
			// type������ ��Ϊ��init��ʱ�����Բ��õ���Ч��
			if ("int".equals(type) || "integer".equals(type)) {

				entry.typeHandler = new IntegerTypeHandler();
			} else if ("long".equals(type)) {
				entry.typeHandler = new LongTypeHandler();
			} else if ("string".equals(type) || "str".equals(type)) {
				entry.typeHandler = new StringTypeHandler();
			} else {
				throw new IllegalArgumentException("unknow type handler");
			}
			entry.index = index;
			typeHandlerMap.put(columnsAndType[0], entry);

			index++;
		}

		// type������������ɺ󣬻���Ҫ��parent��columns��ĿҲ��� �������ܲ鵽��������
		this.columns = cols.toArray(new String[cols.size()]);

	}

	public void setRouteTable(String routeTable) {
		this.routeTable = routeTable;
	}

	public void setRouteDatasource(DataSource routeDatasource) {
		this.routeDatasource = routeDatasource;
	}

	public String getSourceColumn() {
		return sourceColumn;
	}

	public void setSourceColumn(String sourceColumn) {
		this.sourceColumn = sourceColumn;
	}

	public static class LongTypeHandler implements TypeHandler {
		public Object process(String value) {
			if (value == null) {
				return null;
			}
			return Long.valueOf(value);
		}

		public Object getRequestValue(Object source) {
			if (source instanceof String) {
				return Integer.valueOf((String) source);
			} else if (source instanceof Number) {
				return ((Number) source).longValue();
			}
			logger.warn("��֧�ֵ�ǰֵת�� �� ��ǰֵ ��" + source
					+ " type : " + source.getClass());
			return source;
		}
	}

	/**
	 * tairҪ��ʹ�ü򵥶��������tair���ŵ�ȫ������String����ֻ��ȡ����ʱ�����ת��
	 * 
	 * @author shenxun
	 * 
	 */
	static interface TypeHandler {
		Object process(String value);

		Object getRequestValue(Object source);
	}

	public static class StringTypeHandler implements TypeHandler {
		public Object process(String value) {
			return value;
		}

		public Object getRequestValue(Object source) {
			return String.valueOf(source);
		}
	}

	public static class IntegerTypeHandler implements TypeHandler {
		public Object process(String value) {
			if (value == null) {
				return null;
			}
			return Integer.valueOf(value);
		}

		public Object getRequestValue(Object source) {
			if (source instanceof String) {
				return Integer.valueOf((String) source);
			} else if (source instanceof Number) {
				return ((Number) source).intValue();
			}
			logger.warn("��֧�ֵ�ǰֵת�� �� ��ǰֵ ��" + source + " type : "
					+ source.getClass());
			return source;

		}
	}

	static class TypeHandlerEntry {
		public TypeHandler typeHandler;
		public int index;
	}

}
