package com.taobao.tddl.client;

import java.util.List;

import com.taobao.tddl.util.IDAndDateCondition.routeCondImp.SimpleCondition;

public interface SqlBaseExecutor {
	/**
	 * ��ѯ�������ݵķ����� ��ibatis�е��÷�һ�¡� ����漰����ѯ���׳�ibatis������DataAccessExpetion�쳣
	 * 
	 * @param statementID
	 *            ibatis �����ļ��е�id���ԡ�
	 * @param parameterObject
	 *            �󶨱�������������ΪMap,Bean��������͡�
	 * @param rc
	 *            �������ʽ�����ⵥ���ѯӦ��ʹ��{@link SimpleCondition}
	 * @return ibatic �����ļ��е�resultMap�ж�Ӧ�Ķ����������͡�
	 */
	Object queryForObject(String statementID, Object parameterObject,
			RouteCondition rc);
	/**
	 * ��ѯ�������ݵķ����� ��ibatis�е��÷�һ�¡� ����漰����ѯ���׳�ibatis������DataAccessExpetion�쳣
	 * 
	 * @param statementID
	 *            ibatis �����ļ��е�id���ԡ�
	 * @param parameterObject
	 *            �󶨱�������������ΪMap,Bean��������͡�
	 * @return ibatic �����ļ��е�resultMap�ж�Ӧ�Ķ����������͡�
	 */
	Object queryForObject(String statementID, Object parameterObject);
	/**
	 * ��ѯ�������ݵķ��������������ֵ�������ۼ�
	 * 
	 * @param statementID
	 *            ibatis �����ļ��е�id���ԡ�
	 * @param param
	 *            �󶨱�������������ΪMap,Bean��������͡�
	 * @param isExistsQuit
	 *            �Ƿ���ֵ�������ڱ�������������ۼ�
	 * @param rc
	 *            �������ʽ�����ⵥ���ѯӦ��ʹ��{@link SimpleCondition}
	 * @return ibatic �����ļ��е�resultMap�ж�Ӧ�Ķ����������͡�
	 */
	Object queryForObject(String statementID, Object param,
			boolean isExistsQuit, RouteCondition rc);
	/**
	 * ��ѯ�������ݵķ��������������ֵ�������ۼ�
	 * 
	 * @param statementID
	 *            ibatis �����ļ��е�id���ԡ�
	 * @param param
	 *            �󶨱�������������ΪMap,Bean��������͡�
	 * @param isExistsQuit
	 *            �Ƿ���ֵ�������ڱ�������������ۼ�
	 * @return ibatic �����ļ��е�resultMap�ж�Ӧ�Ķ����������͡�
	 */
	Object queryForObject(String statementID, Object param,
			boolean isExistsQuit);
	/**
	 * ��ѯ������ݵķ�����һ��ֻ֧�ֵ�����Ĳ�ѯ������漰����ѯ�����в�ѯ����ϲ��󷵻ء�
	 * 
	 * @param statementID
	 *            ibatis �����ļ��е�id���ԡ�
	 * @param parameterObject
	 *            �󶨱�������������ΪMap��<b>����Bean��������Ͳ��ܶ�̬������ԣ��������漰��Χ��ѯ�벻Ҫʹ��Bean�����������Ϊ����
	 *            </b>
	 * @param rc
	 *            �������ʽ�����ⵥ���ѯӦ��ʹ��{@link SimpleCondition}
	 * @return ibatic �����ļ��е�resultMap�ж�Ӧ�Ķ����������͡�
	 */
	List<Object> queryForList(String statementID, Object parameterObject,
			RouteCondition rc);
	/**
	 * ��ѯ������ݵķ�����һ��ֻ֧�ֵ�����Ĳ�ѯ������漰����ѯ�����в�ѯ����ϲ��󷵻ء�
	 * 
	 * @param statementID
	 *            ibatis �����ļ��е�id���ԡ�
	 * @param parameterObject
	 *            �󶨱�������������ΪMap��<b>����Bean��������Ͳ��ܶ�̬������ԣ��������漰��Χ��ѯ�벻Ҫʹ��Bean�����������Ϊ����
	 *            </b>
	 * @return ibatic �����ļ��е�resultMap�ж�Ӧ�Ķ����������͡�
	 */
	List<Object> queryForList(String statementID, Object parameterObject);
	/**
	 * ��ѯ������ݵķ�����һ��ֻ֧�ֵ�����Ĳ�ѯ������漰����ѯ�����в�ѯ����ϲ��󷵻ء�
	 * 
	 * @param statementID
	 *            ibatis �����ļ��е�id���ԡ�
	 * @param parameterObject
	 *            �󶨱�������������ΪMap��<b>����Bean��������Ͳ��ܶ�̬������ԣ��������漰��Χ��ѯ�벻Ҫʹ��Bean�����������Ϊ����
	 *            </b>
	 * @param isExistQuit
	 *            �Ƿ���ֵ�������ڱ�������������ۼ�
	 * @param rc
	 *            �������ʽ�����ⵥ���ѯӦ��ʹ��{@link SimpleCondition}
	 * @return ibatic �����ļ��е�resultMap�ж�Ӧ�Ķ����������͵��б�
	 */
	List<Object> queryForList(String statementID, Object parameterObject,
			boolean isExistQuit, RouteCondition rc);
	/**
	 * ��ѯ������ݵķ�����һ��ֻ֧�ֵ�����Ĳ�ѯ������漰����ѯ�����в�ѯ����ϲ��󷵻ء�
	 * 
	 * @param statementID
	 *            ibatis �����ļ��е�id���ԡ�
	 * @param parameterObject
	 *            �󶨱�������������ΪMap��<b>����Bean��������Ͳ��ܶ�̬������ԣ��������漰��Χ��ѯ�벻Ҫʹ��Bean�����������Ϊ����
	 *            </b>
	 * @param isExistQuit
	 *            �Ƿ���ֵ�������ڱ�������������ۼ�
	 * @return ibatic �����ļ��е�resultMap�ж�Ӧ�Ķ����������͵��б�
	 */
	List<Object> queryForList(String statementID, Object parameterObject,
			boolean isExistQuit);
	/**
	 * �����ѯ,��Ŀǰֻ֧�ֵ��������������ҳ��һ���о���
	 * {@link #queryForMergeSortTables(String, Object, RouteCondition)} �����غ���
	 * 
	 * @param statementID
	 *            . ibatis �����ļ��е�id���ԡ�
	 * @param parameterObject
	 *            �󶨱�������������ΪMap��<b>����Bean��������Ͳ��ܶ�̬������ԣ��������漰��Χ��ѯ�벻Ҫʹ��Bean�����������Ϊ����
	 *            </b>
	 * @param rc
	 *            �������ʽ���������ѯӦ��ʹ��{@link MergeSortTablesCondition}
	 * @return ibatic �����ļ��е�resultMap�ж�Ӧ�Ķ����������͵��б�
	 */
	List<Object> queryForMergeSortList(String statementID,
			Object parameterObject, RouteCondition rc);
	/**
	 * �����ѯ,��Ŀǰֻ֧�ֵ��������������ҳ��һ���о���
	 * {@link #queryForMergeSortTables(String, Object, RouteCondition)} �����غ���
	 * 
	 * @param statementID
	 *            . ibatis �����ļ��е�id���ԡ�
	 * @param parameterObject
	 *            �󶨱�������������ΪMap��<b>����Bean��������Ͳ��ܶ�̬������ԣ��������漰��Χ��ѯ�벻Ҫʹ��Bean�����������Ϊ����
	 *            </b>
	 * @return ibatic �����ļ��е�resultMap�ж�Ӧ�Ķ����������͵��б�
	 */
	List<Object> queryForMergeSortList(String statementID,
			Object parameterObject);
	/**
	 * �����ѯ,��ֻ֧�ֵ������ѯ�������֮��Ĵ�С��ϵ���ڲ�������ʱ���Ѿ�ȷ���õģ����1������������ԶС�ڱ�2,�Դ����ƣ����������ҳ��
	 * ����������{@link MergeSortTablesCondition}
	 * 
	 * @param statementID
	 *            ibatis �����ļ��е�id���ԡ�
	 * @param parameterObject
	 *            �󶨱�������������ΪMap��<b>����Bean��������Ͳ��ܶ�̬������ԣ��������漰��Χ��ѯ�벻Ҫʹ��Bean�����������Ϊ����
	 *            </b>
	 * @param rc
	 *            �������ʽ���������ѯӦ��ʹ��{@link MergeSortTablesCondition}
	 * @return ibatic �����ļ��е�resultMap�ж�Ӧ�Ķ����������͵��б�
	 */
	List<Object> queryForMergeSortTables(String statementID,
			Object parameterObject);
	/**
	 * �����ѯ,��ֻ֧�ֵ������ѯ�������֮��Ĵ�С��ϵ���ڲ�������ʱ���Ѿ�ȷ���õģ����1������������ԶС�ڱ�2,�Դ����ƣ����������ҳ��
	 * ����������{@link MergeSortTablesCondition}
	 * 
	 * @param statementID
	 *            ibatis �����ļ��е�id���ԡ�
	 * @param parameterObject
	 *            �󶨱�������������ΪMap��<b>����Bean��������Ͳ��ܶ�̬������ԣ��������漰��Χ��ѯ�벻Ҫʹ��Bean�����������Ϊ����
	 *            </b>
	 * @param rc
	 *            �������ʽ���������ѯӦ��ʹ��{@link MergeSortTablesCondition}
	 * @return ibatic �����ļ��е�resultMap�ж�Ӧ�Ķ����������͵��б�
	 */
	List<Object> queryForMergeSortTables(String statementID,
			Object parameterObject, RouteCondition rc);

	/**
	 * �������ݿ⣬ɾ���ᱨ������ֻ�ܽ��е��ⵥ�����
	 * 
	 * @param statementID
	 *            ibatis �����ļ��е�id���ԡ�
	 * @param parameterObject
	 *            �󶨱�������������ΪMap��Bean��������͡�
	 * @param rc
	 *            �������ʽ�����ⵥ��Ӧ��ʹ��{@link SimpleCondition}
	 * @return ����Ӱ�������
	 */
	int update(String statementID, Object parameterObject, RouteCondition rc);
	/**
	 * �������ݿ⣬ɾ���ᱨ������ֻ�ܽ��е��ⵥ�����
	 * 
	 * @param statementID
	 *            ibatis �����ļ��е�id���ԡ�
	 * @param parameterObject
	 *            �󶨱�������������ΪMap��Bean��������͡�
	 * @return ����Ӱ�������
	 */
	int update(String statementID, Object parameterObject);
	/**
	 * ����һ����¼
	 * 
	 * @param statementID
	 *            ibatis �����ļ��е�id���ԡ�
	 * @param parameterObject
	 *            �󶨱�������������ΪMap��Bean��������͡�
	 * @param rc
	 *            �������ʽ�����ⵥ��Ӧ��ʹ��{@link SimpleCondition}
	 * @return ��ibatis����һ��
	 */
	Object insert(String statementID, Object parameterObject, RouteCondition rc);
	/**
	 * ����һ����¼
	 * 
	 * @param statementID
	 *            ibatis �����ļ��е�id���ԡ�
	 * @param parameterObject
	 *            �󶨱�������������ΪMap��Bean��������͡�
	 * @return ��ibatis����һ��
	 */
	Object insert(String statementID, Object parameterObject);

}
