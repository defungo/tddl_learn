//Copyright(c) Taobao.com
package com.taobao.tddl.client.pipeline;

import java.sql.SQLException;

import com.taobao.tddl.client.databus.DataBus;
import com.taobao.tddl.client.handler.Handler;

/**
 * @description ���߽ӿڶ���,��Ҫ����һЩ�Թ���Ԫ�صĲ�����������ת
 *              ����.
 * 
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 2.4.3
 * @since 1.6
 * @date 2010-08-15����03:45:43
 */
public interface Pipeline {
	void addFirst(String name, Handler handler);

	void addLast(String name, Handler handler);

	void addBefore(String baseName, String name, Handler handler);

	void addAfter(String baseName, String name, Handler handler);

	void remove(Handler handler);

	Handler remove(String name);

	<T extends Handler> T remove(Class<T> handlerType);

	Handler removeFirst();

	Handler removeLast();

	void replace(Handler oldHandler, String newName, Handler newHandler);

	Handler replace(String oldName, String newName, Handler newHandler);

	<T extends Handler> T replace(Class<T> oldHandlerType, String newName,
			Handler newHandler);

	Handler getFirst();

	Handler getLast();

	Handler get(String name);

	@SuppressWarnings("rawtypes")
	HandlerContext getContext(Handler handler);
	
	@SuppressWarnings("rawtypes")
	HandlerContext getContext(String name);
	
	@SuppressWarnings("rawtypes")
	HandlerContext getContext(Class<? extends Handler> handlerType);

	/**
	 * ��ʼִ�����̡�pipeline��������HandlerContext��
	 * ֱ�ӵ��õ���ͷ����HandlerContext.flowNext()
	 * 
	 * @param dataBus   ��������
	 * @throws SQLException
	 */
	void startFlow(DataBus dataBus) throws SQLException;
}
