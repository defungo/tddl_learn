package com.taobao.tddl.sqlobjecttree;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.taobao.tddl.interact.bean.ComparativeMapChoicer;
import com.taobao.tddl.sqlobjecttree.outputhandlerimpl.HandlerContainer;

/**
 * DMLCommon dmlc �������õ��ķ����ĳ���
 *  
 * @author linxuan
 */
public interface SqlParserResult {
	/**
	 * ��ȡ��ǰ����
	 * @return
	 */
	Set<String> getTableName();

	/**
	 * ��ȡsql��SKIPֵ����еĻ���û�е�����»᷵��DEFAULTֵ
	 * TODO ���ǰѲ���ȥ������Ϊ�ڽ����������ʱ���Ѿ���param��������������
	 * @param param
	 * @return
	 */
	int getSkip(List<Object> param);

	/**
	 * ��ȡsql��maxֵ����еĻ���û�еĻ��᷵��DEFAULTֵ
	 * TODO ���ǰѲ���ȥ������Ϊ�ڽ����������ʱ���Ѿ���param��������������
	 * @param param
	 * @return
	 */
	int getMax(List<Object> param);

	/**
	 * ����ǰsql��������group function.������ҽ���һ��group function,��ôʹ�ø�function
	 * ���û��group function�����ж��group function.�򷵻�NORMAL
	 * 
	 * @return
	 */
	GroupFunctionType getGroupFuncType();

	/**
	 * ��ȡorder by ����Ϣ
	 * @return
	 */
	List<OrderByEle> getOrderByEles();
	
	/**
	 * ��ȡgroup by ��Ϣ
	 * @return
	 */
	List<OrderByEle> getGroupByEles();

	/**
	 * ��������Ľӿ�
	 * @param tables
	 * @param args
	 * @param skip
	 * @param max
	 * @param outputType
	 * @param modifiedMap
	 * @return
	 */
	public List<SqlAndTableAtParser> getSqlReadyToRun(Collection<Map<String/*�������*/,String/*��ʵ����*/>> tables, List<Object> args,
			HandlerContainer handlerContainer);
	
	 /**
	  * ��ȡ�����ɸѡ��
	 * @return
	 */
	ComparativeMapChoicer getComparativeMapChoicer();
	
	/**
	 * @tofix �Ѿ���Distinct ������һ�������� Distinct �ͺ����column��һ����Ϊһ��Function column,
	 *        �˴������������Distinct����Ϊһ�����࣬����Ϊ����ķֿ�ֱ�ʱ�Ĵ�����Ҫ����������������в���!
	 *        ��Distinctֻ��Ϊ���࣬����Parser���������������SQL��Stringʱ�����������Distinct--add by
	 *        mazhidan.pt
	 *        
	 */
	public List<String> getDistinctColumn();
	
	/**
	 * �ж��Ƿ����having
	 * @return
	 */
	public boolean hasHavingCondition();
	
	/**
	 * ȡ��in ��Ϣ
	 * ʵ���ౣ֤���List����Ϊnull
	 * @return
	 */
	public List<InExpressionObject> getInExpressionObjectList();
}
