package com.taobao.tddl.sqlobjecttree.mysql.function.datefunction;

import com.taobao.tddl.sqlobjecttree.common.value.OperationBeforTwoArgsFunction;

public class DateAdd extends OperationBeforTwoArgsFunction{
	public String getFuncName() {
		return "date_add";
	}
	
//	/**
//	 * Ĭ�ϳ��������һ������Ϊʱ��
//	 * �ڶ�������ΪInterval ����
//	 */
//	public Comparable<?> getVal(List<Object> args) {
//		Calendar cal=Calendar.getInstance();
//		cal.setTime((java.util.Date)arg1);
//		Interval temp=(Interval)arg2;
//		cal.add((Integer)temp.dateUnit.getVal(args), temp.expr);
//	    return cal;
//	}
}
