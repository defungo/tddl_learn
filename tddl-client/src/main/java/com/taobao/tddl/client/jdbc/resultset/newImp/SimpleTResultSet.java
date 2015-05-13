package com.taobao.tddl.client.jdbc.resultset.newImp;

import static com.taobao.tddl.client.util.ExceptionUtils.appendToExceptionList;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
 * ���Ե��߼�: 1. д������ 2. sql 1 �� 1 �Ķ�ȡ��ֻҪ��һ��resultSet���Ͳ���ʾ���׳�����ֻ�����log.
 * ���һ��resultSet��ô�У�����ʾ�׳����� 3. sql 1 �� ��Ķ�ȡ��ֻҪ��һ��resultSet����ô�Ͳ���ʾ���׳�����ֻ��ӡ����log
 *
 * @author shenxun
 * @author junyu
 *
 */
public class SimpleTResultSet extends ProxyTResultSet {
	private static final Log log = LogFactory.getLog(SimpleTResultSet.class);
	/**
	 * ��ǰ���е�statement
	 */
	protected Statement statement;
	/**
	 * ��ǰ���е�resultSet
	 */
	protected ResultSet resultSet;
	/**
	 * ��ǰ���ݿ�selector ��id
	 */
	protected String currentDBIndex = null;
	protected int fetchSize = -1;
	protected int tableIndex = 0;
	/**
	 * ����ResultSet��tStatement
	 */
	protected final TStatementImp tStatementImp;

	/**
	 * ִ�мƻ�
	 */
	protected final ExecutionPlan executionPlan;

	/**
	 * Sqlִ����
	 */
	protected final RealSqlExecutor realSqlExecutor;

	/**
	 * �Ƿ��ʼ��q
	 */
	protected boolean inited = false;

	/**
	 * ���Ľ���
	 */
	protected int limitTo = -1;
	/**
	 * ���Ŀ�ʼ
	 */
	protected int limitFrom = 0;


	private final long startQueryTime;

	private boolean hasMoreResourcesOnInit = false;

	public SimpleTResultSet(TStatementImp tStatementImp, ConnectionManager connectionManager,
			ExecutionPlan executionPlan,RealSqlExecutor realSqlExecutor) throws SQLException {
		this(tStatementImp, connectionManager, executionPlan,realSqlExecutor,true);
	}

	public SimpleTResultSet(TStatementImp tStatementImp,
			ConnectionManager connectionManager, ExecutionPlan executionPlan,RealSqlExecutor realSqlExecutor,boolean init)
			throws SQLException {
		super(connectionManager);
		this.tStatementImp = tStatementImp;

		// add by jiechen.qzm �رս����ʱ���ж���Ҫ���ʱ��
		super.setResultSetProperty(tStatementImp);
		// add end

		this.executionPlan = executionPlan;
		this.realSqlExecutor=realSqlExecutor;
		startQueryTime = System.currentTimeMillis();
		if(init){
			//��ʼ��һ��
			hasMoreResourcesOnInit = superReload();
		}
	}
	public boolean next() throws SQLException {
		checkClosed();

		if (limitTo == 0) {
			return false;
		}
		// ��ʼ��
		if (!inited) {
			inited = true;
			if(!hasMoreResourcesOnInit){
				//��ʾû�пɹ�ѡ�������Դ������������ԴΪ�գ����쳣���ܶ���֮��������²������ߵ����
				throw new SQLException("�����Ϊ�գ������������пտ�ձ��query�쳣���£���Ӧ���ߵ�����");
			}
			Map<String, List<RealSqlContext>> map = executionPlan.getSqlMap();
			if (map.size() == 0) {
				throw new SQLException("should not be here");
			}
			int tableSize = map.values().iterator().next().size();
			if (tableSize != 1 || map.size() != 1) {
				for (int i = 0; i < limitFrom; i++) {
					if (!next()) {
						// ���next����false,���ʾ��ǰ�Ѿ�û�����ݿ��Է��أ�ֱ�ӷ���false
						return false;
					}
				}
			}

		}
		/*
		 * // ��ʾ��ǰ�Ѿ�û�п��õ�resultSet�ˡ� if (resultSet == null) { return false; }
		 */
		while (true) {
			if (resultSet == null) {
				return false;
			}

			// exception throw by real resultSet , we can do nothing just throw
			// it.
			
			if (resultSet.next()) {
				// �п�����Դ����ôָ���Ѿ����ƣ�����true����
				limitTo--;
				return true;
			}
			
			if (!superReload()) {
				// ���û�п�����Դ�ˣ�Ҫ����false��������п�����Դ����ôreload������ statement��resultset
				// �ߵ�resultSet.next()�����ж��Ƿ��п�����Դ
				return false;
			}

		}

	}

	protected boolean superReload() throws SQLException{
		List<SQLException> sqlExceptions = new LinkedList<SQLException>();
		/**
		 * ������������֮ǰ��statement��resultset
		 * ��ΪSimpleTResultSetֻ����һ��resultset��statement
		 * �������۴��кͲ��б�����reload֮ǰ���֮ǰ
		 * ���е�statement��resultset
		 */
		closeAndClearResources(sqlExceptions);

		QueryReturn qr=realSqlExecutor.query();
		
		//���qrΪnull,˵�������ȡ��
		if(null==qr){
			return false;
		}

		if(null==qr.getExceptions()){
			this.statement=qr.getStatement();
			this.resultSet=qr.getResultset();
			this.currentDBIndex=qr.getCurrentDBIndex();
			super.currentResultSet=resultSet;
			return true;
		}else{
			sqlExceptions=appendToExceptionList(sqlExceptions,qr.getExceptions());
			writeLogOrThrowSQLException("TDDL print sqlException while retry :",
					sqlExceptions);

			return false;
		}
	}

	public int getFetchDirection() throws SQLException {
		log.debug("invoke getFetchDirection");
		checkClosed();
		return FETCH_FORWARD;
	}

	public void setFetchDirection(int direction) throws SQLException {
		log.debug("invoke setFetchDirection");
		checkClosed();
		if (direction != FETCH_FORWARD) {
			throw new SQLException("only support fetch direction FETCH_FORWARD");
		}
	}

	/**
	 * ������رյ�ǰstatement����Դ
	 *
	 * @param exceptions
	 * @param closeConnection
	 * @throws SQLException
	 */
	protected List<SQLException> closeAndClearResources(
			List<SQLException> exceptions) {
		if (resultSet != null) { 
			try {
				resultSet.close();
			} catch (SQLException e) {
				exceptions = appendToExceptionList(exceptions, e);
			} finally {
				resultSet = null;
			}
		}
		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
				exceptions = appendToExceptionList(exceptions, e);
			} finally {
				statement = null;
			}
		}
		// ��ɨս����ʱ���׳��쳣������׳��쳣����϶����ٲ���һ���ˣ����Բ����ߵ�catch����
		return exceptions;
	}

	protected void checkClosed() throws SQLException {
		if (closed) {
			throw new SQLException(
					"No operations allowed after result set closed.");
		}
	}

	protected void checkPoint() throws SQLException {
		if (resultSet == null) {
			throw new SQLException("�����Ϊ�ջ��Ѿ�ȡ��");
		}
	}

	public void setFetchSize(int rows) throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug("invoke setFetchSize");
		}

		checkClosed();

		if (rows < 0) {
			throw new SQLException("fetch size must greater than or equal 0");
		}

		this.fetchSize = rows;
	}

	// TODO: not used
	public int getFetchSize() throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug("invoke getFetchSize");
		}

		checkClosed();

		return fetchSize;
	}

	public int getLimitTo() {
		return limitTo;
	}

	public void setLimitTo(int limitTo) {
		this.limitTo = limitTo;
	}

	public int getLimitFrom() {
		return limitFrom;
	}

	public void setLimitFrom(int limitFrom) {
		this.limitFrom = limitFrom;
	}

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

		closed = true;
		// ͳ��������ѯ�ĺ�ʱ�������Ǻ�׼�����Ƚ���Ҫ��
		long elapsedTime = System.currentTimeMillis() - startQueryTime;
		profileDuringTime(exceptions, executionPlan.getVirtualTableName().toString(),
				executionPlan.getOriginalSql(), elapsedTime);
		try {
			if (resultSet != null) {
				resultSet.close();
			}
		} catch (SQLException e) {
			exceptions = appendToExceptionList(exceptions, e);
		} finally {
			resultSet = null;
		}

		try {
			if (this.statement != null) {
				this.statement.close();
			}
		} catch (SQLException e) {
			exceptions = appendToExceptionList(exceptions, e);
		} finally {
			this.statement = null;
		}

		// ���Ҫ���Թرյ�ǰ����
		if(currentDBIndex != null){
			//currentDBIndex == null���ʾ��û�г�ʼ���͵����˹ر�
			exceptions = tryCloseConnection(exceptions, currentDBIndex);
		}

		//�Է���һ
		for (String key : executionPlan.getSqlMap().keySet()) {
			exceptions = tryCloseConnection(exceptions, key);
		}

		writeLogOrThrowSQLException("sql exception during close resources",
				exceptions);
	}

	private void writeLogOrThrowSQLException(String message,
			List<SQLException> sqlExceptions) throws SQLException {
		// ��ʱ���׳��쳣,������쳣�Ļ�
		ExceptionUtils.throwSQLException(sqlExceptions, executionPlan
				.getOriginalSql(), executionPlan.getOriginalArgs());

	}

	public Statement getStatement() throws SQLException {
		return tStatementImp;
	}
}
