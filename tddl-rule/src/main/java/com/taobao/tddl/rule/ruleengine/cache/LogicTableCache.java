//package com.taobao.tddl.rule.ruleengine.cache;
//
//import java.util.Set;
//
//import com.taobao.tddl.rule.ruleengine.entities.abstractentities.RuleChain;
//
///**
// * �߼�����һЩ��Ҫcache������
// * @author shenxun
// *
// */
//public class LogicTableCache {
//	private  boolean isModifiable = true;
//	
//	public Set<RuleChain> getRuleChain() {
//		return ruleChain;
//	}
//	public void setRuleChain(Set<RuleChain> ruleChain) {
//		if(isModifiable)
//			this.ruleChain=ruleChain;
//		else
//			throw new IllegalArgumentException("�������޸�");
//	}
//	public boolean isModifiable() {
//		return isModifiable;
//	}
//	public void setModifiable(boolean isModifiable) {
//		this.isModifiable = isModifiable;
//	}
//	
//	
//	
//}
