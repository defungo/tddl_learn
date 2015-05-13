package com.taobao.tddl.client.sequence;

import com.taobao.tddl.client.sequence.exception.SequenceException;

/**
 * ��·����Ϣ���нӿ�
 *
 * @author guangxia
 *
 * @param <DatabaseRouteType> ���ݿ�·����Ϣ����
 * @param <TableRouteType> ��·����Ϣ����
 */
public interface RoutedSequence<DatabaseRouteType, TableRouteType> {
	/**
	 * ȡ��������һ��ֵ
	 *
	 * @param databaseRoute ���ݿ�·����Ϣ
	 * @param tableRoute ��·����Ϣ
	 * @return ����������һ��ֵ
	 * @throws SequenceException
	 */
	long nextValue(DatabaseRouteType databaseRoute, TableRouteType tableRoute) throws SequenceException;
}
