package com.taobao.tddl.common.monitor;

import java.util.Date;
/**
 * 
 * @author junyu
 *
 */
public interface TimeComputer {
	/**
	 * �õ����������ĳ��ʱ��ļ��
	 * 
	 * @return �����
	 */
    public long getMostNearTimeInterval();
    
    /**
     * �õ�����������ĳ��ʱ��
     * 
     * @return Date
     */
    public Date getMostNearTime();
}
