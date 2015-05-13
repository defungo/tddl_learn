package com.taobao.tddl.common;

/**
 * @author huali
 * 
 * ���ݿ�������ֻ����Insert����Update
 * �������������ͣ������ı����������ֶ����ƺ�����ֵ
 * ������ֵ��long����String���͡�
 */
public class SyncCommand {
	public static enum TYPE {
		UPDATE,
		INSERT
	};
	
	/**
	 * �������ͣ�Update����Insert
	 */
	private TYPE type;
	
	/**
	 * ���ݿ�����
	 */
	private String dbName;

	/**
	 * �������ı������
	 */
	private String tableName;
	
	/**
	 * ������������
	 */
	private String columnName;
	
	/**
	 *������ֵ��������long����String 
	 */
	private Object value;

	public TYPE getType() {
		return type;
	}

	public String getTableName() {
		return tableName;
	}

	public String getColumnName() {
		return columnName;
	}

	public Object getValue() {
		return value;
	}

	public void setType(TYPE type) {
		this.type = type;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public void setValue(Object value) {
		this.value = value;
	}
}
