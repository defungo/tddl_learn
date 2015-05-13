package com.taobao.tddl.jdbc.druid.exception;

import java.sql.SQLException;

/**
 * ��ȡ����ʱ���ж�Ϊ��ʱ�ͷ��ڣ��׳����쳣
 * 
 * @author linxuan
 *
 */
public class DruidSlowPunishException extends SQLException {
	private static final long serialVersionUID = 1L;

	public DruidSlowPunishException() {
		super();
	}

	public DruidSlowPunishException(String msg) {
		super(msg);
	}

	public DruidSlowPunishException(String reason, String SQLState) {
		super(reason, SQLState);
	}

	public DruidSlowPunishException(String reason, String SQLState, int vendorCode) {
		super(reason, SQLState, vendorCode);
	}

}
