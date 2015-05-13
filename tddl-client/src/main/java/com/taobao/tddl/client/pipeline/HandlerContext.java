//Copyright(c) Taobao.com
package com.taobao.tddl.client.pipeline;

import java.sql.SQLException;

import com.taobao.tddl.client.databus.DataBus;
import com.taobao.tddl.client.handler.Handler;

/**
 * @description �����ڲ�ʹ�����ݽṹ(������Ԫ��),��װ��handler,һ��handler
 *              ��Ӧһ��HandlerContext
 * 
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 2.4.3
 * @since 1.6
 * @date 2010-08-15����03:54:42
 */
public interface HandlerContext <T> {
	
	/**
	 * ȡ�õ�ǰ�Ĺ���ʵ��
	 * 
	 * @return
	 */
    Pipeline getPipeLine();
    
    /**
     * ȡ�õ�ǰִ����������
     * 
     * @return
     */
    String getName();
    
    /**
     * ȡ�ð󶨵�ִ����
     *
     * @return
     */
    Handler getHandler();
    
    /**
     * ������Ϻ���󴫵���������
     * 1.contextά����ǰ��ڵ㣬��Pipeline��ʼ���󶨡�
     * 2.������������󶨵�Handler������ҵ���
     *   ȡ�����ڽӵ�congtext�ڵ㣬���ҵ�����һ��
     *   context�ڵ��flowNext
     * 3.���next�ڵ�Ϊnull�����ڵ�����ǵ��ý�����
     * 
     * @param dataBus
     * @throws SQLException
     */
	void flowNext(DataBus dataBus) throws SQLException;
}
