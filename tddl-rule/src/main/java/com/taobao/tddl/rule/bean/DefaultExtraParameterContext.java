package com.taobao.tddl.rule.bean;

import java.util.Map;

import com.taobao.tddl.interact.rule.bean.ExtraParameterContext;

/**
 * ExtraParameterContext��һ��Ĭ��ʵ����
 * �����������Map<Object,Object>���󣬷ֱ���dbMap,tabMap
 * �ɷֱ������洢db����صĲ���
 * ��tab����صĲ���
 * 
 * @author xudanhui.pt 2010-10-18,����11:33:44
 */
public class DefaultExtraParameterContext implements ExtraParameterContext {

	private Map<Object, Object> dbMap;

	private Map<Object, Object> tabMap;

	public Map<Object, Object> getDbMap() {
		return dbMap;
	}

	public void setDbMap(Map<Object, Object> dbMap) {
		this.dbMap = dbMap;
	}

	public Map<Object, Object> getTabMap() {
		return tabMap;
	}

	public void setTabMap(Map<Object, Object> tabMap) {
		this.tabMap = tabMap;
	}
}
