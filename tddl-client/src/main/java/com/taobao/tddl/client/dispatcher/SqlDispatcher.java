package com.taobao.tddl.client.dispatcher;

import java.util.List;

import com.taobao.tddl.client.RouteCondition;
import com.taobao.tddl.client.pipeline.PipelineFactory;
import com.taobao.tddl.common.exception.checked.TDLCheckedExcption;
import com.taobao.tddl.interact.rule.VirtualTableRoot;
import com.taobao.tddl.parser.SQLParser;
import com.taobao.tddl.rule.bean.TDDLRoot;

/**
 * ��TStatement�ĽǶȿ���ֻ�贫��sql����������Ϳ��Եõ�������Ϣ��
 *    1. ���sql��Ҫ����Щ�����ִ��
 *    2. ��Щsql������Щ����ĺ�������ҪTStatement���ر�Ĵ�������
 *       a)
 *       b)
 * �����������ӿ�
 * 
 * Ҫ�������ӿ�Ҫ��������������Ҫ���²��裺
 *    1. ����sql�õ�sql����Ľṹ����Ϣ
 *    2. �ӽ�������õ��߼�����, �Ӷ��õ���Ӧ��һ�׹���
 *    3. �ӹ���õ��ֿ�ֱ��ֶ���Ϣ���ӽ�������õ���Щ�ֶ���sql�е�����
 *    4. ���ݷֿ�ֱ��ֶ���sql�е�������=��Χ�����͹�����ƥ��
 * 
 * ����Ҫ��һ�������¼����ӿ�: ����������ƥ��
 *  
 * @author linxuan
 */
public interface SqlDispatcher extends DatabaseChoicer {
	/**
	 * ��ȡ��ǰ���ݿ�ͱ�
	 * @param sql
	 * @param args
	 * @return
	 * @throws TDLCheckedExcption
	 */
	DispatcherResult getDBAndTables(String sql, List<Object> args);

	/**
	 * ������SQL����ThreadLocal�����ָ������RouteCondition�����������Ŀ�ĵصĽӿ�
	 * @param rc
	 * @return
	 */
	DispatcherResult getDBAndTables(RouteCondition rc);
	
	/**
	 * ȡ��Ĭ�ϵ�TDDLRoot
	 * 
	 * add by junyu
	 * 
	 * @return
	 */
    public TDDLRoot getRoot();
    
    /**
     * ȡ��VirtualTableRoot
     * @return
     */
    public VirtualTableRoot getVtabroot();
    
    /**
     * ȡ��SqlParser
     * 
     * add by junyu
     * @return
     */
	public SQLParser getParser();
	
	/**
	 * ע��pipelineFactory,������ʹ��
	 * 
	 * add by junyu
	 */
	public void setPipelineFactory(PipelineFactory pipelineFactory);
}
