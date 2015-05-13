package com.taobao.tddl.client.jdbc.resultset.newImp;

import static com.taobao.tddl.client.util.ExceptionUtils.appendToExceptionList;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.client.jdbc.ConnectionManager;
import com.taobao.tddl.client.jdbc.RealSqlContext;
import com.taobao.tddl.client.jdbc.TStatementImp;
import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlan;
import com.taobao.tddl.client.jdbc.sqlexecutor.QueryReturn;
import com.taobao.tddl.client.jdbc.sqlexecutor.RealSqlExecutor;
import com.taobao.tddl.client.util.ExceptionUtils;

/**
 * @author guangxia
 * @author junyu
 *
 */
public class PlainAbstractTResultSet extends ProxyTResultSet {
	public PlainAbstractTResultSet(TStatementImp tStatementImp,
			ConnectionManager connectionManager, ExecutionPlan executionPlan,
			RealSqlExecutor realSqlExecutor) throws SQLException {
		this(tStatementImp, connectionManager, executionPlan, realSqlExecutor,
				true);
		super.setResultSetProperty(tStatementImp);
	}

	/**
	 * �������µĺ��š���������init����
	 *
	 * @param tStatementImp
	 * @param connectionManager
	 * @param executionPlan
	 * @param init
	 * @throws SQLException
	 */
	public PlainAbstractTResultSet(TStatementImp tStatementImp,
			ConnectionManager connectionManager, ExecutionPlan executionPlan,
			RealSqlExecutor realSqlExecutor, boolean init) throws SQLException {
		super(connectionManager);
		this.tStatementImp = tStatementImp;
		this.realSqlExecutor = realSqlExecutor;
		this.executionPlan = executionPlan;
		if (init) {
			init(connectionManager, executionPlan);
		}
	}

	private long startQueryTime = 0;
	/**
	 * �Ƿ�������֤���еĿ�ͱ�
	 */
	protected boolean enableProfileRealDBAndTables;

	/**
	 * ִ�мƻ�
	 */
	protected final ExecutionPlan executionPlan;

	/**
	 * ��ǰ���е������Ľ����
	 */
	protected List<ResultSet> actualResultSets;
	/**
	 * ��ǰ���е�����statement
	 */
	protected Set<Statement> actualStatements;

	/**
	 * ˭�½��˵�ǰ�������
	 */
	protected final TStatementImp tStatementImp;

	/**
	 * sqlִ����
	 */
	protected final RealSqlExecutor realSqlExecutor;

	/**
	 * ��ʼ���ķ����� ����������Գ�ʼ������Դ��Ĭ�ϵ�ʵ���� ��TStatement������һ�µ�һ�����Ի��ơ�Ŀ������һЩ�����ȫ��Ĳ�ѯ���Լ��ݡ�
	 *
	 *
	 * @param connectionManager
	 * @param executionContext
	 * @throws SQLException
	 */
	protected void init(ConnectionManager connectionManager,
			ExecutionPlan context) throws SQLException {
		startQueryTime = System.currentTimeMillis();
		checkClosed();
		Map<String/* db Selector id */, List<RealSqlContext>/* �����ڵ�ǰdatabase��ִ�е�sql���б� */> sqlMap = context
				.getSqlMap();
		// �ȼ���һ�±���ܸ�����������ʼ����ʱ�����ʡЩ����
		int tableSize = 0;
		for (List<RealSqlContext> l : sqlMap.values()) {
			tableSize += l.size();
		}

		List<SQLException> exceptions = new LinkedList<SQLException>();
		actualResultSets = new ArrayList<ResultSet>(tableSize);
		actualStatements = new HashSet<Statement>(tableSize);

		// �����Ľ��в�ѯ�����ˡ�
		boolean needBreak = false;
		for (Entry<String, List<RealSqlContext>> dbEntry : sqlMap.entrySet()) {
			if (needBreak) {
				break;
			}
			List<RealSqlContext> sqlList = dbEntry.getValue();
			for (int i=0;i<sqlList.size();i++) {
				QueryReturn qr = null;
				try {
					qr = this.realSqlExecutor.query();
				} catch (SQLException e) {
					exceptions.add(e);
					break;
				}
				if (qr != null) {
					if (null == qr.getExceptions()) {
						actualResultSets.add(qr.getResultset());
						actualStatements.add(qr.getStatement());
					} else {
						exceptions = appendToExceptionList(exceptions,
								qr.getExceptions());
					}
				} else {
					needBreak = true;
					break;
				}
			}
		}

		int databaseSize = sqlMap.size();

		// ��ѯ��ֻ��db tab����ͳ�ơ����쳣Ҳ����ͳ��
		profileNumberOfDBAndTablesOnly(
				context.getVirtualTableName().toString(), databaseSize,
				tableSize, context.getOriginalSql());

		ExceptionUtils.throwSQLException(exceptions, context.getOriginalSql(),
				context.getOriginalArgs());
	}

	private static final Log log = LogFactory
			.getLog(PlainAbstractTResultSet.class);

	/**
	 * bug fix by shenxun : ԭ���ᷢ��һ������������TStatement������close()����
	 * ������������TResultSetû��closedʱ���ⲿ��ʹ��iterator������ÿһ��
	 * TResultSet�����ùرյķ���������ΪTResultSet��close������ص�
	 * TStatement�������ڴ���iterator��Set<ResultSet>���󣬲�ʹ��remove������
	 * ��ͻ��׳�һ��concurrentModificationException��
	 *
	 * @param removeThis
	 *            Ŀǰ��TResultSet�У��Ƿ�Ϊtrue������Ӱ���κ�������
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	public void closeInternal() throws SQLException {
		List<SQLException> exceptions = null;
		if (log.isDebugEnabled()) {
			log.debug("invoke close");
		}

		if (closed) {
			return;
		}

		/**
		 * ��ֹ�鵽һ�������ѯ
		 */
		realSqlExecutor.clearQueryResource();

		// ͳ��������ѯ�ĺ�ʱ�������Ǻ�׼�����Ƚ���Ҫ��
		long elapsedTime = System.currentTimeMillis() - startQueryTime;

		profileDuringTime(exceptions, executionPlan.getVirtualTableName()
				.toString(), executionPlan.getOriginalSql(), elapsedTime);

		try {
			// �ر�resultset
			for (ResultSet rs : actualResultSets) {
				try {
					rs.close();
				} catch (SQLException e) {
					exceptions = appendToExceptionList(exceptions, e);
				}
			}

			// �ر�statement
			for (Statement stmt : actualStatements) {
				try {
					stmt.close();
				} catch (SQLException e) {
					exceptions = appendToExceptionList(exceptions, e);
				}
			}
		} finally {
			closed = true;
			actualStatements.clear();
			actualResultSets.clear();
			// ����Ҫ�Ƴ���ǰresultSet�Ӹ��࣬��Ϊ����ֻ�ǹرգ�������Ҫ�Ƴ���
			// if (removeThis) {
			// tStatementImp.removeCurrentTResultSet(this);
			// }
		}
		// ֪ͨ����ر���������
		for (String key : executionPlan.getSqlMap().keySet()) {
			exceptions = tryCloseConnection(exceptions, key);
		}
		// �׳��쳣�����exception ��Ϊnull
		ExceptionUtils.throwSQLException(exceptions,
				"sql exception during close resources", Collections.EMPTY_LIST);

	}

	protected void checkClosed() throws SQLException {
		if (closed) {
			throw new SQLException(
					"No operations allowed after resultset closed.");
		}
	}

	@Override
	public Statement getStatement() throws SQLException {
		return tStatementImp;
	}

	public List<ResultSet> getActualResultSets() {
		return actualResultSets;
	}

	public void setActualResultSets(List<ResultSet> actualResultSets) {
		this.actualResultSets = actualResultSets;
	}

}
