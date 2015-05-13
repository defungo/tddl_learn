/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.taobao.tddl.client.dispatcher;

import java.util.List;
import com.taobao.tddl.client.controller.DatabaseExecutionContext;
import com.taobao.tddl.interact.bean.TargetDB;

import com.taobao.tddl.sqlobjecttree.DMLCommon;
import com.taobao.tddl.sqlobjecttree.GroupFunctionType;

/**
 * @author shenxun
 */
public interface Result {
	/**
	 * ��ȡ�������
	 * @return
	 */
	public LogicTableName getVirtualTableName();
	
	/** 
	 * ��ȡĿ����Ŀ�����б�
	 * @deprecated �Ѿ�����
	 */
	public List<TargetDB> getTarget();
	
	/**
	 * ��ȡĿ����Ŀ�����·���
	 * @return
	 */
	public List<DatabaseExecutionContext> getDataBaseExecutionContexts();
	
	/**
	 * ��ȡ��ǰsql��select | columns | from
	 * ��columns������
	 * ���Ϊmax min count�ȣ���ô���ͻ�����Ӧ�仯
	 * ͬʱ���group function�����������ֶλ��ã�������᷵��NORMAL
	 * @return
	 */
	public GroupFunctionType getGroupFunctionType();
	
	/**
	 * ��ȡmaxֵ
	 * 
	 * @return maxֵ����Ӧoracle����rownum<= ? ����rownum < ? .mysql��ӦLimint m,n�����m+n��Ĭ��ֵ��{@link DMLCommon.DEFAULT_SKIP_MAX}
	 */
	public int getMax();

	/**
	 * ��ȡskipֵ��
	 * 
	 * return skipֵ����Ӧoracle����rownum>= ? ����rownum > ? .mysql��ӦLimint m,n�����m��Ĭ��ֵ��{@link DMLCommon.DEFAULT_SKIP_MAX}
	 * 
	 */
	public int getSkip();
}
