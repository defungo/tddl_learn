package com.taobao.tddl.rule.bean;

import groovy.lang.GroovyClassLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.common.lang.StringUtil;
import com.taobao.tddl.rule.ruleengine.entities.abstractentities.SharedElement;
import com.taobao.tddl.rule.ruleengine.entities.convientobjectmaker.TableMapProvider;
import com.taobao.tddl.rule.ruleengine.util.RuleUtils;

/**
 * �ṩͨ��ƴװ�ķ�ʽ������SimpleTableMap�ķ�ʽ
 * 
 * ʹ�÷���:�ڸ������з����Ļ����ϣ�����Ҫָ��fromDateString ,toDateString, calendarType �������ԣ�
 * 
 * �����Ҫ������ں���������Ҫ�����ں���Ҳһ�����룬Ҳ����timeStyle.
 * 
 * ���ܹ���ԭ���Ļ��������һ���µ�date�ֶ������档
 * 
 * 
 * TODO: �ع��ⲿ�ִ��룬SimpleTableMapProvider�ʹ˴����н�����ʹ�ô˴����滻
 * 
 * @author shenxun
 * 
 */
public class SimpleDateTableMapProvider implements TableMapProvider {
	public enum TYPE {
		NORMAL, CUSTOM
	}

	public static final String NORMAL_TAOBAO_TYPE = "NORMAL";

	public static final String DEFAULT_PADDING = "_";

	protected static final int DEFAULT_INT = -1;

	public static final int DEFAULT_TABLES_NUM_FOR_EACH_DB = -1;

	private String type = NORMAL_TAOBAO_TYPE;
	/**
	 * table[padding]suffix Ĭ�ϵ�padding��_
	 */
	private String padding;
	/**
	 * width ���
	 */
	private int width = DEFAULT_INT;
	/**
	 * �ֱ��ʶ���ӡ�����˵�ʼ��ͷ����ʲô�������ָ����Ĭ�����߼�����
	 */
	private String tableFactor;
	/**
	 * �߼�����
	 */
	private String logicTable;
	/**
	 * ÿ��������
	 */
	private int step = 1;

	/**
	 * ÿ�����ݿ�ı�ĸ��������ָ����������ÿ�����ڵĸ�����Ϊָ�����
	 */
	private int tablesNumberForEachDatabases = DEFAULT_TABLES_NUM_FOR_EACH_DB;
	/**
	 * database id
	 */
	private String parentID;
	/**
	 * ÿ�����ݿ�ı�ĸ����ж��ٸ� >= ?
	 */
	private int from = DEFAULT_INT;
	/**
	 * <= ?
	 */
	private int to = DEFAULT_INT;

	private boolean doesNotSetTablesNumberForEachDatabases() {
		return tablesNumberForEachDatabases == -1;
	}

	public int getFrom() {
		return from;
	}

	public String getPadding() {
		return padding;
	}

	public String getParentID() {
		return parentID;
	}

	public int getStep() {
		return step;
	}

	private static final Log logger = LogFactory
			.getLog(SimpleDateTableMapProvider.class);
	/**
	 * simple date format ��ʽ
	 */
	private String timeStyle = "yyMM";
	/**
	 * �趨��������������ʽ
	 */
	private final static String inputTimeStyle = "yyyyMM";

	/**
	 * ���Ŀ�ʼ
	 */
	private String fromDateString;

	/**
	 * ���Ľ���
	 */
	private String toDateString;

	/**
	 * �������������õ�calendar��ʽ
	 */
	private CALENDAR_TYPE calendarType = CALENDAR_TYPE.MONTH;

	private boolean isOnlyDateSharding = false;

	public enum CALENDAR_TYPE {
		DATE(Calendar.DATE), MONTH(Calendar.MONTH), YEAR(Calendar.YEAR), HOUR(
				Calendar.HOUR), QUARTER(Calendar.MONTH), HALF_A_YEAR(
				Calendar.MONTH),WEEK_OF_MONTH(Calendar.WEEK_OF_MONTH)
				,WEEK_OF_YEAR(Calendar.WEEK_OF_YEAR),GROOVY(Calendar.MONTH);

		private int i;

		public int value() {
			return this.i;
		}

		private CALENDAR_TYPE(int i) {
			this.i = i;
		}

	}
	
	private String groovyScript;
	

	public static void main(String[] args) {
		System.out.println(CALENDAR_TYPE.DATE);
		System.out.println(CALENDAR_TYPE.DATE.value());

		System.out.println(CALENDAR_TYPE.QUARTER);
		System.out.println(CALENDAR_TYPE.QUARTER.value());

		System.out.println(CALENDAR_TYPE.DATE == CALENDAR_TYPE.QUARTER);
	}

	SimpleDateFormat simpleDateFormat;

	public Map<String, SharedElement> getTablesMap() {

		if (tableFactor == null && logicTable != null) {
			tableFactor = logicTable;
		}
		if (tableFactor == null) {
			throw new IllegalArgumentException("û�б�����������");
		}

		List<String> dateArgsList = getDateStringList();
	
		logger.warn(dateArgsList);
		TYPE typeEnum = TYPE.valueOf(type);

		makeRealTableNameTaobaoLike(typeEnum);

		// ���û������ÿ�����ݿ��ĸ�������ô��ʾ���б���ͳһ�ı���������(tab_0~tab_3)*16�����ݿ�=64�ű�
		if (doesNotSetTablesNumberForEachDatabases()) {
			return getSuffixList(from, to, width, step, tableFactor, padding,
					dateArgsList);
		} else {
			// ���������ÿ�����ݿ��ĸ�������ô��ʾ���б��ò�ͬ�ı���������(tab_0~tab63),�ֲ���16�����ݿ���
			int multiple = 0;
			try {
				multiple = Integer.valueOf(parentID);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(
						"ʹ��simpleTableMapProvider����ָ����tablesNumberForEachDatabase������database��indexֵ�����Ǹ�integer����"
								+ "��ǰdatabase��index��:" + parentID);
			}
			int start = tablesNumberForEachDatabases * multiple;
			// ��Ϊβ׺�ķ�Χ�ǵ�<=�����֣�����Ҫ-1.
			int end = start + tablesNumberForEachDatabases - 1;
			// ���õ�ǰdatabase����ı���
			return getSuffixList(start, end, width, step, tableFactor, padding,
					dateArgsList);
		}

	}

	/**
	 * ���ڴ���ʱ���ִ���ƴ������
	 * 
	 * @author shenxun
	 * 
	 */
	private static interface DateStringListHandler {
		List<String> getDateStringList(String tableFactor, String logicTable,
				String timeStyle, String fromDateString,
				String toDateString, CALENDAR_TYPE calendarType,
				String groovyScript);
	}

	
	public static abstract class CustomStringListHandlerCommon implements
			DateStringListHandler {
		// �����˼����Ժ� һ����˵�������÷���һ���Ǽ���ݣ�4λ 2λ��һ���ǲ�����ݡ�
		public List<String> getDateStringList(String tableFactor,
				String logicTable, String timeStyle,
				String fromDateString, String toDateString,
				CALENDAR_TYPE calendarType, String groovyScript) {
			List<String> dateArgsList = null;
			logger.warn("init data table map");
			// �ڳ�ʼ����ʱ������TableMap ;
			if (tableFactor == null && logicTable != null) {
				tableFactor = logicTable;
			}
			if (tableFactor == null) {
				throw new IllegalArgumentException("û�б�����������");
			}
			if (timeStyle == null) {
				throw new IllegalArgumentException("ʱ�����");
			}
			dateArgsList = new ArrayList<String>();
			SimpleDateFormat inputDateFormat = new SimpleDateFormat(inputTimeStyle);
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(timeStyle);
			Date fromDate = null;
			Date toDate = null;
			Calendar cal = Calendar.getInstance();

			Calendar calDis = Calendar.getInstance();
			try {
				fromDate = inputDateFormat.parse(fromDateString);
				toDate = inputDateFormat.parse(toDateString);

				cal.setTime(fromDate);
				calDis.setTime(toDate);
				logger.warn("from date :" + fromDate);
				logger.warn("to date :" + toDate);
			} catch (ParseException e) {
				throw new IllegalArgumentException("from : " + fromDateString
						+ ". to : " + toDateString + " inputDateFormat : "
						+ inputDateFormat);
			} catch (NullPointerException e) {
				throw new IllegalArgumentException("from : " + fromDateString
						+ ". to : " + toDateString + " inputDateFormat : "
						+ inputDateFormat);
			}

			// ��ȡ��ݴ��Ŀ�ʼ�����Ľ���
			
			int start = timeStyle.indexOf("y");
			start = (start == -1)?0:start;
			int end = timeStyle.lastIndexOf("y") + 1;
			// first element
			Date date = cal.getTime();
			String dateStr = buildQuarterString(simpleDateFormat, calendarType,
					cal, start, end, date, getDuration());
			dateArgsList.add(dateStr);

			while (true) {
				cal.add(calendarType.value(), 3);

				if (cal.compareTo(calDis) > 0) {
					date = calDis.getTime();
					dateStr = buildQuarterString(simpleDateFormat,
							calendarType, calDis, start, end, date,
							getDuration());
					dateArgsList.add(dateStr);
					break;
				} else {
					date = cal.getTime();
					dateStr = buildQuarterString(simpleDateFormat,
							calendarType, cal, start, end, date, getDuration());
					dateArgsList.add(dateStr);
				}

			}
			logger.warn(dateArgsList + "inited");
			return dateArgsList;
		}

		protected abstract int getDuration();
	}

	public static class QuarterStringListHandler extends
			CustomStringListHandlerCommon {

		@Override
		protected int getDuration() {
			return 3;
		}

	}

	public static class HalfayearStringListHandler extends
			CustomStringListHandlerCommon {

		@Override
		protected int getDuration() {
			return 6;
		}

	}

	/**
	 * ���뵽�������� yyyyQ yyQ Q Q��quarter�����ֱ�ʾ����1��ʼ��4
	 * 
	 * @param simpleDateFormat
	 * @param calendarType
	 * @param cal
	 * @param start
	 * @param end
	 * @param date
	 * @return
	 */
	private static String buildQuarterString(SimpleDateFormat simpleDateFormat,
			CALENDAR_TYPE calendarType, Calendar cal, int start, int end,
			Date date, int duration) {
		String dateStr = simpleDateFormat.format(date);
		// ��year��ȡ�����ŵ���builder��
		StringBuilder dateStringBuilder = new StringBuilder();
		dateStringBuilder.append(dateStr.subSequence(start, end));
		int dateField = cal.get(calendarType.value());
		
		int value = dateField / duration + 1;
		dateStringBuilder.append(RuleUtils.placeHolder(2, value));
		dateStr = dateStringBuilder.toString();
		return dateStr;
	}

	/**
	 * Ĭ�ϵĴ�����
	 * 
	 * @author shenxun
	 * 
	 */
	public static class DefaultStringListHandler implements
			DateStringListHandler {

		public List<String> getDateStringList(String tableFactor,
				String logicTable, String timeStyle,
				String fromDateString, String toDateString,
				CALENDAR_TYPE calendarType, String groovyScript) {
			
			// �ڳ�ʼ����ʱ������TableMap ;
			if (tableFactor == null && logicTable != null) {
				tableFactor = logicTable;
			}
			if (tableFactor == null) {
				throw new IllegalArgumentException("û�б�����������");
			}
			if (timeStyle == null) {
				throw new IllegalArgumentException("ʱ�����");
			}
			SimpleDateFormat inputDateFormat = new SimpleDateFormat(inputTimeStyle);
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(timeStyle);
			Date fromDate = null;
			Date toDate = null;
			Calendar cal = Calendar.getInstance();

			Calendar calDis = Calendar.getInstance();
			try {
				fromDate = inputDateFormat.parse(fromDateString);
				toDate = inputDateFormat.parse(toDateString);

				cal.setTime(fromDate);
				calDis.setTime(toDate);
				logger.warn("from date :" + fromDate);
				logger.warn("to date :" + toDate);
			} catch (ParseException e) {
				throw new IllegalArgumentException("from : " + fromDateString
						+ ". to : " + toDateString + " inputDateFormat : "
						+ inputTimeStyle);
			} catch (NullPointerException e) {
				throw new IllegalArgumentException("from : " + fromDateString
						+ ". to : " + toDateString + " inputDateFormat : "
						+ inputTimeStyle);
			}
			List<String> dateArgsList = buildArgsList(calendarType,
					simpleDateFormat, cal, calDis);
			return dateArgsList;
		}

		protected List<String> buildArgsList(CALENDAR_TYPE calendarType,
				SimpleDateFormat simpleDateFormat, Calendar cal, Calendar calDis) {
			List<String> dateArgsList = new ArrayList<String>();
			dateArgsList.add(simpleDateFormat.format(cal.getTime()));
			while (true) {
				cal.add(calendarType.value(), 1);
				if (cal.compareTo(calDis) > 0) {
					dateArgsList.add(simpleDateFormat.format(calDis.getTime()));
					break;
				} else {
					dateArgsList.add(simpleDateFormat.format(cal.getTime()));
				}

			}
			return dateArgsList;
		}

	}

	/**
	 * ��ȡdateStringList
	 * 
	 * Ĭ�ϵĸ�ʽ��yyyyMMdd ȫ�������֡������ڱ���ƴװ��ͬʱҲ�ᱻ������ӵ��ֱ��key��ȥ��
	 * 
	 * �������÷������֡� 1. Ĭ����������£����yyyyMMdd����ֱ��ת��Ϊ���� 2.
	 * ���ҵ��ָ�����Լ��ĸ�ʽ����ô��Ҫ��ҵ���key��Ҳƴ����Ӧ��ģʽ���ɡ�
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<String> getDateStringList() {
		DateStringListHandler handler;
		if (CALENDAR_TYPE.QUARTER == calendarType) {
			handler = new QuarterStringListHandler();
		} else if (CALENDAR_TYPE.HALF_A_YEAR == calendarType) {
			handler = new HalfayearStringListHandler();
		} else if (CALENDAR_TYPE.GROOVY == calendarType) {
			GroovyClassLoader loader = new GroovyClassLoader(SimpleDateTableMapProvider.class.getClassLoader());
			Class<DateStringListHandler> handlerClass = loader.parseClass(groovyScript);
			try {
				handler = handlerClass.newInstance();
			} catch (InstantiationException e) {
				throw new IllegalArgumentException("groovy script new instance error , script is "
						+groovyScript,e);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException("groovy script new instance error , script is "
						+groovyScript,e);
			}
		} else {
			// �����ķǼ��Ȼ�������r
			handler = new DefaultStringListHandler();
		}
		List<String> dateArgsList = handler.getDateStringList(tableFactor,
				logicTable, timeStyle, fromDateString, toDateString,
				calendarType, groovyScript);
		return dateArgsList;
	}

	/**
	 * ���������������£���ôӦ���Ǳ�������+"_"+�����λ����������β׺������ tab_001~tab_100
	 */
	protected void makeRealTableNameTaobaoLike(TYPE typeEnum) {
		if (typeEnum == TYPE.CUSTOM) {
			// do nothing
		} else {
			if (padding == null)
				padding = DEFAULT_PADDING;
			if (to != DEFAULT_INT && width == DEFAULT_INT) {
				width = String.valueOf(to).length();
			}
		}

	}

	protected Map<String, SharedElement> getSuffixList(int from, int to,
			int width, int step, String tableFactor, String padding,
			List<String> dateArgsList) {
		Map<String, SharedElement> map = null;
		if (!isOnlyDateSharding) {
			map = buildTableMapWithTableSharding(from, to, width, step,
					tableFactor, padding, dateArgsList);
		} else {
			map = buildTableMapWithoutTableSharding(from, to, width, step,
					tableFactor, padding, dateArgsList);
		}

		logger.info("table map :" + map + "inited!");
		return map;
	}

	private Map<String, SharedElement> buildTableMapWithoutTableSharding(
			int from, int to, int width, int step, String tableFactor,
			String padding, List<String> dateArgsList) {
		Map<String, SharedElement> tableMap = new HashMap<String, SharedElement>(
				1);
		StringBuilder sb = new StringBuilder();
		sb.append(tableFactor);
		sb.append(padding);
		String tableProfix = sb.toString();

		for (String time : dateArgsList) {
			String key = time;
			StringBuilder singleTableBuilder = new StringBuilder(tableProfix);
			singleTableBuilder.append(time);
			tableMap.put(key, new Table(singleTableBuilder.toString()));
		}
		
		return tableMap;
	}

	private Map<String, SharedElement> buildTableMapWithTableSharding(int from,
			int to, int width, int step, String tableFactor, String padding,
			List<String> dateArgsList) {
		if (from == DEFAULT_INT || to == DEFAULT_INT) {
			throw new IllegalArgumentException("from,to must be spec!");
		}
		int length = to - from + 1;
		Map<String, SharedElement> tableMap = new HashMap<String, SharedElement>(
				length);
		StringBuilder sb = new StringBuilder();
		sb.append(tableFactor);
		sb.append(padding);
		String tableProfix = sb.toString();
		int tableForEachDB = to - from + 1 ;
		for (int i = from; i <= to; i = i + step) {
			//key �ڶ��databases�б�����һ�µġ�
			int index = i % tableForEachDB;
			//����������·ݺ��Ϊ��β׺
			String keySuffix = null;
			//��������ڱ��ı�β׺��tableSuffix��intֵ mod ��ǰ��ı�ĸ������͵���keySuffix��ֵ��
			String tableSuffix = null;
			if (width != DEFAULT_INT) {
				tableSuffix = RuleUtils.placeHolder(width, i);
				keySuffix = RuleUtils.placeHolder(width, index);
			} else {
				// �������ʽָ��width����ָ��Ϊ-1���򲻲��㣬ֱ������ֵΪ��׺
				tableSuffix = String.valueOf(i);
				keySuffix = String.valueOf(index);
			}
			for (String time : dateArgsList) {
				String key = time + keySuffix;
				StringBuilder singleTableBuilder = new StringBuilder(
						tableProfix);
				singleTableBuilder.append(time);
				singleTableBuilder.append(padding);
				singleTableBuilder.append(tableSuffix);
				tableMap.put(key, new Table(singleTableBuilder.toString()));
			}
		}
		return tableMap;
	}

	public String getTimeStyle() {
		return timeStyle;
	}

	public void setTimeStyle(String timeStyle) {
		if (timeStyle != null) {
			this.timeStyle = timeStyle;
		}
	}

	public String getFromDateString() {
		return fromDateString;
	}

	public void setFromDateString(String fromDateString) {
		if (fromDateString != null) {
			this.fromDateString = fromDateString;
		}
	}

	public String getToDateString() {
		return toDateString;
	}

	public void setToDateString(String toDateString) {
		if (toDateString != null) {
			this.toDateString = toDateString;
		}
	}

	public CALENDAR_TYPE getCalendarType() {
		return calendarType;
	}

	public void setCalendarType(CALENDAR_TYPE calendarType) {
		this.calendarType = calendarType;
	}

	public void setCalendarTypeString(String calendarTypeString) {
		if (calendarTypeString != null) {
			calendarTypeString = StringUtil.toUpperCase(calendarTypeString);
			calendarType = CALENDAR_TYPE.valueOf(calendarTypeString);
		}

	}

	public void setFrom(int from) {
		this.from = from;
	}

	public void setLogicTable(String logicTable) {
		this.logicTable = logicTable;
	}

	public void setPadding(String padding) {
		this.padding = padding;
	}

	public void setParentID(String parentID) {
		this.parentID = parentID;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public void setTableFactor(String tableFactor) {
		this.tableFactor = tableFactor;
	}

	public void setTablesNumberForEachDatabases(int tablesNumberForEachDatabases) {
		this.tablesNumberForEachDatabases = tablesNumberForEachDatabases;
	}

	public void setTo(int to) {
		this.to = to;
	}

	public void setType(String type) {
		this.type = type;
	}


	public boolean isOnlyDateSharding() {
		return isOnlyDateSharding;
	}

	public void setOnlyDateSharding(boolean isOnlyDateSharding) {
		this.isOnlyDateSharding = isOnlyDateSharding;
	}

	public void setWidth(int width) {
		if (width > 8) {
			throw new IllegalArgumentException("ռλ�����ܳ���8λ");
		}
		// ���׺ռλ������Ϊ0, ��ʱ������
		if (width < 0) {
			throw new IllegalArgumentException("ռλ������Ϊ��ֵ");
		}
		this.width = width;
	}
	

	public String getGroovyScript() {
		return groovyScript;
	}

	public void setGroovyScript(String groovyScript) {
		this.groovyScript = groovyScript;
	}

	
	public String getInputTimeStyle() {
		return inputTimeStyle;
	}

	

	@Override
	public String toString() {
		return "SimpleDateTableMapProvider [calendarType=" + calendarType
				+ ", from=" + from + ", fromDateString=" + fromDateString
				+ ", isOnlyDateSharding=" + isOnlyDateSharding
				+ ", logicTable=" + logicTable + ", padding=" + padding
				+ ", parentID=" + parentID + ", simpleDateFormat="
				+ simpleDateFormat + ", step=" + step + ", tableFactor="
				+ tableFactor + ", tablesNumberForEachDatabases="
				+ tablesNumberForEachDatabases + ", timeStyle=" + timeStyle
				+ ", to=" + to + ", toDateString=" + toDateString + ", type="
				+ type + ", width=" + width + "]";
	}
	
}
