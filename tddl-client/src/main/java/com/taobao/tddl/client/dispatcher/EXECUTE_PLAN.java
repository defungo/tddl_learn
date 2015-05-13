package com.taobao.tddl.client.dispatcher;

/**
 * ִ�мƻ�����
 * 
 * ר��Ϊ���ж�����sql��ִ�����Զ���ơ�
 * @author shenxun
 *
 */
public enum EXECUTE_PLAN {
	SINGLE(1),
	MULTIPLE(2),
	NONE(0);
	private int i;
	private EXECUTE_PLAN(int i ){
		this.i = i	;
	}
	public int value(){
		return this.i;
	}
	public static EXECUTE_PLAN valueOf(int i ){
		for(EXECUTE_PLAN p :values()){
			if(p.value() == i){
				return p;
			}
		}
		throw new IllegalArgumentException("Invalid Execute_plan"+ i );
	}
}
