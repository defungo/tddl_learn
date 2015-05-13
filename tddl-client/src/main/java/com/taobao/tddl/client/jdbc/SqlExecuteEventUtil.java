package com.taobao.tddl.client.jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.taobao.tddl.client.controller.ColumnMetaData;
import com.taobao.tddl.client.dispatcher.DispatcherResult;
import com.taobao.tddl.interact.rule.bean.SqlType;
import com.taobao.tddl.interact.rule.enumerator.Enumerator;
import com.taobao.tddl.interact.rule.enumerator.EnumeratorImp;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.interact.sqljep.ComparativeBaseList;

/**
 * һ����ExecuteEvent��صĹ�����
 * 
 * @author linxuan
 *
 */
public class SqlExecuteEventUtil {
	private static final Enumerator enumerator = new EnumeratorImp();

	/**
	 * �����и��Ƶ�SqlExecuteEvent
	 */
	private static SqlExecuteEvent createEvent(DispatcherResult metaData, SqlType sqlType, String originalSql,
			Object primaryKeyValue) throws SQLException {
		DefaultSqlExecuteEvent event = new DefaultSqlExecuteEvent();
		event.setReplicated(metaData.needRowCopy());
		event.setSqlType(sqlType);
		event.setLogicTableName(metaData.getVirtualTableName().toString());
		event.setPrimaryKeyColumn(metaData.getPrimaryKey().key.toLowerCase());
		//event.setPrimaryKeyValue(metaData.getPrimaryKey().value.getValue());
		event.setPrimaryKeyValue(primaryKeyValue);

		if (hasAvalue(metaData.getSplitDB())) {
			event.setDatabaseShardColumn(metaData.getSplitDB().get(0).key.toLowerCase());
			event.setDatabaseShardValue(metaData.getSplitDB().get(0).value.getValue());
		}
		if (hasAvalue(metaData.getSplitTab())) {
			event.setTableShardColumn(metaData.getSplitTab().get(0).key.toLowerCase());
			event.setTableShardValue(metaData.getSplitTab().get(0).value.getValue());
		}

		event.setSql(originalSql);
		return event;
	}

	private static boolean hasAvalue(List<ColumnMetaData> columnMetaDatas) {
		if (columnMetaDatas != null && !columnMetaDatas.isEmpty() && columnMetaDatas.get(0).key != null
				&& columnMetaDatas.get(0).key.length() > 0 && columnMetaDatas.get(0).value != null) {
			return true;
		}
		return false;
	}

	private static boolean isConfuse(ColumnMetaData uniqeMeta, List<ColumnMetaData> splitMetas) {
		if (hasAvalue(splitMetas) && !uniqeMeta.key.equals(splitMetas.get(0).key)) {
			Comparative value = splitMetas.get(0).value;
			if ((value instanceof ComparativeBaseList) || (value.getComparison() != Comparative.Equivalent)) {
				//Ψһ�����Ƿֿ�/�ֱ��, �ҷֿ�/�ֱ����sql�������������Ҳ��ǽ���һ��=����(and/or��>=<)
				return true;
			}
		}
		return false;
	}

	/**
	 * update in�����⣺
	 * ���update in ���ֶα�����Ƿֿ�ֱ��ֶΣ����ǿ���֧��ͬʱ���¶������ݵ���ͬ���ġ�
	 * ֻ����update in���Ƿֿ�ֱ��ֶε�����£���Ҫ��where�еķֿ�ֱ��Ҫôֻ��һ��=������Ҫôû�С�
	 * ����tddl�޷�֪���ĸ�id�ڷֿ�ֱ�����ĸ�ֵ�����ҵ���ֻ�ܼ򵥵���ÿ��id��Ӧ��һ���ֿ�ֱ����
	 * �����ڶԲ��Ϻŵ������,�и���ȥgetMasterRow�������⣩ʱ�ͻᶨλ������Ŀ���������¼�����ڣ�
	 * ����������ݸ��ж�ʧ
	 * 
	 * ����һ��Ч�ʵ��µĽ��������������uniqekey���Ƿֿ�ֱ��ֶΣ����ҷֿ�ֱ��ֶ��ж��������ʱ��,
	 * ���и��Ƶ�event�иɴ಻��ֿ�ֱ��ֶ�ֵ��������ʱȥ�����б�ɨ�衣��������Ҫ��ҵ������Ĭ�Ͽ��
	 * 
	 * �ܵ���˵���������ƣ�
	 * 1. ����ȱ��Ψһ������SQL��û��Ψһ��ֵ���������и��Ʋ�֧�֣����쳣
	 * 2. �ֿ����ֱ���ж������֧�֡����쳣������Ϊsync_log��־��ṹ�����⣬�����ö��ŷָ��ķ�ʽ����
	 * 3. ��������˵�ģ�Ψһ�����Ƿֿ�/�ֱ��, �ҷֿ�/�ֱ����sql�������������Ҳ��ǽ���һ��=���������쳣
	 *    Ψһ�����Ƿֿ�/�ֱ���������������Ϊ���³�����
	 *    a���ֿ�/�ֱ����SQL��û��������֧�֡���־��¼�зֿ�ֱ���ֵ��Ϊ�գ��и��ƶ�����ʱ��ɨ���еı�
	 *    b: �ֿ�/�ֱ����SQL��ֻ��һ������������֧�֡�����־ʱ��������¼Ψһ����ͬ��in�����ֿ�/�ֱ����=���ֵ
	 *    c: �ֿ�/�ֱ����SQL��ֻ��һ��������������=��������֧�֣����쳣��< > <= >= != �޷��Ժ�
	 *    d: �ֿ�/�ֱ����SQL���ж����������֧�֣����쳣��
	 */
	public static List<SqlExecuteEvent> createEvent(DispatcherResult metaData, SqlType sqlType, String originalSql)
			throws SQLException {

		if (metaData.getPrimaryKey() == null || metaData.getPrimaryKey().key == null
				|| metaData.getPrimaryKey().key.length() == 0) {
			throw new SQLException("�ֿ�ֱ����ȱ��Ψһ����");
		}
		if (metaData.getPrimaryKey().value == null) {
			throw new SQLException("SQL��û��Ψһ��, sql = " + originalSql);
		}

		if (metaData.getSplitDB() != null && metaData.getSplitDB().size() > 1) {
			throw new SQLException("TDDL�и���Ŀǰ��֧��sql�еķֿ��ֶζ���������" + originalSql);
		}
		if (metaData.getSplitTab() != null && metaData.getSplitTab().size() > 1) {
			throw new SQLException("TDDL�и���Ŀǰ��֧��sql�еķֱ��ֶζ���������" + originalSql);
		}

		if (isConfuse(metaData.getPrimaryKey(), metaData.getSplitDB())) {
			throw new SQLException("Ψһ�����Ƿֿ��, �ҷֿ����sql�������������Ҳ��ǽ���һ������������" + originalSql);
		}
		if (isConfuse(metaData.getPrimaryKey(), metaData.getSplitTab())) {
			throw new SQLException("Ψһ�����Ƿֱ��, �ҷֿ����sql�������������Ҳ��ǽ���һ������������" + originalSql);
		}

		boolean needMergeValueInCloseRange = false;
		List<SqlExecuteEvent> res = new ArrayList<SqlExecuteEvent>();
		Set<Object> uniqeKeyValues = enumerator.getEnumeratedValue(metaData.getPrimaryKey().value, null, null,needMergeValueInCloseRange);
		for (Object uniqeKeyValue : uniqeKeyValues) {
			res.add(createEvent(metaData, sqlType, originalSql, uniqeKeyValue));
		}
		return res;
	}
}
