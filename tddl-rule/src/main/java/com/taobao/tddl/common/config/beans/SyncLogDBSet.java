package com.taobao.tddl.common.config.beans;

import java.util.Map;

public class SyncLogDBSet {
	private Map<String/*���ݿ��ڲ�����*/, Datasource> synclogDatasources;
	private String synclogDatasourceWeight;

	/**
	 * ���߼���getter/setter
	 */


	/**
	 * ���߼���getter/setter
	 */
	public Map<String, Datasource> getSynclogDatasources() {
		return synclogDatasources;
	}
	public void setSynclogDatasources(Map<String, Datasource> synclogDatasources) {
		this.synclogDatasources = synclogDatasources;
	}
	public String getSynclogDatasourceWeight() {
		return synclogDatasourceWeight;
	}
	public void setSynclogDatasourceWeight(String synclogDatasourceWeight) {
		this.synclogDatasourceWeight = synclogDatasourceWeight;
	}
}
