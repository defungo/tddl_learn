package com.taobao.tddl.parser;

import com.taobao.tddl.sqlobjecttree.SqlParserResult;

/**
 * SQL����������
 * 
 * @author shenxun 
 *
 */
public interface SQLParser{
	SqlParserResult parse(String sql, boolean isMySQL);
}
