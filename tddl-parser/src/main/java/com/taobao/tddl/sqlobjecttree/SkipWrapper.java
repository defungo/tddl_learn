package com.taobao.tddl.sqlobjecttree;

import java.util.Map;


public class SkipWrapper extends PageWrapperCommon{


	public SkipWrapper(Object val) {
		super(val);
	}

	public String getSqlReturn(Number skip, Number max) {
		if(skip instanceof Long){
			return String.valueOf(((Long)skip).longValue());
		}else if(skip instanceof Integer){
			return String.valueOf(((Integer)skip).intValue());
		}else{
			throw new IllegalArgumentException("ֻ֧��int long�����");
		}
	}

	public void modifyParam(Number skip, Number max, Map<Integer, Object> changeParam) {

		Object obj=null;
		if(skip instanceof Long){
			obj=(Long)skip;
		}else if(skip instanceof Integer){
			obj=(Integer)skip;
		}else{
			throw new IllegalArgumentException("ֻ֧��int long�����");
		}
		changeParam.put(index, obj);
	}

}
