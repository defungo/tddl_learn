//Copyright(c) Taobao.com
package com.taobao.tddl.client.handler;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.client.ThreadLocalString;
import com.taobao.tddl.client.databus.DataBus;
import com.taobao.tddl.client.databus.PipelineRuntimeInfo;
import com.taobao.tddl.client.util.LogUtils;
import com.taobao.tddl.client.util.ThreadLocalMap;
import com.taobao.tddl.parser.ParserCache;

/**
 * @description ����handlerʵ����ĸ���,��Ҫ�ṩ���¼�������ķ��� 1.��־��¼���� 2.����ƽ��handler�Ĺ�������,��������滻
 * 
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 2.4.3
 * @since 1.6
 * @date 2010-08-27����04:43:40
 */
public abstract class AbstractHandler implements Handler {
	protected static final Log sqlLog = LogFactory
			.getLog(LogUtils.TDDL_SQL_LOG);

	/**
	 * ȫ�ֱ�cache
	 */
	protected static final ParserCache globalCache = ParserCache.instance();

	/**
	 * �õ�����ʱ��Ϣ
	 * 
	 * @param dataBus
	 * @return
	 */
	protected PipelineRuntimeInfo getPipeLineRuntimeInfo(DataBus dataBus) {
		return (PipelineRuntimeInfo) dataBus
				.getPluginContext(PipelineRuntimeInfo.INFO_NAME);
	}

	/**
	 * debug log
	 * 
	 * @param log
	 * @param contents
	 */
	protected void debugLog(Log log, Object[] contents) {
		if (log.isDebugEnabled()) {
			log.debug(getLogStr(contents));
		}
	}

	/**
	 * info log
	 * 
	 * @param log
	 * @param contents
	 */
	protected void infoLog(Log log, Object[] contents) {
		if (log.isInfoEnabled()) {
			log.info(getLogStr(contents));
		}
	}

	/**
	 * warn log
	 * 
	 * @param log
	 * @param contents
	 */
	protected void warnLog(Log log, Object[] contents) {
		if (log.isWarnEnabled()) {
			log.warn(getLogStr(contents));
		}
	}

	/**
	 * error log
	 * 
	 * @param log
	 * @param contents
	 */
	protected void errorLog(Log log, Object[] contents) {
		if (log.isErrorEnabled()) {
			log.error(getLogStr(contents));
		}
	}

	/**
	 * ȡ����־
	 * 
	 * @param contents
	 * @return
	 */
	private String getLogStr(Object[] contents) {
		StringBuilder sb = new StringBuilder();
		for (Object obj : contents) {
			sb.append(String.valueOf(obj));
		}
		return sb.toString();
	}

	/**
	 * ��ThreadLocal����ȡ�ñ��β�ѯ�Ƿ�ʹ�ò���
	 * 
	 * @return
	 */
	protected boolean getUseParallelFromThreadLocal() {
		Object obj = ThreadLocalMap.get(ThreadLocalString.PARALLEL_EXECUTE);
		boolean useParallel = false;
		if (null != obj) {
			useParallel = (Boolean) obj;
			ThreadLocalMap.put(ThreadLocalString.PARALLEL_EXECUTE, null);
		}

		return useParallel;
	}

	/**
	 * �滻SQL������������Ϊʵ�ʱ����� �� �滻_tableName$ �滻_tableName_ �滻tableName.
	 * �滻tableName( �����滻 _tableName, ,tableName, ,tableName_
	 * 
	 * @param originalSql
	 *            SQL���
	 * @param virtualName
	 *            �������
	 * @param actualName
	 *            ʵ�ʱ���
	 * @return �����滻���SQL��䡣
	 */
	public String replaceTableName(String originalSql, String virtualName,
			String actualName, Log log) {
		boolean padding = false;
		if (log.isDebugEnabled()) {
			StringBuilder buffer = new StringBuilder();
			buffer.append("virtualName = ").append(virtualName).append(", ");
			buffer.append("actualName = ").append(actualName);
			log.debug(buffer.toString());
		}

		if (virtualName.equalsIgnoreCase(actualName)) {
			return originalSql;
		}
		List<Object> sqlPieces = globalCache
				.getTableNameReplacement(originalSql);
		if (sqlPieces == null) {
			List<Object> pieces1 = parseAPattern_begin(virtualName,
					originalSql, new StringBuilder("\\s").append(virtualName)
							.append("$").toString(), padding);

			pieces1 = parseAPattern(virtualName, pieces1, new StringBuilder(
					"\\s").append(virtualName).append("\\s").toString(),
					padding);
			pieces1 = parseAPattern(virtualName, pieces1,
					new StringBuilder(".").append(virtualName).append("\\.")
							.toString(), padding);
			pieces1 = parseAPattern(virtualName, pieces1, new StringBuilder(
					"\\s").append(virtualName).append("\\(").toString(),
					padding);
			pieces1 = parseAPatternByCalcTable(virtualName, pieces1,
					new StringBuilder("//*+.*").append("_").append(virtualName)
							.append("_").append(".*/*/").toString(), padding);
			pieces1 = parseAPattern(virtualName, pieces1, new StringBuilder(
					"\\s").append(virtualName).append("\\,").toString(),
					padding);
			pieces1 = parseAPattern(virtualName, pieces1, new StringBuilder(
					"\\,").append(virtualName).append("\\s").toString(),
					padding);
			// �滻,tableName,
			pieces1 = parseAPattern(virtualName, pieces1, new StringBuilder(
					"\\,").append(virtualName).append("\\,").toString(),
					padding);
			sqlPieces = pieces1;
			sqlPieces = globalCache.setTableNameReplacementIfAbsent(
					originalSql, sqlPieces);
		}
		// ��������SQL
		StringBuilder buffer = new StringBuilder();
		boolean first = true;
		for (Object piece : sqlPieces) {
			if (!(piece instanceof String)) {
				throw new IllegalArgumentException(
						"should not be here ! table is " + piece);
			}
			if (!first) {
				buffer.append(actualName);
			} else {
				first = false;
			}
			buffer.append(piece);
		}

		return buffer.toString();
	}

	protected List<Object> parseAPattern_begin(String virtualName,
			String originalSql, String pattern, boolean padding) {
		Pattern pattern1 = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		List<Object> pieces1 = new LinkedList<Object>();
		Matcher matcher1 = pattern1.matcher(originalSql);
		int start1 = 0;
		while (matcher1.find(start1)) {
			pieces1.add(originalSql.substring(start1, matcher1.start() + 1));
			start1 = matcher1.end();
			if (padding) {
				// TODO: ��СдҪ��֤һ��
				pieces1.add(new LogicTable(virtualName));
			}
		}

		pieces1.add(originalSql.substring(start1));
		return pieces1;
	}

	protected List<Object> parseAPatternByCalcTable(String virtualName,
			List<Object> pieces, String pattern, boolean padding) {
		List<Object> pieces2 = new LinkedList<Object>();
		for (Object piece : pieces) {
			if (piece instanceof String) {
				String strpiece = (String) piece;
				Pattern pattern2 = Pattern.compile(pattern,
						Pattern.CASE_INSENSITIVE);
				Matcher matcher2 = pattern2.matcher(strpiece);
				int start2 = 0;
				while (matcher2.find(start2)) {
					int tableNameStart = matcher2.group().toUpperCase()
							.indexOf(virtualName.toUpperCase())
							//+ start2;
							+matcher2.start();
					int tableNameEnd = tableNameStart + virtualName.length();
					pieces2.add(strpiece.substring(start2, tableNameStart));
					start2 = tableNameEnd;
					if (padding) {
						pieces2.add(new LogicTable(virtualName));
					}
				}
				pieces2.add(strpiece.substring(start2));
			} else {
				pieces2.add(piece);
			}
		}
		return pieces2;
	}

	protected List<Object> parseAPattern(String virtualName,
			List<Object> pieces, String pattern, boolean padding) {
		List<Object> pieces2 = new LinkedList<Object>();
		for (Object piece : pieces) {
			if (piece instanceof String) {
				String strpiece = (String) piece;
				Pattern pattern2 = Pattern.compile(pattern,
						Pattern.CASE_INSENSITIVE);
				Matcher matcher2 = pattern2.matcher(strpiece);
				int start2 = 0;
				while (matcher2.find(start2)) {
					pieces2.add(strpiece.substring(start2 - 1 < 0 ? 0
							: start2 - 1, matcher2.start() + 1));
					start2 = matcher2.end();
					if (padding) {
						pieces2.add(new LogicTable(virtualName));
					}
				}
				pieces2.add(strpiece.substring(start2 - 1 < 0 ? 0 : start2 - 1));
			} else {
				pieces2.add(piece);
			}
		}
		return pieces2;
	}

	/**
	 * ����滻����Ϊ�����ã���������ʱû�к͵����滻���кϲ������ǵ����ó���һ���µķ����� �뵥��ͬ���ǣ����滻�Ĺ�����Ҫ��¼�¡�
	 * 
	 * @param originalSql
	 * @param tableToBeReplaced
	 * @return
	 */
	protected String replcaeMultiTableName(String originalSql,
			Map<String, String> tableToBeReplaced) {
		boolean padding = true;
		if (sqlLog.isDebugEnabled()) {
			StringBuilder buffer = new StringBuilder();
			buffer.append("table  = ").append(tableToBeReplaced).append(", ");
			sqlLog.debug(buffer.toString());
		}

		if (tableToBeReplaced.size() == 1) {
			for (Entry<String, String> entry : tableToBeReplaced.entrySet()) {
				return replaceTableName(originalSql, entry.getKey(),
						entry.getValue(), sqlLog);
			}
		}
		List<Object> sqlPieces = globalCache
				.getTableNameReplacement(originalSql);
		if (sqlPieces == null) {
			for (Entry<String, String> entry : tableToBeReplaced.entrySet()) {
				String virtualName = entry.getKey();
				// tab$
				if (sqlPieces == null) {
					// ��һ�ν��룬�ڶ����Ժ����ͻ���sqlPieces��
					sqlPieces = parseAPattern_begin(virtualName, originalSql,
							new StringBuilder("\\s").append(virtualName)
									.append("$").toString(), padding);
				} else {
					// tab$
					sqlPieces = parseAPattern(virtualName, sqlPieces,
							new StringBuilder("\\s").append(virtualName)
									.append("$").toString(), padding);
				}

				// tab
				sqlPieces = parseAPattern(
						virtualName,
						sqlPieces,
						new StringBuilder("\\s").append(virtualName)
								.append("\\s").toString(), padding);
				// table.
				sqlPieces = parseAPattern(virtualName, sqlPieces,
						new StringBuilder(".").append(virtualName)
								.append("\\.").toString(), padding);
				// tab(
				sqlPieces = parseAPattern(
						virtualName,
						sqlPieces,
						new StringBuilder("\\s").append(virtualName)
								.append("\\(").toString(), padding);
				// /*+ hint */
				sqlPieces = parseAPatternByCalcTable(
						virtualName,
						sqlPieces,
						new StringBuilder("//*+.*").append("_")
								.append(virtualName).append("_")
								.append(".*/*/").toString(), padding);
				sqlPieces = parseAPattern(
						virtualName,
						sqlPieces,
						new StringBuilder("\\s").append(virtualName)
								.append("\\,").toString(), padding);
				sqlPieces = parseAPattern(
						virtualName,
						sqlPieces,
						new StringBuilder("\\,").append(virtualName)
								.append("\\s").toString(), padding);
				// �滻,tableName,
				sqlPieces = parseAPattern(
						virtualName,
						sqlPieces,
						new StringBuilder("\\,").append(virtualName)
								.append("\\,").toString(), padding);
			}

			sqlPieces = globalCache.setTableNameReplacementIfAbsent(
					originalSql, sqlPieces);

		}

		// ��������SQL
		StringBuilder buffer = new StringBuilder();
		for (Object piece : sqlPieces) {
			if (piece instanceof String) {
				buffer.append(piece);
			} else if (piece instanceof LogicTable) {
				buffer.append(tableToBeReplaced
						.get(((LogicTable) piece).logictable));
			}
		}
		return buffer.toString();
	}

	static class LogicTable {
		public LogicTable(String logicTable) {
			this.logictable = logicTable;
		}

		public String logictable;

		@Override
		public String toString() {
			return "logictable:" + logictable;
		}
	}
	
	/**
	 * ��ת���ͣ�handlerֻ���Լ�����Ȥ�����ͽ��д��������Թ�
	 * 
	 * @author junyu
	 * 
	 */
	public enum FlowType {
		DIRECT, NOSQLPARSE, DEFAULT, BATCH,BATCH_DIRECT,BATCH_NOSQLPARSER,DBANDTAB_RC, // ֻ�õ�dispatchResult,����������
		DBANDTAB_SQL
		// ֻ�õ�dispatchResult,����������
	}
}
