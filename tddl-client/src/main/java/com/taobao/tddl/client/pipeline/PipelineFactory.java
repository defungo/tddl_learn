//Copyright(c) Taobao.com
package com.taobao.tddl.client.pipeline;

import java.sql.SQLException;
import java.util.Map;

import com.taobao.tddl.client.dispatcher.SqlDispatcher;
import com.taobao.tddl.interact.rule.bean.DBType;
import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.DirectlyRouteCondition;

/**
 * @description ���߹����ӿڶ���,AbstractPipelineFactoryʵ��������ӿ�,
 *              �����ʵ�����ǳ�����,getPipeline�������������ȥʵ��,�Դﵽ
 *              ���岻ͬhandler�Ĺ����Լ��ṩһЩ��ͬ�����Ŀ��
 *           
 *              һ��ʵ���Զ���PipelineFactory��ֱ��ʵ�ֱ��ӿ�,�̳�
 *              AbstractPipelineFactory��һ����ȷ�Ҽ��ķ���.
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 2.4.3
 * @since 1.6
 * @date 2010-08-15����03:45:43
 */
public interface PipelineFactory {
	/**
	 * ȡ�ù���
	 * @return
	 */
	public Pipeline getPipeline();
	
	/**
	 * ����Ĭ��sqlDispatcher
	 * @param defaultDispatcher
	 */
	public void setDefaultDispatcher(SqlDispatcher defaultDispatcher);
	
	/**
	 * �趨dispatcherMap,��ʼ��ʱ��ʹ��
	 * @param dispatcherMap
	 */
	public void setDispatcherMap(Map<String, SqlDispatcher> dispatcherMap);
	
	/**
	 * ����ָ���Ĺ������ѡ�����
	 * @param selectKey
	 * @return
	 * @throws SQLException
	 */
	public SqlDispatcher selectSqlDispatcher(String selectKey) throws SQLException;
	
	/**
	 * sqlԤ�������Զ��ж��Ƿ��Զ�ִ��
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public DirectlyRouteCondition sqlPreParse(String sql) throws SQLException ;
	
	/**
	 * ֧�ֶ�������DBType����
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public DBType decideDBType(String sql,SqlDispatcher sqlDispatcher)throws SQLException;
}
