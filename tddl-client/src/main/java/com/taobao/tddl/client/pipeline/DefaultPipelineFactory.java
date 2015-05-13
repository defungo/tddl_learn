//Copyright(c) Taobao.com
package com.taobao.tddl.client.pipeline;

import com.taobao.tddl.client.handler.executionplan.BatchTargetSqlHandler;
import com.taobao.tddl.client.handler.executionplan.ExecutionPlanHandler;
import com.taobao.tddl.client.handler.executionplan.SqlDirectDispatchHandler;
import com.taobao.tddl.client.handler.rulematch.RouteMatchHandler;
import com.taobao.tddl.client.handler.sqlparse.RouteConditionHandler;
import com.taobao.tddl.client.handler.sqlparse.SqlParseHandler;
import com.taobao.tddl.client.handler.validate.SqlDispatchHandler;

/**
 * @description Ĭ�Ϲ��߹���ʵ����,�ṩһ���̶�handler�Ĺ���,ֻ��ʼ��һ��,
 *              �Ҳ��ܶ�̬�ı������handler����ʹ���(��Ȼ�ж���ӿڷ���)
 * 
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 2.4.3
 * @since 1.6
 * @date 2010-08-15����03:24:42
 */
public class DefaultPipelineFactory extends AbstractPipelineFactory {
	private Pipeline defaultPipeline = new DefaultPipeline();

	{
		/**
		 * ��ʼ���������ߣ����߶�Ϊ����
		 * ÿ�β�ѯ�����ݸ��¶��᷵��ͬһ������ʵ����
		 * Ψһ��һ��������Ӧ�����������ݣ�������
		 * �����ľֲ������ڹ����еĲ�ͬ������֮�䴫�ݡ�
		 * �Ӷ��������ϱ�����߳����⡣
		 */
		defaultPipeline.addLast(RouteConditionHandler.HANDLER_NAME,new RouteConditionHandler());
		defaultPipeline.addLast(SqlParseHandler.HANDLER_NAME,new SqlParseHandler());
		defaultPipeline.addLast(RouteMatchHandler.HANDLER_NAME,new RouteMatchHandler());
		defaultPipeline.addLast(SqlDirectDispatchHandler.HANDLER_NAME, new SqlDirectDispatchHandler());
		defaultPipeline.addLast(SqlDispatchHandler.HANDLER_NAME,new SqlDispatchHandler()); 
		defaultPipeline.addLast(ExecutionPlanHandler.HANDLER_NAME, new ExecutionPlanHandler());
	    defaultPipeline.addLast(BatchTargetSqlHandler.HANDLER_NAME, new BatchTargetSqlHandler());
	}

	public DefaultPipelineFactory() {}
	
	public DefaultPipelineFactory(Pipeline pipeline) {
		this.defaultPipeline=pipeline;
	}
	
	public Pipeline getPipeline() {
		return defaultPipeline;
	}

	public void setDefaultPipeline(Pipeline defaultPipeline) {
		this.defaultPipeline = defaultPipeline;
	}
}
