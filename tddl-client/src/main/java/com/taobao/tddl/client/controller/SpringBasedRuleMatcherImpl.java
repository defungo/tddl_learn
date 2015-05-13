package com.taobao.tddl.client.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.taobao.tddl.client.dispatcher.Matcher;
import com.taobao.tddl.interact.bean.ComparativeMapChoicer;
import com.taobao.tddl.interact.bean.MatcherResult;
import com.taobao.tddl.interact.bean.MatcherResultImp;
import com.taobao.tddl.interact.bean.TargetDB;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.rule.LogicTableRule;
import com.taobao.tddl.rule.bean.RuleContext;
import com.taobao.tddl.rule.ruleengine.entities.abstractentities.RuleChain;
import com.taobao.tddl.rule.ruleengine.entities.inputvalue.CalculationContextInternal;

public class SpringBasedRuleMatcherImpl implements Matcher {
    public MatcherResult match(ComparativeMapChoicer comparativeMapChoicer, List<Object> args, LogicTableRule rule){
		return this.match(false, comparativeMapChoicer, args, rule);
	}
	
    public MatcherResult match(boolean useNewTypeRuleCalculate,ComparativeMapChoicer comparativeMapChoicer, List<Object> args, LogicTableRule rule){
    	return this.match(false,false,comparativeMapChoicer, args, rule);
    }
    
	public MatcherResult match(boolean useNewTypeRuleCalculate,boolean needSourceKey,ComparativeMapChoicer comparativeMapChoicer,
			List<Object> args, LogicTableRule rule) {
		// �����������ϣ������˹��������й�����
		Set<RuleChain> ruleChainSet = rule.getRuleChainSet();
		// ����Ҫ������ݿ�ֿ��ֶκͶ�Ӧ��ֵ������ж����ô�������һ��
		Map<String, Comparative> comparativeMapDatabase = new HashMap<String, Comparative>(
				2);
		// ����Ҫ���talbe�ֱ��ֶκͶ�Ӧ��ֵ������ж����ô�������һ��
		Map<String, Comparative> comparativeTable = new HashMap<String, Comparative>(
				2);

		Map<RuleChain, CalculationContextInternal/* ������Ľ�� */> resultMap = new HashMap<RuleChain, CalculationContextInternal>(
				ruleChainSet.size());

		for (RuleChain ruleChain : ruleChainSet) {

			// ���ÿһ��������
			List<Set<String>/* ÿһ��������Ҫ�Ĳ��� */> requiredArgumentSortByLevel = ruleChain
					.getRequiredArgumentSortByLevel();
			/*
			 * ��ΪruleChain����ĸ�����һ���ģ�������getRequiredArgumentSortByLevel
			 * list��sizeһ���࣬��˲���Խ��
			 */
			int index = 0;

			for (Set<String> oneLevelArgument : requiredArgumentSortByLevel) {
				// ���ÿһ���������е�һ�����𣬼����Ǵӵ͵��ߵ����Ȳ鿴�Ƿ��������Ҫ������������������
				Map<String/* ��ǰ����Ҫ������� */, Comparative> sqlArgs = comparativeMapChoicer
						.getColumnsMap(args, oneLevelArgument);
				if (sqlArgs.size() == oneLevelArgument.size()) {
					// ��ʾƥ��,��������Ϊkey,valueΪ���
					resultMap.put(ruleChain, new CalculationContextInternal(
							ruleChain, index, sqlArgs));
					if (ruleChain.isDatabaseRuleChain()) {
						comparativeMapDatabase.putAll(sqlArgs);
					} else {
						// isTableRuleChain
						comparativeTable.putAll(sqlArgs);
					}
					break;
				} else {
					index++;
				}
			}
		}
		
		RuleContext innerContext=new RuleContext();
		innerContext.setCalContextMap(resultMap);
		innerContext.setRule(rule);
		innerContext.setNeedSourceKey(needSourceKey);
		
		List<TargetDB> calc = useOneTypeCalculate(useNewTypeRuleCalculate,innerContext);
		return new MatcherResultImp(calc, comparativeMapDatabase,
				comparativeTable);
	}

    /**
     * ��2�ּ�����Կ���ѡ,��һ�����ȼ�������ڼ����,�ڶ�����һ�������������Ͻ��б����
     * @param firstDb      ���Ϊtrue,ѡ���Ȱѿ�������,�ټ����
     * @param ruleContext  ��������ڲ�context.
     * @return
     */
	private List<TargetDB> useOneTypeCalculate(boolean useNewTypeRuleCalculate,RuleContext ruleContext){
		if(!useNewTypeRuleCalculate){
		    return ruleContext.getRule().calculate(ruleContext.getCalContextMap());
		}else{
	        return ruleContext.getRule().calculateNew(ruleContext);	
		}
	}
}
