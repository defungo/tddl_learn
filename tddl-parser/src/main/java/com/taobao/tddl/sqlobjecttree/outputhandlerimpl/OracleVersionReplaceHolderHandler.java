package com.taobao.tddl.sqlobjecttree.outputhandlerimpl;

import java.util.Map;

import com.taobao.tddl.sqlobjecttree.ReplacableWrapper;

/**
 * oracle + sync_version�õĶ���
 * 
 * @author shenxun
 *
 */
public class OracleVersionReplaceHolderHandler extends PlaceHolderReplaceHandler{

	@Override
	public String getReplacedString(Map<String, String> targetTableName,
			ReplacableWrapper replacedObj) {
		return ",sync_version=nvl(sync_version,0) + 1 ";
	}
	
}
