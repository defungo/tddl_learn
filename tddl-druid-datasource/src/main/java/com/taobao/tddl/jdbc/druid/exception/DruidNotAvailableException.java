package com.taobao.tddl.jdbc.druid.exception;

import java.sql.SQLException;

/**
 * Atom��ͨ��ExceptionSorter��⵽����Դ������ʱ�׳���
 * �������ݿⲻ���ã�ͬʱû��trylock�����Ի���ʱҲ�׳�
 * ����group������
 * 
 * 
 * @author linxuan
 *
 */
public class DruidNotAvailableException extends SQLException {
	private static final long serialVersionUID = 1L;

	public DruidNotAvailableException() {
		super();
	}

	public DruidNotAvailableException(String msg) {
		super(msg);
	}

	public DruidNotAvailableException(String reason, String SQLState) {
		super(reason, SQLState);
	}

	public DruidNotAvailableException(String reason, String SQLState, int vendorCode) {
		super(reason, SQLState, vendorCode);
	}

}
