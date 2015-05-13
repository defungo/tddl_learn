package com.taobao.tddl.rule.bean;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.interact.rule.bean.DBType;
import com.taobao.tddl.rule.LogicTableRule;

public class TDDLRoot {
	final Log log = LogFactory.getLog(TDDLRoot.class);
	protected DBType dbType = DBType.MYSQL;
	protected Map<String/* key */, LogicTable> logicTableMap;
	protected String defaultDBSelectorID;
	//true��ʹ��id in�����Ż�
	protected boolean needIdInGroup=false;
	//true��ʹ�ö���Distinct֧��
	protected boolean completeDistinct=false;
	//ture��ʹ�������������Ĺ�����㷽ʽ
	protected boolean newTypeRuleCalculate=false;

	/**
	 * ��Ҫע�����init�����Ǻ��ڲ����е����init�����޹صģ� ��Ȼ�����ڷ���һ����ʼ������
	 */
	public void init() {
		for (Entry<String, LogicTable> logicTableEntry : logicTableMap
				.entrySet()) {
			log.warn("logic Table is starting :" + logicTableEntry.getKey());
			LogicTable logicTable = logicTableEntry.getValue();
			String logicTableName = logicTable.getLogicTableName();
			if (logicTableName == null || logicTableName.length() == 0) {
				// ���û��ָ��logicTableName,
				// ��ô��map��key��ΪlogicTable��key
				logicTable.setLogicTableName(logicTableEntry.getKey());
			}
			//modify by junyu 2010.10.26 Oracle��Mysql���ø���
			logicTable.setShardRuleDbType(dbType);
			logicTable.init(false);

			log.warn("logic Table inited :" + logicTable.toString());
		}
	}

	public LogicTableRule getLogicTableMap(String logicTableName) {
		LogicTableRule logicTableRule = getLogicTable(logicTableName);
		if (logicTableRule == null) {
			// �߼������������ڹ�����У����Դ�Ĭ�ϱ����Ѱ�ң�
			// ������Ҳ��������쳣�ˡ�
			if (defaultDBSelectorID != null
					&& defaultDBSelectorID.length() != 0) {
				// �����Ĭ�Ϲ�����ô��ΪĬ�Ϲ����г��е�ֻ������Դ��
				// ��Ҫ������������¡һ���Ժ������������֤�̰߳�ȫ
				log.debug("use default table rule");
				DefaultLogicTableRule defaultLogicTableRule = new DefaultLogicTableRule(
						defaultDBSelectorID, logicTableName);
				logicTableRule = defaultLogicTableRule;
			} else {
				throw new IllegalArgumentException("δ���ҵ���Ӧ����,�߼���:"
						+ logicTableName);
			}
		}
		return logicTableRule;
	}

	public LogicTable getLogicTable(String logicTableName) {
		if (logicTableName == null) {
			throw new IllegalArgumentException("logic table name is null");
		}
		
		LogicTable logicTable = logicTableMap.get(logicTableName.toLowerCase());
		return logicTable;
	}

	/**
	 * logicMap��key���붼��ʾ������ΪСд
	 * 
	 * @param logicTableMap
	 */
	public void setLogicTableMap(Map<String, LogicTable> logicTableMap) {
		this.logicTableMap = new HashMap<String, LogicTable>(logicTableMap
				.size());
		for (Entry<String, LogicTable> entry : logicTableMap.entrySet()) {
			String key = entry.getKey();
			if (key != null) {
				key = key.toLowerCase();
			}
			this.logicTableMap.put(key, entry.getValue());
		}
	}

	public Map<String, LogicTable> getLogicTableMap() {
		return Collections.unmodifiableMap(logicTableMap);
	}

	public Object getDBType() {
		return dbType;
	}

	public void setDBType(Object dbType) {
		if (dbType instanceof DBType) {
			this.dbType = (DBType) dbType;
		} else if (dbType instanceof String) {
			this.dbType = DBType.valueOf(((String) dbType).toUpperCase());
		}
	}

	public String getDefaultDBSelectorID() {
		return defaultDBSelectorID;
	}

	public void setDefaultDBSelectorID(String defaultDBSelectorID) {
		this.defaultDBSelectorID = defaultDBSelectorID;
	}

	public boolean isNeedIdInGroup() {
		return needIdInGroup;
	}

	public void setNeedIdInGroup(boolean needIdInGroup) {
		this.needIdInGroup = needIdInGroup;
	}

	public boolean isCompleteDistinct() {
		return completeDistinct;
	}

	public void setCompleteDistinct(boolean completeDistinct) {
		this.completeDistinct = completeDistinct;
	}

	public boolean isNewTypeRuleCalculate() {
		return newTypeRuleCalculate;
	}

	public void setNewTypeRuleCalculate(boolean newTypeRuleCalculate) {
		this.newTypeRuleCalculate = newTypeRuleCalculate;
	}
}
