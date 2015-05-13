//Copyright(c) Taobao.com
package com.taobao.tddl.client.handler.sqlparse;

import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.client.databus.DataBus;
import com.taobao.tddl.client.databus.PipelineRuntimeInfo;
import com.taobao.tddl.client.handler.AbstractHandler;
import com.taobao.tddl.common.util.TStringUtil;
import com.taobao.tddl.interact.rule.bean.DBType;
import com.taobao.tddl.parser.SQLParser;
import com.taobao.tddl.sqlobjecttree.SqlParserResult;

/**
 * @description ��handler��Ҫ�����ǽ���sql����,��sql�еõ�������Ҫ����Ϣ,����
 *              ��,��,�������Լ���Ӧ������(PreparedStatement)����ֵ(Statement),
 *              Order by,Group By,Limit M,N�����.
 *              
 * @author <a href="junyu@taobao.com">junyu</a>
 * @version 2.4.4
 * @since 1.6
 * @date 2010-09-02����04:33:32
 */
public class SqlParseHandler extends AbstractHandler {
	public static final String HANDLER_NAME = "SqlParseHandler";
	private Log log = LogFactory.getLog(SqlParseHandler.class);

	/**
	 * SqlParseHandlerֻ��Ĭ�ϵ�ִ�����ͽ��д���
	 */
	public void handleDown(DataBus dataBus) throws SQLException {
		FlowType flowType = getPipeLineRuntimeInfo(dataBus).getFlowType();
		if (FlowType.DEFAULT == flowType || FlowType.BATCH == flowType
				|| FlowType.DBANDTAB_SQL == flowType) {
			preHandle(dataBus);
			parse(dataBus);
			afterHandle(dataBus);
		}
	}

	/**
	 * Ԥ����,������Ҫ����groupHint����ʱȥ��
	 * @param dataBus
	 */
	protected void preHandle(DataBus dataBus){
		PipelineRuntimeInfo runtime = getPipeLineRuntimeInfo(dataBus);
		String groupHintStr=TStringUtil.getBetween(runtime.getStartInfo().getSql(), "/*+TDDL_GROUP({", "})*/");
	    if(groupHintStr!=null&&!"".equals(groupHintStr.trim())){
	    	String tempSql=TStringUtil.removeBetweenWithSplitor(runtime.getStartInfo().getSql(),  "/*+TDDL_GROUP({", "})*/");
	    	runtime.getStartInfo().setSql(tempSql);
	    }
		runtime.setGroupHintStr(groupHintStr);
	}
	
	/**
	 * ����,������Ҫ������sql������groupHint��ӻ�ȥ
	 * @param dataBus
	 */
	protected void afterHandle(DataBus dataBus){
		PipelineRuntimeInfo runtime = getPipeLineRuntimeInfo(dataBus);
		String sql=runtime.getStartInfo().getSql();
		String groupHintStr=runtime.getGroupHintStr();
		if(groupHintStr!=null&&!"".equals(groupHintStr.trim())){
			StringBuilder sb=new StringBuilder("/*+TDDL_GROUP({");
			sb.append(groupHintStr);
			sb.append("})*/");
		    sb.append(sql);
		    runtime.getStartInfo().setSql(sb.toString());
		}
	}
	
	/**
	 * Sql�������
	 * 
	 * @param dataBus
	 */
	protected void parse(DataBus dataBus) {
		PipelineRuntimeInfo runtime = getPipeLineRuntimeInfo(dataBus);
		/**
		 * �õ�����������Ҫ������
		 */
		DBType dbType = (DBType) runtime.getStartInfo().getDbType();
		boolean isMySQL = DBType.MYSQL.equals(dbType);
		SQLParser sqlParser = runtime.getSQLParser();
		String sql = runtime.getStartInfo().getSql();

		/**
		 * sql����
		 */
		SqlParserResult sqlParserResult = sqlParser.parse(sql, isMySQL);

		setResult(sqlParserResult, true, runtime);
		debugLog(log, new Object[] { "sql parse end." });
	}

	/**
	 * ���ý������ҪΪSqlParserResult
	 * 
	 * @param sqlParserResult
	 * @param logicTableName
	 * @param isRealSqlParsed
	 * @param isAllowReverseOutput
	 * @param runtime
	 */
	private void setResult(SqlParserResult sqlParserResult,
			boolean isRealSqlParsed, PipelineRuntimeInfo runtime) {
		runtime.setSqlParserResult(sqlParserResult);
		runtime.setLogicTableNames(sqlParserResult.getTableName());
		runtime.setIsSqlParsed(isRealSqlParsed);
	}
}
