package com.taobao.tddl.common;

/**
 * һ���������л���Holder��ʹ��ʱ��set����get
 * ��������ʱ������Ϣ��Ҫ��̬�޸ģ�ʵʱ��Ч�ĳ�����
 * ��ν����ʱ������Ϣ����ָ����ʱ��ʵʱ��ȡ������Ӱ������ʱ��Ϊ��������Ϣ��
 * ����ʱ������Ϣ��̬�޸�ʱ��Э��ʹ�������copyonwriteʵ�֣�
 * ������
 * 
 * @author linxuan
 *
 * @param <T> ��������ʱ������Ϣ�Ķ��������
 */
public class RuntimeConfigHolder<T> {
	private volatile T runtime;

	/**
	 * @return ��һ������İ�������ʱ������Ϣ�Ķ���
	 */
	public T get() {
		return runtime;
	}

	/**
	 * @param runtime ��������ʱ������Ϣ�Ķ���
	 */
	public void set(T runtime) {
		this.runtime = runtime;
	}
}
