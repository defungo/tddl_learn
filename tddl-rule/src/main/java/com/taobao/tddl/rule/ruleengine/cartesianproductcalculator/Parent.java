package com.taobao.tddl.rule.ruleengine.cartesianproductcalculator;

/**
 * ԭ���Ұɣ�ʵ�ڲ�֪�����ĸ���
 * 
 * parent ��λʱ��ļ�����
 * @author shenxun
 *
 */
public interface Parent {
	/**
	 * ѯ�ʸ�����û��ֵ
	 * 
	 * @return
	 */
	public boolean parentHasNext();

	/**
	 * ֪ͨparent��λ�ķ���
	 */
	public void add();
}
