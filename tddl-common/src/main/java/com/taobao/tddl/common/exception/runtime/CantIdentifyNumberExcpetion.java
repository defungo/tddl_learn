package com.taobao.tddl.common.exception.runtime;

public class CantIdentifyNumberExcpetion extends TDLRunTimeException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 7861250013675710468L;
	public CantIdentifyNumberExcpetion(String input,String input1,Throwable e) {
		super("�ؼ��֣�"+input+"��"+input1+"����ʶ��Ϊһ�������������趨",e);
	}
}
