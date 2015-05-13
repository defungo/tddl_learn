package com.taobao.tddl.client.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;

import com.taobao.tddl.interact.rule.bean.SqlType;

public class DefaultSqlExecuteEvent implements SqlExecuteEvent {
	private SqlType sqlType;
	private String logicTableName;
	private String primaryKeyColumn;
	private Object primaryKeyValue;
	private String databaseShardColumn;
	private Object databaseShardValue;
	private String tableShardColumn;
	private Object tableShardValue;
	private JdbcTemplate syncLogJdbcTemplate;
	private String syncLogId;
	private String syncLogDsKey;
	private boolean replicated;
	private long afterMainDBSqlExecuteTime;
	private String sql;
	private String masterColumns;
	
	public SqlType getSqlType() {
		return sqlType;
	}

	public void setSqlType(SqlType sqlType) {
		this.sqlType = sqlType;
	}

	public String getLogicTableName() {
		return logicTableName;
	}

	public void setLogicTableName(String logicTableName) {
		this.logicTableName = logicTableName;
	}

	public String getPrimaryKeyColumn() {
		return primaryKeyColumn;
	}

	public void setPrimaryKeyColumn(String primaryKeyColumn) {
		this.primaryKeyColumn = primaryKeyColumn;
	}

	public Object getPrimaryKeyValue() {
		return primaryKeyValue;
	}

	public void setPrimaryKeyValue(Object primaryKeyValue) {
		this.primaryKeyValue = primaryKeyValue;
	}

	public String getDatabaseShardColumn() {
		return databaseShardColumn;
	}

	public void setDatabaseShardColumn(String databaseShardColumn) {
		this.databaseShardColumn = databaseShardColumn;
	}

	public Object getDatabaseShardValue() {
		return databaseShardValue;
	}

	public void setDatabaseShardValue(Object databaseShardValue) {
		this.databaseShardValue = databaseShardValue;
	}

	public String getTableShardColumn() {
		return tableShardColumn;
	}

	public void setTableShardColumn(String tableShardColumn) {
		this.tableShardColumn = tableShardColumn;
	}

	public Object getTableShardValue() {
		return tableShardValue;
	}

	public void setTableShardValue(Object tableShardValue) {
		this.tableShardValue = tableShardValue;
	}

	public JdbcTemplate getSyncLogJdbcTemplate() {
		return syncLogJdbcTemplate;
	}

	public void setSyncLogJdbcTemplate(JdbcTemplate syncLogJdbcTemplate) {
		this.syncLogJdbcTemplate = syncLogJdbcTemplate;
	}

	public String getSyncLogId() {
		return syncLogId;
	}

	public void setSyncLogId(String syncLogId) {
		this.syncLogId = syncLogId;
	}

	public boolean isReplicated() {
		return replicated;
	}

	public void setReplicated(boolean replicated) {
		this.replicated = replicated;
	}

	public long getAfterMainDBSqlExecuteTime() {
		return afterMainDBSqlExecuteTime;
	}

	public void setAfterMainDBSqlExecuteTime(long afterMainDBSqlExecuteTime) {
		this.afterMainDBSqlExecuteTime = afterMainDBSqlExecuteTime;
	}
	public String getSql() {
		return sql;
	}
	public void setSql(String sql) {
		this.sql = sql;
	}

	public String getMasterColumns() {
		return masterColumns;
	}

	public void setMasterColumns(String masterColumns) {
		this.masterColumns = masterColumns;
	}

	public void setSyncLogDsKey(String dsKey) {
		this.syncLogDsKey = dsKey;
	}

	public String getSyncLogDsKey() {
		return syncLogDsKey;
	}
}
