package com.taobao.tddl.common.sync;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

/**
 * ������������ģʽ֮��עˮ��ˮģʽ�������ȡģʽ����������ģʽ
 * ˮ���ﲻ����ˮ������������ʱ��ʱ����
 * ��ˮ�ߵ�һͰˮ���˺󣬲�ȡ��ʹ��
 * 
 * @author linxuan
 *
 * @param <T>
 */
public interface BucketSwitcher<T> {
	/**
	 * עˮ��ע��ˮ��
	 * ͨ���÷������ϵؼ������񡢶��󡣡���
	 * ����ˮ���ﲻ����ˮ����ˮͰ�
	 * ˮͰ���ˣ��Զ��л���עˮ��(�û���һ�������߳�)���ع���ˮ�����������Ǹ�Ͱ
	 */
	void pourin(T task);

	/**
	 * ���ý�ˮ�ߡ�
	 * ��������˽�ˮ�ߣ�һͨˮ������Զ����ߣ����ø���ˮ�ߡ�
	 */
	abstract class BucketTaker<T> {
		private final ExecutorService executor;
		
		public BucketTaker(ExecutorService executor) {
			this.executor = executor;
		}
		
		public abstract Runnable createTakeAwayTask(Collection<T> list);
		
		public void takeAway(Collection<T> list) {
			executor.execute(createTakeAwayTask(list));
		}
	}
}
