package com.taobao.tddl.jdbc.atom.listener;

import com.taobao.tddl.jdbc.atom.config.object.AtomDbStatusEnum;

/**���ݿ�״̬�仯������
 * 
 * @author qihao
 *
 */
public interface TAtomDbStatusListener {

	void handleData(AtomDbStatusEnum oldStatus, AtomDbStatusEnum newStatus);
}
