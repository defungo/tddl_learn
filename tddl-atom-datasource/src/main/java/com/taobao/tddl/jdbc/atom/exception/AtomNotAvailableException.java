package com.taobao.tddl.jdbc.atom.exception;

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
public class AtomNotAvailableException extends SQLException {
	private static final long serialVersionUID = 1L;

	public AtomNotAvailableException() {
		super();
	}

	public AtomNotAvailableException(String msg) {
		super(msg);
	}

	public AtomNotAvailableException(String reason, String SQLState) {
		super(reason, SQLState);
	}

	public AtomNotAvailableException(String reason, String SQLState, int vendorCode) {
		super(reason, SQLState, vendorCode);
	}

}
