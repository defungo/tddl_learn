package com.taobao.tddl.rule.ruleengine.entities.inputvalue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.taobao.tddl.interact.rule.bean.DBType;

/**
 * ��Ӧһ�������Ķ����DBRule
 * 
 * @author shenxun
 * 
 */
@SuppressWarnings("unchecked")
public class LogicTabMatrix {
	/**
	 * ���������
	 */
	private boolean allowReverseOutput;

	/*public enum DB_TYPE {
		ORACLE, MYSQL
	}*/

	/**
	 * Ĭ�ϵĽ�������
	 */
	private DBType dbType = DBType.MYSQL;
	/**
	 * ���������ֻ���ڱ����滻��ʱ��������tableName������ԭ���Ĵ�Сд
	 */
	private String tableName;

	/**
	 * ��ѡ��������Map
	 */
	private Map<String, DBRule> depositedRules = Collections.EMPTY_MAP;

	/**
	 * ���й����ŵ�Map,������Щû��expression String�ֶΣ�ֻ����defaultRule��keyvalue��
	 */
	private Map<String, DBRule> allRules = Collections.EMPTY_MAP;

	/**
	 * Ĭ�����������б�
	 */
	private List<DBRule> defaultRules = new ArrayList<DBRule>();

	private TabRule globalTableRule = null;

	/**
	 * �Ƿ���Ҫ�������
	 */
	private boolean needRowCopy = false;

	/**
	 * ���������� ��������������ͱ��׺֮��ķָ�
	 */
	private String tableFactor = null;

	public Map<String, DBRule> getAllRules() {
		return allRules;
	}

	public void setAllRules(Map<String, DBRule> allRules) {
		this.allRules = allRules;
	}

	public TabRule getGlobalTableRule() {
		return globalTableRule;
	}

	public void setGlobalTableRule(TabRule globalTableRule) {
		this.globalTableRule = globalTableRule;
	}

	/**
	 * ֻ���ڱ����滻��ʱ��������tableName������ԭ���Ĵ�Сд
	 * 
	 * @return
	 */
	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		if (tableName != null) {
			this.tableName = tableName.toLowerCase();
		} else {
			this.tableName = "";
		}
	}

	public Map<String, DBRule> getDepositedRules() {
		return depositedRules;
	}

	public void setDepositedRules(Map<String, DBRule> depositedRules) {
		this.depositedRules = depositedRules;
	}

	public List<DBRule> getDefaultRules() {
		return defaultRules;
	}

	public void setDefaultRules(List<DBRule> defaultRules) {
		this.defaultRules = defaultRules;
	}

	public boolean isNeedRowCopy() {
		return needRowCopy;
	}

	public void setNeedRowCopy(boolean needRowCopy) {
		this.needRowCopy = needRowCopy;
	}

	public String getTableFactor() {
		return tableFactor;
	}

	public void setTableFactor(String tableFactor) {
		if (tableFactor != null) {
			this.tableFactor = tableFactor.toLowerCase();
		}

	}

	public DBType getDBType() {
		return dbType;
	}

	public void setDBType(String dbType) {
		if (dbType != null && !dbType.equals("")) {
			this.dbType = DBType.valueOf(dbType.toUpperCase());
		}
	}

	public boolean isAllowReverseOutput() {
		return allowReverseOutput;
	}

	public void setAllowReverseOutput(boolean allowReverseOutput) {
		this.allowReverseOutput = allowReverseOutput;
	}

}
