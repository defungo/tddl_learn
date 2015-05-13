//Copyright(c) Taobao.com
package com.taobao.tddl.rule.le.bean;

import java.util.List;
import java.util.Map;

import com.taobao.tddl.interact.bean.Field;

/**
 * @description
 * @author <a href="junyu@taobao.com">junyu</a> 
 * @version 1.0
 * @since 1.6
 * @date 2011-3-29����02:22:04
 */
public class TargetDatabase {
	//Ŀ��dbKey
	private String dbIndex;
	//��Ҫִ�е�ʵ�ʱ�
	private List<String> tableNames;
	
	private Map<String, Field> tableNamesWithSourceKeys;
	
	public String getDbIndex() {
		return dbIndex;
	}
	public void setDbIndex(String dbIndex) {
		this.dbIndex = dbIndex;
	}
	public List<String> getTableNames() {
		return tableNames;
	}
	public void setTableNames(List<String> tableNames) {
		this.tableNames = tableNames;
	}
	public Map<String, Field> getTableNamesWithSourceKeys() {
		return tableNamesWithSourceKeys;
	}
	public void setTableNamesWithSourceKeys(
			Map<String, Field> tableNamesWithSourceKeys) {
		this.tableNamesWithSourceKeys = tableNamesWithSourceKeys;
	}
}
