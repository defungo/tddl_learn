package com.taobao.tddl.sqlobjecttree;


public  class BindIndexHolder {
	private int selfAdd = 0;
	/**
	 * ���ص�ǰֵ��������where�ڵļ�����������Ӱ󶨱���ʱ�ı�ʶ
	 * 
	 * @return
	 */
	public int selfAddAndGet() {
		int ret = selfAdd;
		selfAdd++;
		return ret;
	}
	public BindIndexHolder() {
	}
	public BindIndexHolder(int index) {
		this.selfAdd = index;
	}
	public int getCurrentIndex(){
		return selfAdd;
	}
}
