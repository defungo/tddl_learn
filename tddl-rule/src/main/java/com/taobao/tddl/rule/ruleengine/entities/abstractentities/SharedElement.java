package com.taobao.tddl.rule.ruleengine.entities.abstractentities;


/**
 * ��Ҫ�ṩ��һЩ�����ķ���
 * 
 * ���ڵ����
 * 
 * @author shenxun
 * 
 */
public abstract class SharedElement implements Cloneable,OneToMany {

	private String id;
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public String getId() {
		return id;
	}

	public void init() {
	}
	/**
	 * �����Է�������Ϊ��ǰTableRule��¶��init�������ⲿʹ�á�
	 * @param invokeBySpring
	 */
	public void init(boolean invokeBySpring){
		
	}

	/**
	 * ����û�ͨ��map�ķ�ʽ�����ӽڵ㣬����init�Ĺ����лὫmap��key��Ϊ�ӽڵ��id���ý�����
	 * ����û�����list�ķ�ʽ�����ӽڵ㣬��list���±��string���Ϊ�ӽڵ��id.
	 */
	public void setId(String id) {
		this.id = id;
	}
	public void put(OneToManyEntry oneToManyEntry) {
		//do nothing 
		throw new IllegalArgumentException("should not be here");
	}
}
