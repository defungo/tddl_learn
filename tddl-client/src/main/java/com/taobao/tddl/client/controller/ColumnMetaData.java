package com.taobao.tddl.client.controller;

import com.taobao.tddl.interact.sqljep.Comparative;

public class ColumnMetaData {
	/**
	 * ָ���������ֶ�
	 */
	public final String key;
	/**
	 * �������ֶεĶ�ӦComparative
	 */
	public final  Comparative value;
	public ColumnMetaData(String key,Comparative value) {
		this.key=key;
		this.value=value;
	}
}
