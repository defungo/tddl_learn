package com.taobao.tddl.rule.ruleengine.entities.retvalue;

import java.util.List;

import com.taobao.tddl.interact.bean.TargetDB;

public class TargetDBMetaData {

	/**
	 * �Ƿ����������
	 */
	private  boolean allowReverseOutput;
	/**
	 * Ŀ���
	 */
	private final List<TargetDB> target;
	/**
	 * �Ƿ������и���
	 */
	private final boolean needRowCopy;
	/**
	 * �������
	 */
	private final String virtualTableName;
	public TargetDBMetaData(String virtualTableName,List<TargetDB> targetdbs,boolean needRowCopy,boolean allowReverseOutput) {
		this.virtualTableName=virtualTableName;
		this.target=targetdbs;
		this.needRowCopy=needRowCopy;
		this.allowReverseOutput = allowReverseOutput;
	}
	public List<TargetDB> getTarget() {
		return target;
	}
	public String getVirtualTableName() {
		return virtualTableName;
	}
	public boolean needRowCopy(){
		return needRowCopy;
	}
	public boolean allowReverseOutput(){
		return allowReverseOutput;
	}
	public void needAllowReverseOutput(boolean reverse){
		this.allowReverseOutput=reverse;
	}
}
