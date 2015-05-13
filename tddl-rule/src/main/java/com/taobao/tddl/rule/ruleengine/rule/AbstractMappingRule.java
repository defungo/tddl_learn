package com.taobao.tddl.rule.ruleengine.rule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.interact.rule.bean.AdvancedParameter;
import com.taobao.tddl.interact.rule.bean.ExtraParameterContext;
import com.taobao.tddl.interact.rule.bean.SamplingField;
import com.taobao.tddl.rule.groovy.GroovyListRuleEngine;

/**
 * ���󷽷������ڶԶ�mapping�ĳ���
 * ���Ƚ���ö�٣�Ȼ��ѿ��������õ������
 * ��Ҫ��ӳ��Ķ����Ժ󣬵���get��������ӳ�䡣
 * Ȼ��ӳ��Ľ������targetRule��������
 * 
 * @author shenxun
 *
 */
public abstract class AbstractMappingRule extends CartesianProductBasedListResultRule{
//	protected CartesianProductBasedListResultRule targetRule;
	Log logger = LogFactory.getLog(AbstractMappingRule.class);
	/**
	 * ת���Ժ��Ŀ�����
	 */
	protected GroovyListRuleEngine targetRule = new GroovyListRuleEngine();
	/**
	 * ת�����Ŀ������
	 */
	private String targetKey = null;
	
	/* (non-Javadoc)
	 * @see com.taobao.tddl.rule.ruleengine.rule.CartesianProductBasedListResultRule#evalueateSamplingField(com.taobao.tddl.rule.ruleengine.cartesianproductcalculator.SamplingField)
	 * 
	 * ������ͨ��ӳ������������ؽ����ӳ����������testCase,��Ӧ�ڷֿ�ʱȡ��������߼���
	 * @Test void com.taobao.tddl.rule.intergration.TairBasedMappingRuleIntegrationTest.test_����ӳ��_2������_��tair_��tair�Ժ�ὫtargetKeyҲ��¼����_����targtKey��targetValueSet��()
	 * 
	 * 
	 * 
	 */
	@Override
	public ResultAndMappingKey evalueateSamplingField(SamplingField samplingField,
			ExtraParameterContext extraParameterContext) {
		
		List<String> columns = samplingField.getColumns();
		List<Object> enumFields = samplingField.getEnumFields();
		if(columns != null&& columns.size() == 1){
			//ӳ���Ժ������
			
			Object target = null;
			if(samplingField.getMappingValue() != null&&samplingField.getMappingTargetKey().equals(targetKey)){
				//��ȡ��ӳ��ֵ��Ϊ�գ�����targetKey = targetKey.���ʾ�����Ѿ����ֿ�ȡ�����������ÿ��Ա��ֱ���ʹ�á�
				target = samplingField.getMappingValue();
			}else{
				target = get(targetKey,columns.get(0),enumFields.get(0));
			}
			if(target == null){
				logger.debug("target value is null");
				return null;
			}
			Map<String/*target column*/, Object/*target value*/> argumentMap = new HashMap<String, Object>(1);
			
			argumentMap.put(targetKey, target);
			logger.debug("invoke target rule ,value is "+target);
			Object[] args = new Object[] { argumentMap, extraParameterContext };
			//���������ֵ �����в�ѯ
			String resultString = targetRule.imvokeMethod(args);
			ResultAndMappingKey result = null;
			if(resultString != null){
				//���ع����������Ӧ��mapping key
				result = new ResultAndMappingKey(resultString);
				result.mappingKey = target;
                result.mappingTargetColumn = targetKey;
			}else{
				//���Ϊ�����׳��쳣�����ӳ��û��ȡ��targetValue��������������顣
				throw new IllegalArgumentException("��������Ľ������Ϊnull");
			}
			return result;
		}else{
			throw new IllegalStateException("��������Ҫ��:columns:"+columns);
		}
	}

	
	@Override
	protected boolean ruleRequireThrowRuntimeExceptionWhenSetIsEmpty() {
		//��mapping rule�У���Ҫ��Ϊ�մ���ʱ���׳��쳣
		return true;
	}
	
	/**
	 * ����sourceKey��sourceValue��ȡ ����targerRule��Ĳ�����targetValue
	 * 
	 * @param sourceKey
	 * @param sourceValue
	 * @return
	 */
	protected abstract Object get(String targetKey ,String sourceKey,Object sourceValue);

	public CartesianProductBasedListResultRule getTargetRule() {
		return targetRule;
	}

	protected void initInternal() {
		if (targetRule == null) {
			throw new IllegalArgumentException("target rule is null");
		}
		// ����Ŀ�����
		targetRule.initRule();
		// �ӽ������Ŀ��������õ���ǰ����
		Set<AdvancedParameter> advancedParameters = targetRule.getParameters();
		if (advancedParameters.size() != 1) {
			throw new IllegalArgumentException("Ŀ�����Ĳ�������Ϊ1��������ʹ��" + "ӳ�����");
		}
		// ȷ�ϲ���Ψһ�Ժ�ȡ���ò���
		AdvancedParameter advancedParameter = advancedParameters.iterator()
				.next();
		targetKey = advancedParameter.key;
		if (targetKey == null || targetKey.length() == 0) {
			throw new IllegalArgumentException("target key is null .");
		}
		StringBuilder sb = new StringBuilder();
		sb.append("parse mapping rule , target rule is ").append(targetRule)
				.append("target target key is ").append(targetKey);
		logger.debug(sb.toString());
	}


	@Override
	/**
	 * ������ ��ӳ���Ӧ���ߵĹ�����ɶ
	 */
	public void setExpression(String expression) {
		targetRule.setExpression(expression);
	}
	
}
