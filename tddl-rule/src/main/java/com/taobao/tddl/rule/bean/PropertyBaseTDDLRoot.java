package com.taobao.tddl.rule.bean;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.common.lang.StringUtil;
import com.taobao.tddl.common.config.beans.TableRule;
import com.taobao.tddl.interact.rule.bean.DBType;

public class PropertyBaseTDDLRoot extends TDDLRoot {
	private Log log = LogFactory.getLog(PropertyBaseTDDLRoot.class);
	public static final String DB_TYPE = "db_type";
	public static final String DEFAULT_DB_INDEX = "default_db_index";
	public static final String TABLE_RULES = "table_rules";
	public static final String DB_INDEXES_SUFFIX = ".db_indexes";
	public static final String TABLE_INDEXES_SUFFIX = ".table_indexes";
	public static final String TABLE_RULEINDEX_SUFFIX = ".table_rule";
	public static final String DB_RULEINDEX_SUFFIX = ".db_rule";
	public static final String TABLE_DB_TYPE=".db_type";
	public static final String DISABLE_FULL_TABLE_SCAN=".disable_full_table_scan";
	public static final String DEFAULT_SEPARATOR = ",";
	public static final String CLONE_TABLE_SEPARATOR = ";";
	
	/**
	 * 
	 * ��ʼ��TableRule,���ñ�Ҫ��Ϣ����
	 *     dbIndexes
	 *     dbRuleArray
	 *     tbRuleArray
	 *     tbSuffix
	 * 
	 * @param prop
	 */
	public void init(Properties prop) {
		String[] tableRules = getLogicTableList(prop);
		setDBType(getDBType(prop));
		setDefaultDBIndex(getDefaultDBIndex(prop));
		Map<String/* key */, LogicTable> logicTableMap = new HashMap<String, LogicTable>(
				tableRules.length);
		for (int i = 0; i < tableRules.length; i++) {
			//������Ҫ����Ϊ�����������ĳһ��tableRule�У�����ʹ��;�������зֵĻ�����ô����ʹ����ͬ�Ĺ���ֻ�Ǳ�������
			String[] cloneSeparator = StringUtil.split(tableRules[i],CLONE_TABLE_SEPARATOR);
			if(cloneSeparator.length == 1){
				TableRule tableRule = new TableRule();
				tableRule.setDbIndexes(getDBIndexes(tableRules[i], prop));
				tableRule.setDbRuleArray(getDBRules(tableRules[i], prop));
				tableRule.setTbRuleArray(getTableRules(tableRules[i], prop));
				tableRule.setTbSuffix(getTabIndexes(tableRules[i], prop));
				tableRule.setDisableFullTableScan(getDisableFullTableScan(tableRules[i],prop));
				DBType dbType=getTableDBType(tableRules[i],prop);
				if(null!=dbType){
				    tableRule.setDbType(dbType);
				}
				tableRule.init();
				logicTableMap.put(tableRules[i].toLowerCase(), tableRule);
			}else{
				for(String str: cloneSeparator){
					str = StringUtil.trim(str);
					if(str.length() == 0){
						continue;
					}
					TableRule tableRule = new TableRule();
					tableRule.setDbIndexes(getDBIndexes(tableRules[i], prop));
					tableRule.setDbRuleArray(getDBRules(tableRules[i], prop));
					tableRule.setTbRuleArray(getTableRules(tableRules[i], prop));
					tableRule.setTbSuffix(getTabIndexes(tableRules[i], prop));
					tableRule.setDisableFullTableScan(getDisableFullTableScan(tableRules[i],prop));
					DBType dbType=getTableDBType(tableRules[i],prop);
					if(null!=dbType){
					    tableRule.setDbType(dbType);
					}
					tableRule.init();
					logicTableMap.put(str.toLowerCase(), tableRule);
				}
			
			}
		}
		
		//setLogicTableMap(logicTableMap); //���ﲻ��Ҫ��ȥ��һ����
		this.logicTableMap = logicTableMap;
	}
	private void setDefaultDBIndex(String dbIndex){
		if(dbIndex != null && dbIndex.length() != 0){
			this.setDefaultDBSelectorID(dbIndex);
		}
	}
	/**
	 * �õ��߼����б�
	 * 
	 * @param prop
	 * @return ����:modDBTab,gmtTab
	 */
	private String[] getLogicTableList(Properties prop) {
		return StringUtil.split(getPropValue(TABLE_RULES, prop),
				DEFAULT_SEPARATOR);
	}

	/**
	 * �õ����õ����ݿ�����
	 * 
	 * @param prop
	 * @return ����:mysql
	 */
	private String getDBType(Properties prop) {
		return getPropValue(DB_TYPE, prop);
	}

	/**
	 * �õ����õ����ݿ�����
	 * 
	 * @param prop
	 * @return ����:mysql
	 */
	private String getDefaultDBIndex(Properties prop) {
		return getPropValue(DEFAULT_DB_INDEX, prop);
	}
	/**
	 * �õ�dbIndexes
	 * 
	 * @param tableName
	 * @param prop
	 * @return ����: sample_group_0,sample_group_1
	 */
	private String getDBIndexes(String tableName, Properties prop) {
		String key = tableName + DB_INDEXES_SUFFIX;
		return getPropValue(key, prop);
	}

	/**
	 * �õ�tableIndexes
	 * 
	 * @param tableName
	 * @param prop
	 * @return ���磺throughAllDB:[_0001-_0004]
	 */
	private String getTabIndexes(String tableName, Properties prop) {
		String key = tableName + TABLE_INDEXES_SUFFIX;
		return getPropValue(key, prop);
	}

	/**
	 * �õ�tableRule
	 * 
	 * @param tableName
	 * @param prop
	 * @return ����:#pk#.longValue() % 4 % 2
	 */
	private Object[] getTableRules(String tableName, Properties prop) {
		String key = tableName + TABLE_RULEINDEX_SUFFIX;
		return getRule(key, prop);
	}

	/**
	 * �õ�dbRule
	 * 
	 * @param tableName
	 * @param prop
	 * @return ����:(#pk#.longValue() % 4).intdiv(2)
	 */
	private Object[] getDBRules(String tableName, Properties prop) {
		String key = tableName + DB_RULEINDEX_SUFFIX;
		return getRule(key, prop);
	}

	/**
	 * ��prop�и���key�õ����� ���Ϊ�գ�����null
	 * 
	 * @param key
	 * @param prop
	 * @return
	 */
	private Object[] getRule(String key, Properties prop) {
		Object[] rule = new Object[1];
		rule[0] = getPropValue(key, prop);
		if (rule[0] == null) {
			return null;
		} else {
			return rule;
		}
	}
	
	/**
	 * ��prop�и���key�õ�ȫ��ɨ�迪��
	 * 
	 * �������disableFullTableScanû��ֵ,��ô����true
	 * @param key
	 * @param prop
	 * @return
	 */
	private boolean getDisableFullTableScan(String tableName,Properties prop){
		String key=tableName+DISABLE_FULL_TABLE_SCAN;
		String disableFullTableScan=getPropValue(key,prop);
		if(null==disableFullTableScan){
			return true;
		}
		return Boolean.valueOf(disableFullTableScan);
	}
	
	/**
	 * ��prop�и���key�õ��������ݿ���
	 * @param tableName
	 * @param prop
	 * @return
	 */
	private DBType getTableDBType(String tableName,Properties prop){
		String key=tableName+TABLE_DB_TYPE;
		String dbType=getPropValue(key,prop);
		if(null==dbType){
			return null;
		}
		return DBType.valueOf(dbType.toUpperCase());
	}

	/**
	 * ��Properties�и���key�õ���Ӧֵ
	 * 
	 * @param key
	 * @param prop
	 * @return
	 */
	private String getPropValue(String key, Properties prop) {
		String value=prop.getProperty(key);
		writeToLog(new String[]{key,"=",value});
		return value;
	}
	
	private StringBuilder sb=new StringBuilder();
	private void writeToLog(String[] pieces){
		sb.delete(0, sb.length());
		for(String p:pieces){
			sb.append(p);
		}
		log.info(sb.toString());
	}
}
