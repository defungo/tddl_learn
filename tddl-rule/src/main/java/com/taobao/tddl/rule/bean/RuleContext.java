//Copyright(c) Taobao.com
package com.taobao.tddl.rule.bean;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.taobao.tddl.interact.bean.Field;
import com.taobao.tddl.rule.LogicTableRule;
import com.taobao.tddl.rule.ruleengine.entities.abstractentities.RuleChain;
import com.taobao.tddl.rule.ruleengine.entities.inputvalue.CalculationContextInternal;

/**
 * @description ���������ڲ�context,��Ҫ��һЩһ��sql������ԭ����Ҫ����ظ����㵫����ı�Ľ��,�Լ�
 *              �����ͱ�ʱ����Ҫ��һЩ������Ϣ�͹�����Ϣ.
 * 
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 2.4.3
 * @since 1.6
 * @date 2010-12-03����01:14:52
 */
public class RuleContext {
	/**
	 * �������е�TableRule
	 */
	protected LogicTableRule rule=null;
	
	/**
	 * �Ƿ���ҪsourceKey
	 * Ĭ�ϲ���Ҫ,join��ʹ��id in������Ҫ
	 */
	protected boolean needSourceKey=false;
	
	/**
	 * �����������
	 */
	protected Map<RuleChain, CalculationContextInternal/* ������Ľ�� */> calContextMap;

	/**
	 * �Ƿ��ǵ�һ�μ����
	 */
	protected boolean firstTableCalculate=true;
	
	/**
	 * �Ѿ�ö�ٹ��ı����
	 */
	protected Map<String,Set<Object>> tabArgsMap;
	
	/**
	 * �����û��ʱ��sourceKey Map
	 */
	protected Map<String, Field> tabSourceWithNoRule;
	
	/**
	 * �ֿ�ֱ������
	 */
	protected List<String> dbAndTabWithSameColumn;

	/**
	 * ���û�н�������ʱ,����������
	 */
	protected Map<String,Field> tabResultSet;
	
	public LogicTableRule getRule() {
		return rule;
	}

	public void setRule(LogicTableRule rule) {
		this.rule = rule;
	}

	public Map<RuleChain, CalculationContextInternal> getCalContextMap() {
		return calContextMap;
	}

	public void setCalContextMap(
			Map<RuleChain, CalculationContextInternal> calContextMap) {
		this.calContextMap = calContextMap;
	}

	public boolean isNeedSourceKey() {
		return needSourceKey;
	}

	public void setNeedSourceKey(boolean needSourceKey) {
		this.needSourceKey = needSourceKey;
	}
}
