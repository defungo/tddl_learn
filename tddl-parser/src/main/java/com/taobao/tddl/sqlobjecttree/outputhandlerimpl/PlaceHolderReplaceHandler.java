package com.taobao.tddl.sqlobjecttree.outputhandlerimpl;

import java.util.Map;

import com.taobao.tddl.sqlobjecttree.ReplacableWrapper;

/**
 * ���ڴ���һЩ��Ҫֱ���滻sql�����ֵĶ���
 * 
 * @author shenxun
 *
 */
public abstract class PlaceHolderReplaceHandler implements ReplaceHandler{
	public abstract String getReplacedString(Map<String,String> targetTableName,ReplacableWrapper replacedObj);
}
