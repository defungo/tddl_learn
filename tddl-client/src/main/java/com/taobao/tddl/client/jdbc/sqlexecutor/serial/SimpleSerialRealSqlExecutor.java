package com.taobao.tddl.client.jdbc.sqlexecutor.serial;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.client.jdbc.ConnectionManager;
import com.taobao.tddl.client.jdbc.RealSqlContext;
import com.taobao.tddl.client.jdbc.TStatementImp;
import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlan;
import com.taobao.tddl.client.jdbc.sqlexecutor.QueryReturn;
import com.taobao.tddl.client.util.ExceptionUtils;

/**
 * @author junyu
 *
 */
public class SimpleSerialRealSqlExecutor extends SerialRealSqlExecutor {
	private static final Log logger = LogFactory
	.getLog(SimpleSerialRealSqlExecutor.class);
	
	public SimpleSerialRealSqlExecutor(ConnectionManager connectionManager) {
		super(connectionManager);
	}
	
    protected String currentDBIndex = null;
	
	protected int tableIndex = 0;

	private List<RealSqlContext> sqlContextToBeExecOnCurrentDB;
	
	protected ExecutionPlan executionPlanIn;
	
	private List<String> dbIndexList;
	
	protected Statement statement;

	protected ResultSet resultSet;
	
	private final Random ran = new Random();
	
	private boolean isPreparedStatement=false;
	
	@Override
	public void serialQuery(ConcurrentLinkedQueue<QueryReturn> queryReturnQueue,
			ExecutionPlan executionPlan, TStatementImp tStatementImp){
		if(null==this.executionPlanIn&&null!=executionPlan){
			this.executionPlanIn=executionPlan;
			isPreparedStatement = this.isPreparedStatement(tStatementImp);
			dbIndexList = new LinkedList<String>(executionPlan.getSqlMap().keySet());
			setSpecialProperty(tStatementImp, executionPlan);
		}
	
		List<SQLException> sqlExceptions = null;
		try {
			while (select()) {
				try {
					Connection con = connectionManager.getConnection(
							currentDBIndex, executionPlan.isGoSlave());
					QueryReturn qr=null;
					if (!isPreparedStatement) {
						qr=executeQueryIntervalST(con, 
								sqlContextToBeExecOnCurrentDB.get(tableIndex));
					} else { 
						qr=executeQueryIntervalPST(con,
								sqlContextToBeExecOnCurrentDB.get(tableIndex));
					}
					
					qr.setCurrentDBIndex(currentDBIndex);
					this.resultSet=qr.getResultset();
					this.statement=qr.getStatement();
					
					queryReturnQueue.add(qr);
					return;
				} catch (SQLException e) {
					//��һʱ���ӡ�쳣
					logger.error(e);
					sqlExceptions = ExceptionUtils.appendToExceptionList(sqlExceptions, e);
					sqlExceptions = tryCloseConnection(sqlExceptions,
							currentDBIndex);
					this.closeAndClearResources(sqlExceptions);
					break;
				}
			}

			// ִ�е������ʾsize�Ѿ����ˣ�û�к�����Դ���Ա�ʹ����
			writeLogOrThrowSQLException("TDDL print sqlException while retry :",
					sqlExceptions);
		} catch (SQLException e) {
			QueryReturn qr = new QueryReturn();
			qr.setExceptions(sqlExceptions);
			queryReturnQueue.add(qr);
		}
	} 
	
	protected boolean select() throws SQLException {
		List<SQLException> sqlExceptions = null;
        
		//���ﲻ����statement��resultset�����ⲿ�Լ���ά����
		if (currentDBIndex == null) {
			if (tableIndex != 0) {
				throw new IllegalStateException("tableIndex != 0 should not be here!");
			}
			// ��һ�ν������ȳ�ʼ��һ��
			writeLogOrThrowSQLException(
					"TDDL print sqlException while close resources:",
					sqlExceptions);
			return selectDBGroupByRandom();
		}

		// �����ǰexecutePlan�ı�ĸ���С�ڱ���index����ֵ����ʾ��ǰds�����б��þ���
		tableIndex++;
		if (sqlContextToBeExecOnCurrentDB.size() <= tableIndex) {
			// dbindex �������һ������ʾ��ǰdbIndex��ָ���������Ѿ�����.��ô�رյ�ǰ����
			sqlExceptions = tryCloseConnection(sqlExceptions, currentDBIndex);
			writeLogOrThrowSQLException(
					"TDDL print sqlException while close resources:",
					sqlExceptions);
			return selectDBGroupByRandom();
		}

		writeLogOrThrowSQLException(
				"TDDL print sqlException while close resources:", sqlExceptions);
		return true;
	}
	
	/**
	 * ���ѡ��һ������Դ�����ѡ��ɹ��򷵻�true ѡ��ʧ���򷵻�false û�ж���Ŀɹ�ѡ�������Դ�󣬻��Զ����ά�ֵ�ָ�롣
	 * 
	 * @return
	 */
	private boolean selectDBGroupByRandom() {
		// tableIndex����
		tableIndex = 0;
		int size = dbIndexList.size();
		if (size == 0) {
			currentDBIndex = null;
			sqlContextToBeExecOnCurrentDB = null;
			return false;
		}
		currentDBIndex = dbIndexList.remove(ran.nextInt(size));
		sqlContextToBeExecOnCurrentDB = executionPlanIn.getSqlMap().get(
				currentDBIndex);
		return true;
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
				exceptions = ExceptionUtils.appendToExceptionList(exceptions, e);
			} finally {
				resultSet = null;
			}
		}
		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
				exceptions = ExceptionUtils.appendToExceptionList(exceptions, e);
			} finally {
				statement = null;
			}
		}
		// ��ɨս����ʱ���׳��쳣������׳��쳣����϶����ٲ���һ���ˣ����Բ����ߵ�catch����
		return exceptions;
	}
	
	private void writeLogOrThrowSQLException(String message,
			List<SQLException> sqlExceptions) throws SQLException {
		// ��ʱ���׳��쳣,������쳣�Ļ�
		ExceptionUtils.throwSQLException(sqlExceptions, executionPlanIn
				.getOriginalSql(), executionPlanIn.getOriginalArgs());
	}
	
	protected List<SQLException> tryCloseConnection(
			List<SQLException> exceptions, String dbSelectorId) {
		try {
			connectionManager.tryClose(dbSelectorId);
		} catch (SQLException e) {
			exceptions = ExceptionUtils.appendToExceptionList(exceptions, e);
		}
		return exceptions;
	}
}
