package com.taobao.tddl.common.exception.checked;

public class CantFindPositionByParamException extends TDLCheckedExcption {
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 3682437768303903330L;

	public CantFindPositionByParamException(String param) {
		super("���ܸ���"+param+"�����ҵ����Ӧ��λ�ã���ע��ֱ����֧����Ϲ����벻Ҫʹ����Ϲ��������зֱ��ѯ");
	}
}
