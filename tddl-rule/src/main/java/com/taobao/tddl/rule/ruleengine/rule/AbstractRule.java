package com.taobao.tddl.rule.ruleengine.rule;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.interact.rule.bean.AdvancedParameter;

/**
 * ������ܳ��� �����ɲ����ͱ��ʽ���
 * 
 * @author shenxun
 * @author junyu
 */
public abstract class AbstractRule {
	Log log = LogFactory.getLog(AbstractRule.class);
	/**
	 * ��ǰ������Ҫ�õ��Ĳ���
	 */
	protected Set<AdvancedParameter> parameters;

	private boolean inited = false;

	/**
	 * ��ǰ������Ҫ�õ��ı��ʽ
	 */
	protected String expression;
	
	/**
	 * �û��Զ���jar��package
	 */
	protected String extraPackagesStr;

	/*
	 * ͨ���������ṹ�������ܼ�����Ӵ�������set�������У���Ҫע����� ����ж��ֵ������һ������ʽ�����ַ����ǲ�׼ȷ�ģ���ʱ�����ͨ�������ļ�
	 * �ֶ��������������ÿһ����������Ĳ����ĵ��Ӵ�����
	 * 
	 * ���ڻ�û���ã���Ϊ�Ƚϸ���
	 * 
	 * @param cumulativeTimes
	 * 
	 * public void setCumulativeTimes(int cumulativeTimes){
	 * for(KeyAndAtomIncValue keyAndAtomIncValue :parameters){
	 * if(keyAndAtomIncValue.cumulativeTimes == null){
	 * keyAndAtomIncValue.cumulativeTimes = cumulativeTimes; } } }
	 */
	protected abstract void initInternal();

	/**
	 * ȷ������ֻ��ʼ��һ��
	 */
	public void initRule() {
		if (inited) {
			log.debug("rule has inited");
		} else {
			initInternal();
			inited = true;
		}
	}

	/**
	 * springע�����Ĭ�������ֶε�ֵ,�Ὣ����ֵ��ΪСд
	 * 
	 * @param parameters
	 */
	public void setParameters(Set<String> parameters) {
		if (this.parameters == null) {
			this.parameters = new HashSet<AdvancedParameter>();
		}
		for (String str : parameters) {
			AdvancedParameter advancedParameter = AdvancedParameter.getAdvancedParamByParamTokenNew(str,false);
			this.parameters.add(advancedParameter);
		}
	}

	/**
	 * Springע����
	 * 
	 * @param parameters
	 */
	public void setAdvancedParameter(Set<AdvancedParameter> parameters) {
		if (this.parameters == null) {
			this.parameters = new HashSet<AdvancedParameter>();
		}
		for (AdvancedParameter keyAndAtomIncValue : parameters) {
			this.parameters.add(keyAndAtomIncValue);
		}
	}

	/**
	 * springע��һ��
	 * 
	 * @param parameter
	 */
	public void setAdvancedParameter(AdvancedParameter parameter) {
		if (this.parameters == null) {
			this.parameters = new HashSet<AdvancedParameter>();
		}
		this.parameters.add(parameter);
	}
	
	public Set<AdvancedParameter> getParameters() {
		return parameters;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		if (expression != null)
			this.expression = expression;
	}

	public void setExtraPackagesStr(String extraPackagesStr) {
		this.extraPackagesStr = extraPackagesStr;
	}

	/**
	 * col,1,7|col1,1,7....
	 * 
	 * @param parameterArray
	 */
	public void setParameter(String parameterArray) {
		if (parameterArray != null && parameterArray.length() != 0) {
			String[] paramArray = parameterArray.split("\\|");
			Set<String> paramSet = new HashSet<String>(Arrays
					.asList(paramArray));
			this.setParameters(paramSet);
		}
	}
}
