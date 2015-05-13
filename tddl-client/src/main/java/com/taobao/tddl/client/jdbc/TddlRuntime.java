package com.taobao.tddl.client.jdbc;

import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

/**
 * ����ΪFinal�ֻ࣬���ؽ��������޸�
 * 
 * ��һ�׶Σ�ֻ֧������Դ�Ķ�̬����֧�ֹ����dbindex�Ķ�̬
 * 
 * @author linxuan
 * 
 */
public class TddlRuntime {
	public final Map<String, DataSource> dsMap;

	public TddlRuntime(Map<String, DataSource> datasourceMap) {
		this.dsMap = Collections.unmodifiableMap(datasourceMap);
	}
}
