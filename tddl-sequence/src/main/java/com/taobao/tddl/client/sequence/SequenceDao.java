package com.taobao.tddl.client.sequence;

import com.taobao.tddl.client.sequence.exception.SequenceException;

/**
 * ����DAO�ӿ�
 *
 * @author nianbing
 */
public interface SequenceDao {
	/**
	 * ȡ����һ�����õ���������
	 *
	 * @param name ��������
	 * @return ������һ�����õ���������
	 * @throws SequenceException
	 */
	SequenceRange nextRange(String name) throws SequenceException;
	
}
