package com.taobao.tddl.common;


/*
 * @author guangxia
 * @since 1.0, 2010-2-9 ����03:40:20
 */
public interface StatMonitorMBean {
	
    /**
     * ���¿�ʼʵʱͳ��
     */
    void resetStat();
    /**
     * ����ͳ�Ƶ�ʱ���
     * 
     * @return
     */
    long getStatDuration();
    /**
     * ��ȡʵʱͳ�ƽ��
     * 
     * @param key1
     * @param key2
     * @param key3
     * @return
     */
    String getStatResult(String key1, String key2, String key3);
    long getDuration();

}
