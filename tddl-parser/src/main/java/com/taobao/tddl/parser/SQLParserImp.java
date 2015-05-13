package com.taobao.tddl.parser;

import static com.taobao.tddl.common.Monitor.KEY3_PARSE_SQL;
import static com.taobao.tddl.common.Monitor.add;
import static com.taobao.tddl.common.Monitor.buildExecuteSqlKey2;
import static com.taobao.tddl.common.Monitor.buildTableKey1;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.common.exception.runtime.NotSupportException;
import com.taobao.tddl.common.util.NagiosUtils;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.parser.mysql.MySQLParserLexer;
import com.taobao.tddl.parser.mysql.MySQLParserParser;
import com.taobao.tddl.parser.mysql.MySQLWalker;
import com.taobao.tddl.parser.oracle.OracleParserLexer;
import com.taobao.tddl.parser.oracle.OracleParserParser;
import com.taobao.tddl.parser.oracle.OracleWalker;
import com.taobao.tddl.sqlobjecttree.DMLCommon;
import com.taobao.tddl.sqlobjecttree.OrderByEle;
import com.taobao.tddl.sqlobjecttree.SqlAndTableAtParser;
import com.taobao.tddl.sqlobjecttree.SqlParserResult;
import com.taobao.tddl.sqlobjecttree.Statement;
import com.taobao.tddl.sqlobjecttree.mysql.function.MySQLConsistStringRegister;
import com.taobao.tddl.sqlobjecttree.mysql.function.MySQLFunctionRegister;
import com.taobao.tddl.sqlobjecttree.oracle.function.OracleConsistStringRegister;
import com.taobao.tddl.sqlobjecttree.oracle.function.OracleFunctionRegister;
import com.taobao.tddl.sqlobjecttree.oracle.function.OracleHintRegister;
import com.taobao.tddl.sqlobjecttree.outputhandlerimpl.HandlerContainer;

/**
 * SQL ��������ʵ���࣬��Ҫ�ǽ�SQL�������ŵ�cache�У�
 * ���cache���и���SQL,��ֱ�Ӵ�cache��ȡ���������parse
 * 
 * @author shenxun
 *
 */
public class SQLParserImp implements SQLParser{
	private static final Log log = LogFactory.getLog(SQLParserImp.class);
	
	/**
	 * ���Դ�cache��ȡ��sql,���δȡ�����������sql����ʼ����
	 * 
	 * �����Ƕ�γ�ʼ��������Ϊkeyһ�£�ͬһ��sql��������ʼ���Ժ�Ľ����һ�µ�
	 * 
	 * ���п�����Ϊ������put��init֮ǰ������,�������������
	 * @param sql
	 */
	public DMLCommon parseSQL(String sql) {
		return nestedParseSql(sql,true);
	}

	private final ParserCache globalCache = ParserCache.instance();

	public DMLCommon parseSQL(String sql,boolean isMysql){
		 return nestedParseSql(sql, isMysql);
	}
	/*
	 * bugfix : http://jira.taobao.ali.com/browse/TDDL-78
	 */
	private DMLCommon nestedParseSql(final String sql,final boolean isMysql) {
		if (sql == null) {
			throw new IllegalArgumentException("sql must not be null");
		}
		//Ϊ�˷�ֹ����ظ���ʼ��������ʹ����future task��ȷ����ʼ��ֻ����һ��
		FutureTask<DMLCommon> future = globalCache.getFutureTask(sql);
		if (future == null) {
			Callable<DMLCommon> handle = new Callable<DMLCommon>() {
				public DMLCommon call() throws Exception {
					final DMLCommon com = getDMLCommonObject(sql, isMysql);
					com.init();
					log.info("successfully parse a sql");
					log.info("original sql:"+sql);
					StringBuilder sb = new StringBuilder();
					com.appendSQL(sb);
					log.info("parsed sql:"+sb.toString());
					return com;
				}


			};
			future = new FutureTask<DMLCommon>(handle);
			future = globalCache.setFutureTaskIfAbsent(sql,future);
			future.run();
		}
		//ȷ���׳��쳣
		DMLCommon dmlcommon = null;
		try {
			dmlcommon = future.get();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return dmlcommon;
	}
	
	/**
	 * Antlr����sql��������java�����ʾ��SQL��
	 * 
	 * @param sql
	 * @param isMysql
	 * @return
	 */
	private DMLCommon getDMLCommonObject(String sql,
			final boolean isMysql) {
		final DMLCommon com;
		try {
			AntlrStringStream st = new AntlrStringStream(
					sql);
			if (isMysql) {
				MySQLWalker.beg_return ret = null;
				MySQLParserLexer pl = new MySQLParserLexer(
						st);
				TokenRewriteStream tokens = new TokenRewriteStream(
						pl);
				MySQLParserParser pa = new MySQLParserParser(
						tokens);

				pa.setFunc(MySQLFunctionRegister.reg);
				pa
						.setConsist(MySQLConsistStringRegister.reg);
				MySQLParserParser.beg_return beg = null;
				beg = pa.beg();
				CommonTree tree = (CommonTree) beg
						.getTree();
				log.debug(tree.toStringTree());
				CommonTreeNodeStream nodes = new CommonTreeNodeStream(
						tree);
				nodes.setTokenStream(tokens);
				MySQLWalker walker = new MySQLWalker(nodes);
				walker.setFunc(MySQLFunctionRegister.reg);
				walker
						.setConsist(MySQLConsistStringRegister.reg);
				ret = walker.beg();
				com = ret.obj;
			} else {
				OracleWalker.beg_return ret = null;
				OracleParserLexer pl = new OracleParserLexer(
						st);
				TokenRewriteStream tokens = new TokenRewriteStream(
						pl);
				OracleParserParser pa = new OracleParserParser(
						tokens);

				pa.setFunc(OracleFunctionRegister.reg);
				pa.setOracleHint(OracleHintRegister.reg);
				pa
						.setConsist(OracleConsistStringRegister.reg);

				OracleParserParser.beg_return beg = null;
				beg = pa.beg();
				CommonTree tree = (CommonTree) beg
						.getTree();
				log.debug(tree.toStringTree());
				CommonTreeNodeStream nodes = new CommonTreeNodeStream(
						tree);
				nodes.setTokenStream(tokens);
				OracleWalker walker = new OracleWalker(
						nodes);
				walker.setFunc(OracleFunctionRegister.reg);
				walker
						.setConsist(OracleConsistStringRegister.reg);
				walker
						.setOracleHint(OracleHintRegister.reg);
				ret = walker.beg();
				com = ret.obj;
			}
		} catch (RecognitionException e) {
			NagiosUtils.addNagiosLog(NagiosUtils.KEY_SQL_PARSE_FAIL, 1);
			throw new RuntimeException("����sql���󣬴����sql��:"
					+ sql, e);
		}
		return com;
	}
	
	/**
	 * ����SQL��ȡ��Ӧ��javaSQL����
	 * @param sql
	 * @return java SQL ���� ���cache��û���򷵻ؿ�
	 */
	public Statement getStatement(String sql) {
		try {
			FutureTask<DMLCommon> future = globalCache.getFutureTask(sql);
			if(future == null){
				return null;
			}
			else{
				return future.get();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * ����partinationSet�����StringȥSQL�в�����where�������Ƿ��ж�Ӧ���С��������ȫ���ҳ������뵽Comparativemap��
	 * 
	 * ���SQL�������еĻ�����Ҫ���󶨵ı����󶨵�comparativeMap�����Comparative�С�
	 * 
	 * ��ֵ��ɺ󷵻ء�
	 * 
	 * @param sql
	 * @param argument
	 * @param partnation
	 * @return
	 */
	public Map<String, Comparative> eval(String sql,List<Object> argument,Set<String> partnation){
		try {
			DMLCommon dmlc = ((DMLCommon)getStatement(sql));
			if(dmlc == null){
				return Collections.emptyMap();
			}else{
				return dmlc.getColumnsMap(argument,partnation);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public Set<String> getTableName(String sql){
		try {
			DMLCommon dmlc = ((DMLCommon)getStatement(sql));
			if(dmlc == null){
				return null;
			}else{
				return dmlc.getTableName();
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public long getLimitFrom(String sql,List<Object> param){
		try {
			DMLCommon dmlc = ((DMLCommon)getStatement(sql));
			if(dmlc == null){
				return DMLCommon.DEFAULT_SKIP_MAX;
			}else{
				return dmlc.getSkip(param);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public long getLimitTo(String sql,List<Object> param){
		try {
			DMLCommon dmlc = ((DMLCommon)getStatement(sql));
			if(dmlc == null){
				return DMLCommon.DEFAULT_SKIP_MAX;
			}else{
				return dmlc.getMax(param);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public List<OrderByEle> getOrderByList(String sql) {
		try {
			DMLCommon dmlc = ((DMLCommon)getStatement(sql));
			if(dmlc == null){
				return Collections.emptyList();
			}else{
				return dmlc.getOrderByEles();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param sql Ŀ��sql
	 * @param tables ����Map,��Ҫ������Ҫ�滻�ı���->Ŀ�����Set
	 * @param args sql��Ӧ�Ĳ���
	 * @param limitFrom ���Ķ���ʼ
	 * @param limitTo ���Ķ�����
	 * @param handlerContainer ����ѡ����
	 * @param map ������Ҫ�޸ĵ�
	 * @return
	 */
	public List<SqlAndTableAtParser> getSqlReadyToRun(String sql,Set<Map/*tables*/<String/*ori table*/,
			String/*target table*/>> tables, List<Object> args,
			HandlerContainer handlerContainer) 
	{
		if(sql==null)
		{
			throw new IllegalArgumentException("Ŀ��sqlΪ��");
		}
		try {
			DMLCommon dmlc = ((DMLCommon)getStatement(sql));
			if(dmlc == null){
				return Collections.emptyList();
			}else{
				return dmlc.getSqlReadyToRun(tables, args,handlerContainer);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public SqlParserResult parse(String sql, boolean isMySQL) {
		DMLCommon com =this.parseSQL(sql, isMySQL);
		Set<String> table = null;
		try {
			if (com == null) {
				/*
				 * bugfix : http://jira.taobao.ali.com/browse/TDDL-78
				 */
				// ���ûȡ�������Է���sql����ʼ��
				com = parseSQL(sql, isMySQL);
				table = ((DMLCommon) com).getTableName();
				
				add(buildTableKey1(table.toString()), buildExecuteSqlKey2(sql), KEY3_PARSE_SQL,
						0, 1);

			} else {
				table = ((DMLCommon) com).getTableName();
				add(buildTableKey1(table.toString()), buildExecuteSqlKey2(sql), KEY3_PARSE_SQL,
						1, 1);
			}
		} catch (ClassCastException e) {
			throw new NotSupportException(e.getMessage() + ".not support yet");
		}
		return com;
	}
}
