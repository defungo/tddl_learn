/**
 * 
 */
package com.taobao.tddl.sqlobjecttree.oracle.function;

import java.util.List;

import com.taobao.tddl.sqlobjecttree.Utils;
import com.taobao.tddl.sqlobjecttree.common.value.ThreeArgsFunction;

/**
 * ֧��oracle��substr����
 * @author liang.chenl
 *
 */
public class SubStr extends ThreeArgsFunction{

	@Override
	public String getFuncName() {
		return "SUBSTR";
	}
	
	@Override
	public Comparable<?> getVal(List<Object> args) {
		Object obj = null;
		String returnStr;
		try {
			obj=Utils.getVal(args, arg1);
			String temp=(String)obj;
			returnStr = temp.substring(Integer.valueOf(arg2.toString()) , Integer.valueOf(arg3.toString()));
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("�����ת��������"+obj+"�ò�������ΪString");
		}
		return returnStr;
	}
}
