package com.taobao.tddl.sqlobjecttree.common;

public class ComparableElement {
	public ComparableElement(Comparable<?> comp,boolean isAnd,int operator) {
		this.comp=comp;
		this.isAnd=isAnd;
		this.operator=operator;
	}
	public int  operator;
	public Comparable<?> comp;
	/**
	 * �������Ŀǰֻ����������ʾin��ʱ����false,����ʱ����true.or���ʽ����Or���ʽ���Լ�������
	 */
	public boolean isAnd;
}
