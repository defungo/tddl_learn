package com.taobao.tddl.rule.ruleengine.entities.abstractentities;

import java.util.List;

import com.taobao.tddl.rule.ruleengine.entities.convientobjectmaker.TableMapProvider;
import com.taobao.tddl.rule.ruleengine.rule.ListAbstractResultRule;

/**
 * ����properties����������
 * 
 * �������һ��һ�Զ�Ľڵ㣬��ȻҲ����logicTable�ڵ㣬��ôʹ�õ�ǰ�ӿڵĽ����
 * 
 * ���ø���ǰ�ӿڵ����ݻᱻ��ɢ���ӽڵ㡣
 * 
 * ����ӽڵ㱾��Ҳ�ж�Ӧ�����ԣ����ӽڵ����Ի�����һ�Զഫ�ݹ��������ԡ�
 * 
 * @author shenxun
 *
 */
public interface TablePropertiesSetter {
	public void setTableMapProvider(TableMapProvider tableMapProvider);
	public void setTableRule(List<ListAbstractResultRule> tableRule) ;
	public void setLogicTableName(String logicTable);
	public void setTableRuleChain(RuleChain ruleChain);
}
