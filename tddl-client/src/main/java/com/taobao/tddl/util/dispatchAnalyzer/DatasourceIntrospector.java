package com.taobao.tddl.util.dispatchAnalyzer;

import javax.sql.DataSource;

import com.taobao.tddl.client.dispatcher.DatabaseChoicer;
import com.taobao.tddl.client.jdbc.TDataSource;


public class DatasourceIntrospector {
	TDataSource targetDataSource;

	public DataSource getTargetDataSource() {
		return targetDataSource;
	}

	public void setTargetDataSource(DataSource targetDataSource) {
		try {
			this.targetDataSource = (TDataSource) targetDataSource;
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("��ʡ��Ҫ�����ʹ��TDataSource�ſ���ʹ��",e);
		}
	}
	public DatabaseChoicer getDatabaseChoicer(boolean isWrite) {
		if(isWrite){
			return getDatabaseChoicer(false, null);
		}else{
			return getDatabaseChoicer(false, null);
		}
	}
	public  DatabaseChoicer getDatabaseChoicer(boolean isWrite,String key){
		if(key == null){
			return targetDataSource.getDefaultDispatcher();
		}else{
			return targetDataSource.getDispatcherMap().get(key);
		}
	}
	

	
}
