/**
 * 
 */
package com.taobao.tddl.rule.groovy;

import java.util.Map;

/**
 * groovy ����������,����֧���Զ����ֶ�
 * ������ֶο�����Ӧ�õ�springע��
 * 
 * @author liang.chenl
 *
 */
public class GroovyContextHelper {
	static private Map<String,Object> context;
	
	static public Map<String,Object> getContext() {
		return context;
	}
	
	static public void setContext(Map<String,Object> context) {
		GroovyContextHelper.context = context; 
	}
}
