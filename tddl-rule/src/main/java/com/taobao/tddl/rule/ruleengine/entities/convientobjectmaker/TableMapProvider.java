package com.taobao.tddl.rule.ruleengine.entities.convientobjectmaker;

import java.util.Map;

import com.taobao.tddl.rule.ruleengine.entities.abstractentities.SharedElement;

/**
 * 
 * ���ڴ���database�г��ж�����map
 * 
 * @author shenxun
 *
 */
public interface TableMapProvider {
	public Map<String, SharedElement> getTablesMap();
	public void setParentID(String parentID) ;
	public void setLogicTable(String logicTable);
}
