package com.taobao.tddl.interact.rule.bean;

import java.util.Calendar;

/**
 * ���ڴ����������ֺ��������ֶ�Ӧ��Calendar�������
 * �̳�Comparable����Ϊ��ʼԤ���Ľӿ���Comparable...
 * @author shenxun
 *
 */
@SuppressWarnings("rawtypes")
public class DateEnumerationParameter implements Comparable{
	/**
	 * Ĭ��ʹ��Date��Ϊ�������͵Ļ���������λ
	 * @param atomicIncreateNumber
	 */
	public DateEnumerationParameter(int atomicIncreateNumber) {
		this.atomicIncreatementNumber = atomicIncreateNumber;
		this.calendarFieldType = Calendar.DATE;
	}
	public DateEnumerationParameter(int atomicIncreateNumber,int calendarFieldType){
		this.atomicIncreatementNumber = atomicIncreateNumber;
		this.calendarFieldType = calendarFieldType;
	}
	public final int atomicIncreatementNumber;
	public final int calendarFieldType;
	public int compareTo(Object o) {
		throw new IllegalArgumentException("should not be here !");
	}
	
}
