package com.taobao.tddl.rule.ruleengine;

import java.util.List;
import java.util.Map;

import com.taobao.tddl.interact.bean.TargetDB;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.rule.ruleengine.entities.inputvalue.LogicTabMatrix;

public interface DatabaseRuleProcesser {
	/**
	 * �������������sql�зֿ�ֱ���Ϣ�ֶΣ��Լ������ļ�����ȡ�ֿ��Դ��Ϣ
	 * @param virtualTabName
	 * @param colMap
	 * @param logTabs
	 * @return
	 */
	public List<TargetDB> process(String virtualTabName,
			Map<String, Comparative> colMap,LogicTabMatrix logTabs);
}
