package com.taobao.tddl.rule.ruleengine.rule;

/**
 * ��Ϊ�����Ժ���õ����������ӳ���������Զ��õ�һ��mappingKey
 * 
 * @author shenxun
 *
 */
public class ResultAndMappingKey {
	public ResultAndMappingKey(String result) {
		this.result = result;
	}
	
	public final String result;

	Object mappingKey;

	String mappingTargetColumn;
	
}
