package com.taobao.tddl.rule.ruleengine.entities.abstractentities;

import java.util.List;

import com.taobao.tddl.rule.ruleengine.rule.ListAbstractResultRule;

public interface TableListResultRuleContainer {
	/**
	 * ��ȫ�ֱ�������ø������������
	 * ������óɹ��򷵻�true;
	 * �������ʧ���򷵻�false;
	 * 
	 * @param listResultRule
	 * @return
	 */
	public boolean setTableListResultRule(List<ListAbstractResultRule> listResultRule);
}
