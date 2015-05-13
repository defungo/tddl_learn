package com.taobao.tddl.client.databus;

/**
 * 
 * @author junyu
 *
 */
public interface DataBus {
	
   /**
    * ������ע��һ��Context.Ĭ������ΪContext��������������ظ���飩
    * 
    * @param name
    * @param context
    */
   public void registerPluginContext(String name,Object context);
   
   /**
    * ���������Ƴ�ָ�����ֵ�Context
    * 
    * @param name
    */
   public void removePluginContext(String name);
   
   /**
    * �������еõ�һ����ע���Context
    * 
    * @param name
    * @return
    */
   public Object getPluginContext(String name);
}
