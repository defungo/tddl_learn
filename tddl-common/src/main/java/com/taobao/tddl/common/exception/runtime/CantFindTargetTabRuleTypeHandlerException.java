package com.taobao.tddl.common.exception.runtime;

public class CantFindTargetTabRuleTypeHandlerException extends TDLRunTimeException{

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -4073830327289870566L;

	public CantFindTargetTabRuleTypeHandlerException(String msg) {
		super("�޷��ҵ�"+msg+"��Ӧ�Ĵ�����");
	}
}
