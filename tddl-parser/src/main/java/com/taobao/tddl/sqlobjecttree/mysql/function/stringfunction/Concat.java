package com.taobao.tddl.sqlobjecttree.mysql.function.stringfunction;

import com.taobao.tddl.sqlobjecttree.common.value.OperationBeforTwoArgsFunction;

public class Concat extends OperationBeforTwoArgsFunction {
	//��ӵģ���Ϊ�и��汾V810û�кϲ�������
	@Override
	public String getFuncName() {
		return "CONCAT";
	}
}