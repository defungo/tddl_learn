package com.taobao.tddl.jdbc.druid.listener;

import com.taobao.tddl.jdbc.druid.config.object.DruidDbStatusEnum;

/**���ݿ�״̬�仯������
 * 
 * @author qihao
 *
 */
public interface DruidDbStatusListener {

	void handleData(DruidDbStatusEnum oldStatus, DruidDbStatusEnum newStatus);
}
