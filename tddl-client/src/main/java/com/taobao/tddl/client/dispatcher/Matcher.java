package com.taobao.tddl.client.dispatcher;

import java.util.List;

import com.taobao.tddl.interact.bean.ComparativeMapChoicer;
import com.taobao.tddl.interact.bean.MatcherResult;
import com.taobao.tddl.rule.LogicTableRule;

/**
 * ƥ������õĽ�ڣ��Ὣsql������Ľ���������Լ��������ƥ��
 * @author shenxun
 * @author junyu
 */
public interface Matcher {
	/**
	 * ����SqlParserResult pr + List<Object> args����Ҫ�����һ����С�Ķ���/�ӿ�
	 * ����ҵ��ͨ��ThreadLocal��ʽ�ƹ�������ֱ��ָ��
	 */
	MatcherResult match(ComparativeMapChoicer comparativeMapChoicer, List<Object> args, LogicTableRule rule) ;

	/**
	 * ָ����������������㵥������������
	 * @param useNewTypeRuleCalculate
	 * @param comparativeMapChoicer
	 * @param args
	 * @param rule
	 * @return
	 */
	MatcherResult match(boolean useNewTypeRuleCalculate,ComparativeMapChoicer comparativeMapChoicer, List<Object> args, LogicTableRule rule);

	/**
	 * ָ����������������㵥������������,����ָ���Ƿ���Ҫ�ṩ���ֵ�Ͳ����Ķ�Ӧ��ϵ(sourceKey)
	 * @param useNewTypeRuleCalculate
	 * @param comparativeMapChoicer
	 * @param args
	 * @param rule
	 * @return
	 */
	MatcherResult match(boolean useNewTypeRuleCalculate,boolean needSourceKey,ComparativeMapChoicer comparativeMapChoicer, List<Object> args, LogicTableRule rule);
}
