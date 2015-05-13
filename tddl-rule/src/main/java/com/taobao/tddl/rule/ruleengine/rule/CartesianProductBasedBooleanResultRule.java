//package com.taobao.tddl.rule.ruleengine.rule;
//
//import java.util.Map;
//import java.util.Set;
//
//import com.taobao.tddl.rule.ruleengine.cache.SharedValueElement;
//import com.taobao.tddl.rule.ruleengine.cartesianproductcalculator.CartesianProductCalculator;
//import com.taobao.tddl.rule.ruleengine.cartesianproductcalculator.SamplingField;
//
//
//public abstract class CartesianProductBasedBooleanResultRule extends BooleanAbstractResultRule{
//	
//	/**
//	 * �Ƿ���Ҫ�Խ����ڵ�����ȡ������
//	 */
//	private boolean needMergeValueInCloseInterval = false;
//	
//	//TODO:boolean�����ԭ����ҲӦ�÷�Ϊ�������裬��һ���������㺯�����ڶ��������Ǿۺ�����
//	public boolean eval(Map<String,SharedValueElement> sharedValueElementMap){
//		
//		Map<String, Set<Object>> enumeratedMap = CartesianProductUtils.getSamplingField(sharedValueElementMap, needMergeValueInCloseInterval);
//		CartesianProductCalculator cartiesianProductCalculator = new CartesianProductCalculator(
//				enumeratedMap);
//	
//		for(SamplingField samplingField:cartiesianProductCalculator){
//			//ö�ٵѿ����������ÿһ��ֵ���������㣬���Ϊtrue��ֱ�ӷ���
//			boolean isTrue = evalueateSamplingField(samplingField);
//			if(isTrue){
//				return true;
//			}
//		}
//		return false;
//	}
//	/**
//	 * ����һ������������һ�����
//	 * @return
//	 */
//	public abstract boolean evalueateSamplingField(SamplingField samplingField);
//	public boolean isNeedMergeValueInCloseInterval() {
//		return needMergeValueInCloseInterval;
//	}
//	public void setNeedMergeValueInCloseInterval(
//			boolean needMergeValueInCloseInterval) {
//		this.needMergeValueInCloseInterval = needMergeValueInCloseInterval;
//	}
//	
//}
