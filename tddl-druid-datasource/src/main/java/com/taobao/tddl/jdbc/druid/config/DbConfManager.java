package com.taobao.tddl.jdbc.druid.config;

import com.taobao.tddl.common.config.ConfigDataListener;

/**
 * TAtom����Դȫ�ֺ�Ӧ�õ����ù���ӿڶ���
 * 
 * @author qihao
 *
 */
public interface DbConfManager {
	/**��ȡȫ������
	 * 
	 * @return
	 */
	public String getGlobalDbConf();

	/**��ȡӦ������
	 * 
	 * @return
	 */
	public String getAppDbDbConf();

	/**
	 * ע��ȫ�����ü���
	 * 
	 * @param Listener
	 */
	public void registerGlobaDbConfListener(ConfigDataListener Listener);

	/**ע��Ӧ�����ü���
	 * 
	 * @param Listener
	 */
	public void registerAppDbConfListener(ConfigDataListener Listener);

	/**
	 * ֹͣDbConfManager
	 */
	public void stopDbConfManager();
}
