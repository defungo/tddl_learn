package com.taobao.tddl.common.jdbc.conurl;

import com.alibaba.common.lang.StringUtil;
import com.taobao.tddl.interact.rule.bean.DBType;

/**���ӵ�ַ��������Ҫͨ��ConnectionURLParser �������þ���Ķ���
 * @author qihao
 *
 */
public abstract class ConnectionURL {
	private String ip;

	private String port;

	private String dbName;

	public abstract DBType getDbType();

	public abstract String renderURL();

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = StringUtil.trim(ip);
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = StringUtil.trim(port);
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName =  StringUtil.trim(dbName);
	}
}
