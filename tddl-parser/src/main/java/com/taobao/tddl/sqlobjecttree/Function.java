package com.taobao.tddl.sqlobjecttree;

import java.util.List;

import com.taobao.tddl.common.sqlobjecttree.Value;

public interface Function extends Value{
	public void setValue(List<Object> values);
	/**
	 * ����ں����е����������������������Ϊ�������׳��쳣
	 * @return	the column name in function,
	 * 		 null if no nestedColName
	 */
	public String getNestedColName();
}
