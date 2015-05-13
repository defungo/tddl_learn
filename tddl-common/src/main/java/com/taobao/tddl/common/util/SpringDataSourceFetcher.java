package com.taobao.tddl.common.util;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.taobao.tddl.interact.rule.bean.DBType;

/**
 * 
 * @author linxuan
 *
 */
public class SpringDataSourceFetcher implements DataSourceFetcher, ApplicationContextAware {

	private ApplicationContext springContext; // �õ�������
	private DBType dbType = DBType.MYSQL;

	@Override
	public DataSource getDataSource(String key) {
		return (DataSource) this.springContext.getBean(key);
	}

	@Override
	public DBType getDataSourceDBType(String key) {
		return dbType;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.springContext = applicationContext;
	}

	public void setDBType(String type) {
		if ("oracle".equalsIgnoreCase(type))
			dbType = DBType.ORACLE;
		else if ("mysql".equalsIgnoreCase(type))
			dbType = DBType.MYSQL;
		else
			throw new IllegalArgumentException(type + " ������Ч�����ݿ����ͣ�ֻ����mysql��oracle(�����ִ�Сд)");
	}
}
