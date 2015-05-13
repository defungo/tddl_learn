package com.taobao.tddl.sqlobjecttree.traversalAction;

import com.taobao.tddl.sqlobjecttree.DMLCommon;

public class TraversalSQLEvent {
	public enum StatementType{
		/**
		 * ����sql���Ա��� 
		 */
		TABLE,
		/**
		 * ����sql����where����
		 */
		WHERE
		,
		/**
		 * ����������ײ�
		 */
		NORMAL
	}
	private final DMLCommon currStatement;
	private final StatementType type;
	public TraversalSQLEvent(StatementType type,DMLCommon currentStatement) {
		this.type=type;
		this.currStatement=currentStatement;
	}
	public StatementType getType() {
		return type;
	}

	public DMLCommon getCurrStatement() {
		return currStatement;
	}
}
