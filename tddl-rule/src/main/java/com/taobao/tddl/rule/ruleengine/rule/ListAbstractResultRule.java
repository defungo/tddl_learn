package com.taobao.tddl.rule.ruleengine.rule;

import java.util.Map;
import java.util.Set;

import com.taobao.tddl.interact.bean.Field;
import com.taobao.tddl.interact.rule.bean.ExtraParameterContext;
import com.taobao.tddl.interact.rule.bean.SamplingField;
import com.taobao.tddl.interact.sqljep.Comparative;

public abstract class ListAbstractResultRule extends AbstractRule {
	/**
	 * �˷����Ѿ�δʹ�ã��滻�ķ������棬Ϊ�˵�Ԫ���Լ�����
	 * �����ֿ�
	 * 
	 * 
	 * @param sharedValueElementMap
	 * @return ���ص�map����Ϊnull,���п���Ϊ�յ�map�����map��Ϊ�գ����ڲ�����map�ض���Ϊ�ա����ٻ���һ��ֵ
	 */
/*	public abstract Map<String column , Field> eval(
			Map<String, Comparative> sharedValueElementMap);
*/
	/**
	 * �˷����Ѿ�δʹ�ã��滻�ķ������棬Ϊ�˵�Ԫ���Լ�����
	 * 
	 * �����ֱ������жԼ������ǰֵ�ĺ�����Դ��׷����Ϣ
	 * 
	 * @param enumeratedMap
	 *            ����->ö�� ��Ӧ��
	 * @param mappingTargetColumn
	 *            ӳ�������
	 * @param mappingKeys
	 *            ӳ�����ֵ
	 * 
	 * @return ������ֶΣ�����Ϊ�� ������෽����������setΪ��ʱ���쳣������Զ��׳�
	 */
/*	public abstract Map<String �����ֵ , Field> evalElement(
			Map<String, Set<Object>> enumeratedMap);*/

	// public abstract Set<String> evalWithoutSourceTrace(Map<String,
	// Set<Object>> enumeratedMap);

	public abstract Map<String/* column */, Field> eval(
			Map<String, Comparative> sharedValueElementMap,
			ExtraParameterContext extraParameterContext);

	public abstract Map<String/* �����ֵ */, Field> evalElement(
			Map<String, Set<Object>> enumeratedMap,
			ExtraParameterContext extraParameterContext);
	
	/**
	 * �õ�������ֵ
	 * @param argumentsMap
	 * @return
	 */
	public abstract Map<String, Set<Object>> prepareEnumeratedMap(
			Map<String, Comparative> argumentsMap);
	
	/**
	 * ��Զ�column,��value�Ĺ������
	 * @param samplingField
	 * @param extraParameterContext
	 * @return
	 */
	public abstract ResultAndMappingKey evalueateSamplingField(
			SamplingField samplingField,ExtraParameterContext extraParameterContext);
	
	/**
	 * ��Ե�column,��value�Ĺ������
	 * @param column
	 * @param value
	 * @param extraParameterContext
	 * @return
	 */
	public abstract ResultAndMappingKey evalueateSimpleColumAndValue(
			String column,Object value,
			ExtraParameterContext extraParameterContext);
}
