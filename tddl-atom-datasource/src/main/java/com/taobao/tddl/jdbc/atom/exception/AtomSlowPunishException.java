package com.taobao.tddl.jdbc.atom.exception;

import java.sql.SQLException;

/**
 * ��ȡ����ʱ���ж�Ϊ��ʱ�ͷ��ڣ��׳����쳣
 * 
 * @author linxuan
 *
 */
public class AtomSlowPunishException extends SQLException {
	private static final long serialVersionUID = 1L;

	public AtomSlowPunishException() {
		super();
	}

	public AtomSlowPunishException(String msg) {
		super(msg);
	}

	public AtomSlowPunishException(String reason, String SQLState) {
		super(reason, SQLState);
	}

	public AtomSlowPunishException(String reason, String SQLState, int vendorCode) {
		super(reason, SQLState, vendorCode);
	}

}
