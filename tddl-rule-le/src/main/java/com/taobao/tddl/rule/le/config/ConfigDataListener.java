package com.taobao.tddl.rule.le.config;

/**
 * @author shenxun
 * @author <a href="zylicfc@gmail.com">junyu</a> 
 * @version 1.0
 * @since 1.6
 * @date 2011-1-11����11:22:29
 * @desc ������Ϣ�Ļص��ӿ�
 */
public interface ConfigDataListener {
	/**
	 * �������Ŀͻ����յ�����ʱ����ע��ļ�����������
	 * �����յ������ݴ��ݵ��˷�����
	 * @param dataId         ��������������ע���id
	 * @param data           �ַ�������
	 */
    void onDataRecieved(String dataId,String data);
}
