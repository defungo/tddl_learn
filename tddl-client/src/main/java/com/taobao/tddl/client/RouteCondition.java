package com.taobao.tddl.client;

/**
 * ͨ�ýӿڣ�����ͨ����д{@link RouteHandler} ����ע�ᵽ{@link RouteHandlerRegister}
 * �ϵķ�ʽ�������µ��Զ���conditionHandler.�����Ϳ���֧�ֶ��ֲ�ͬ�������Լ����� {@link RouteHandlerRegister}
 * �е�keyΪRouteConditionʵ�ֵ�classȫ���ơ�
 * 
 * 
 * @author shenxun
 * 
 */
public interface RouteCondition {
	public enum ROUTE_TYPE{
		FLUSH_ON_CLOSECONNECTION,
		FLUSH_ON_EXECUTE;
	}
	public String getVirtualTableName() ;
	public void setVirtualTableName(String virtualTableName);
	public ROUTE_TYPE getRouteType();
	public void setRouteType(ROUTE_TYPE routeType);
}
