//Copyright(c) Taobao.com
package com.taobao.tddl.client.handler;

import java.sql.SQLException;

import com.taobao.tddl.client.databus.DataBus;

/**
 * @description Handler��ϵ�Ķ����ӿ�
 * 
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 2.4.3
 * @since 1.6
 * @date 2010-08-15����03:23:40
 */
public interface Handler {
	/**
	 * ��ctxȡ���������ݽ��д������������Ż�ctx, ��������һ��������
	 * 
	 * @param ctx
	 *            �������ݽṹ
	 * @throws SQLException
	 */
	public void handleDown(DataBus dataBus) throws SQLException;
}
