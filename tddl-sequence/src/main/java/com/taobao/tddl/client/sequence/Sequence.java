package com.taobao.tddl.client.sequence;

import com.taobao.tddl.client.sequence.exception.SequenceException;

/**
 * ���нӿ�
 *
 * @author nianbing
 */
public interface Sequence {
	/**
	 * ȡ��������һ��ֵ
	 *
	 * @return ����������һ��ֵ
	 * @throws SequenceException
	 */
	long nextValue() throws SequenceException;
}
