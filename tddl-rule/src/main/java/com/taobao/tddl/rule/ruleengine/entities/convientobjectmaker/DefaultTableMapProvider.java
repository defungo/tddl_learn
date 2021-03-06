package com.taobao.tddl.rule.ruleengine.entities.convientobjectmaker;

import java.util.HashMap;
import java.util.Map;

import com.taobao.tddl.rule.bean.Table;
import com.taobao.tddl.rule.ruleengine.entities.abstractentities.SharedElement;

public class DefaultTableMapProvider implements TableMapProvider{
	String logicTable;
	public Map<String, SharedElement> getTablesMap() {
		Table table = new Table();
		if(logicTable == null){
			throw new IllegalArgumentException("没有表名生成因子");
		}
		table.setTableName(logicTable);
		Map<String, SharedElement> returnMap = new HashMap<String, SharedElement>();
		returnMap.put("0", table);
		return returnMap;
	}
	public void setLogicTable(String logicTable) {
		this.logicTable = logicTable;
		
	}
	public void setParentID(String parentID) {
		//do nothing
		
	}
}
