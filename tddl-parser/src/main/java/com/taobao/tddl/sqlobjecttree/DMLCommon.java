package com.taobao.tddl.sqlobjecttree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.taobao.tddl.common.sqlobjecttree.Column;
import com.taobao.tddl.common.sqlobjecttree.SQLFragment;
import com.taobao.tddl.interact.bean.ComparativeMapChoicer;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.interact.sqljep.ComparativeBaseList;
import com.taobao.tddl.sqlobjecttree.common.TableNameSubQueryImp;
import com.taobao.tddl.sqlobjecttree.common.expression.ComparableExpression;
import com.taobao.tddl.sqlobjecttree.common.expression.ExpressionGroup;
import com.taobao.tddl.sqlobjecttree.common.expression.InExpression;
import com.taobao.tddl.sqlobjecttree.common.expression.NotInExpression;
import com.taobao.tddl.sqlobjecttree.common.value.BindVar;
import com.taobao.tddl.sqlobjecttree.common.value.UnknowValueObject;
import com.taobao.tddl.sqlobjecttree.outputhandlerimpl.HandlerContainer;
import com.taobao.tddl.sqlobjecttree.outputhandlerimpl.PlaceHolderReplaceHandler;
import com.taobao.tddl.sqlobjecttree.outputhandlerimpl.RangePlaceHandler;
import com.taobao.tddl.sqlobjecttree.traversalAction.GroupByTraversalAction;
import com.taobao.tddl.sqlobjecttree.traversalAction.OrderByTraversalAction;
import com.taobao.tddl.sqlobjecttree.traversalAction.TableNameTraversalAction;
import com.taobao.tddl.sqlobjecttree.traversalAction.TraversalSQLAction;
import com.taobao.tddl.sqlobjecttree.traversalAction.TraversalSQLEvent;
import com.taobao.tddl.sqlobjecttree.traversalAction.TraversalSQLEvent.StatementType;

/**
 * insert update delete select��������
 * 
 * @author shenxun
 * 
 */
public abstract class DMLCommon implements Statement, SqlParserResult,
		ComparativeMapChoicer {
	protected BindIndexHolder holder = new BindIndexHolder();

	protected List<OrderByEle> orderByEle = Collections.emptyList();
	protected List<OrderByEle> groupByEle = Collections.emptyList();
	protected List<InExpressionObject> inObjList = new ArrayList<InExpressionObject>(
			1);

	protected List<Hint> hints = new ArrayList<Hint>(1);

	public DMLCommon(BindIndexHolder holder) {
		this.holder = holder;
	}

	public DMLCommon() {
	}

	public BindIndexHolder getIndexHolder() {
		return holder;
	}

	/**
	 * ����ӳ�������ʵ��sql�е�Ԫ��֮���ӳ���ϵ����������ֱ��ͨ�������ҵ���Ӧ��Ԫ��
	 * 
	 * Ŀǰ��ʱֻ֧�ֱ����������,���ѯ���������ӳ���ϵ��
	 * 
	 * ��Ҫ���ڽ��Ƕ�ײ�ѯ��taobao�ڶ���str2varlist��str2numlist������
	 */
	protected volatile Map<String, SQLFragment> aliasToSQLFragementMap = new HashMap<String, SQLFragment>();

	/**
	 * ����һ�����where�����ṹ�����sql��Ƕ�׵ģ���ô���List����Ƕ��sql�е�ÿһ��һһ��Ӧ
	 * 
	 * ��0���Ӧsql����㣬�������¡�
	 * 
	 * ÿһ�㶼�Ǹ�sql����where������һ������ϲ����Comparative�����Map.
	 * 
	 * ʹ�õ�ʱ�򣬻��������List,�ҵ��ֿ�ͷֱ��column,����ж��������׳��쳣��
	 * 
	 */
	protected volatile List<Map<String, Comparative>> repListMap = new ArrayList<Map<String, Comparative>>();
	/**
	 * ����String��limit���󣬺��ĵ�ԭ���Ƿ���һ��sql����Ҫ�仯�ĺͲ���Ҫ�仯�Ķ��󣬰�������Ҫ��ӱ����ĵط��ճ���
	 * ֻ����sql������Ҫ�滻�ı�������������ֶΣ�ͬʱ��һ�ζ����ķ����У��������limit�е����ݶ��� �൱��һ��������sql�в���Ԫ�صĻ��档
	 */
	protected final List<Object> modifiableList = new ArrayList<Object>(2);
	/**
	 * ���û��skip��max�᷵�ش�ֵ
	 */
	public final static int DEFAULT_SKIP_MAX = -1000;
	Set<String> tableName = null;

	/**
	 * ����List
	 */
	protected List<TableName> tbNames = new ArrayList<TableName>(2);

	/**
	 * �Ӷ��where�����и���partnationSetѡ�����Ҫ����У����󶨱�����ֵ�����ҷ����к�����Ӧ��ֵ��
	 * 
	 * �����������еĲ�ͬ�㶼������ͬһ���У������쳣��ȥ��
	 * 
	 * ��ȡComparativeMap. map��key ������ value�ǰ󶨱������{@link Comparative}
	 * ����Ǹ����ɸ�ֵ�ı������򲻻᷵�ء� ���ɸ�ֵָ���ǣ���Ȼ���Խ������������Ժ�Ľ�����ܽ��м��㡣 ��where col =
	 * concat(str,str); ����SQL��Ȼ���Խ���������Ϊ��Ӧ�Ĵ�����û����ɣ������ǲ��ܸ�ֵ�ġ����������col
	 * �ǲ��ᱻ�ŵ����ص�map�еġ�
	 * 
	 * @param arguments
	 * @param partnationSet
	 * @param copiedMap
	 * @return
	 */
	public final Map<String, Comparative> getColumnsMap(List<Object> arguments,
			Set<String> partnationSet) {
		Map<String, Comparative> copiedMap = new HashMap<String, Comparative>(
				partnationSet.size());
		for (String aArgument : partnationSet) {
			/*
			 * for (Map<String, Comparative> map : repListMap) { //modified by
			 * shenxun. ��Ϊ�¹����������ڴ����str�п����Ǵ�Сд���еġ�
			 * //����sql��һ��ʵ����ȴ��С�������ˡ����Ҫ��ʾ��ת��һ�� Comparative temp =
			 * map.get(aArgument.toUpperCase()); if (temp != null) { if
			 * (copiedMap.containsKey(aArgument)) { throw new
			 * IllegalArgumentException(
			 * "�������ڶ��sql��where�����г��ֶ�����ַֿ��ֶεĵ㡣������ķֿ��ֶ��ǣ�" + aArgument); }
			 * Comparative comparative = temp.getVal(arguments,
			 * aliasToSQLFragementMap); if
			 * (!containsUnknowValueObject(comparative)) {
			 * //������map�Ļ�������ԭʼ�Ĵ�Сд���е��ִ�����������Ǳ��޷�ʹ�� copiedMap.put(aArgument,
			 * comparative); } } }
			 */
			Comparative comparative = getColumnComparative(arguments, aArgument);
			if (comparative != null) {
				copiedMap.put(aArgument, comparative);
			}
		}
		return copiedMap;
	}

	public Comparative getColumnComparative(List<Object> arguments,
			final String aArgument) {
		Comparative res = null;
		String upperCaseArg = aArgument.toUpperCase();
		for (Map<String, Comparative> map : repListMap) {
			// modified by shenxun. ��Ϊ�¹����������ڴ����str�п����Ǵ�Сд���еġ�
			// ����sql��һ��ʵ����ȴ��С�������ˡ����Ҫ��ʾ��ת��һ��
			Comparative temp = map.get(upperCaseArg);
			if (temp != null) {
				// if (copiedMap.containsKey(aArgument)) {
				if (res != null) {
					throw new IllegalArgumentException(
							"�������ڶ��sql��where�����г��ֶ�����ַֿ��ֶεĵ㡣������ķֿ��ֶ��ǣ�"
									+ aArgument);
				}
				Comparative comparative = temp.getVal(arguments,
						aliasToSQLFragementMap);
				if (!containsUnknowValueObject(comparative)) {
					// ������map�Ļ�������ԭʼ�Ĵ�Сд���е��ִ�����������Ǳ��޷�ʹ��
					// copiedMap.put(aArgument, comparative);
					res = comparative;
				}
			}
		}
		return res;
	}

	protected boolean containsUnknowValueObject(Comparative comparative) {
		;
		if (comparative.getValue() instanceof UnknowValueObject) {
			return true;
		} else if (comparative instanceof ComparativeBaseList) {
			List<Comparative> list = ((ComparativeBaseList) comparative)
					.getList();
			if (list != null) {
				// ����ڲ�ѭ����һ������unknowValueObject�ľ�ֱ�ӷ���true;
				for (Comparative c : list) {
					if (containsUnknowValueObject(c)) {
						return true;
					}
				}
				// һ����û�е�����£�����false;
				return false;
			}
		}
		return false;
	}

	public List<TableName> getTbNames() {
		return tbNames;
	}

	public void addTable(TableName tableName) {
		this.tbNames.add(tableName);
	}

	/**
	 * �������Ӧ��ȷ��ֻ�ڷ����ڱ����á����̰߳�ȫ
	 */
	public void init() {

		initAliasAndComparableMap(aliasToSQLFragementMap, repListMap);

		registerTraversalActionAndGet();

		registerUnmodifiableSqlOutputFragement();
	}

	/**
	 * ע�����ÿһ��where������action,���һ�ȡ���
	 */
	public void registerTraversalActionAndGet() {
		// ��ӵ�ǰһ��sql��wheree�������ݵ�List
		List<TraversalSQLAction> traversalSQLActions = new ArrayList<TraversalSQLAction>();

		TableNameTraversalAction tbNameaction = new TableNameTraversalAction();

		traversalSQLActions.add(tbNameaction);

		OrderByTraversalAction orderby = new OrderByTraversalAction();
		traversalSQLActions.add(orderby);

		GroupByTraversalAction groupby = new GroupByTraversalAction();
		traversalSQLActions.add(groupby);

		traversalSQL(traversalSQLActions, null);

		tableName = tbNameaction.getTableName();

		orderByEle = orderby.getOrderByEles();
		groupByEle = groupby.getGroupByEles();
	}

	/**
	 * ע��ɱ�sql�в����sql
	 */
	protected void registerUnmodifiableSqlOutputFragement() {

		StringBuilder sb = new StringBuilder();

		sb = regTableModifiable(tableName, modifiableList, sb);

		modifiableList.add(sb.toString());

	}

	public List<OrderByEle> getOrderByEles() {
		return orderByEle;
	}

	public List<OrderByEle> getGroupByEles() {
		return groupByEle;
	}

	/**
	 * ����˱�ı�����select |column| from ��column�ı���
	 * 
	 * @param sqlAliasMap
	 */
	public void buildAliasToTableAndColumnMapping(
			Map<String, SQLFragment> sqlAliasMap) {

		for (TableName name : tbNames) {

			name.appendAliasToSQLMap(sqlAliasMap);

		}
	}

	/**
	 * ��ʼ��������map��ComparativeMap
	 * 
	 * @param sqlAliasMap
	 *            ��Ϊֻ��Ҫһ��sqlAliasMap,�����������sqlAliasMap
	 * @param repListMap
	 *            ��Ϊֻ��Ҫһ��ComparableMap,���Դ���������repListMap
	 */
	protected void initAliasAndComparableMap(
			Map<String, SQLFragment> aliasToSQLFragementMap,
			List<Map<String, Comparative>> repListMap) {
		buildAliasToTableAndColumnMapping(aliasToSQLFragementMap);
		boolean hasOneSubSelect = false;
		// tbNamesӦ��ȡ��ǰǶ���е�tbNames
		for (TableName name : tbNames) {
			if (name instanceof TableNameSubQueryImp) {

				if (hasOneSubSelect) {
					throw new IllegalArgumentException("ͬ��sql��������ֶ����sql");
				}
				hasOneSubSelect = true;
				TableNameSubQueryImp subSql = (TableNameSubQueryImp) name;
				Select select = subSql.getSubSelect();
				select.initAliasAndComparableMap(aliasToSQLFragementMap,
						repListMap);
			}

		}
		repListMap.add(getSubColumnsMap());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.taobao.tddl.common.sqlobjecttree.SQLFragment#regTableModifiable(java
	 * .lang.String, java.util.List, java.lang.StringBuilder)
	 */
	public StringBuilder regTableModifiable(Set<String> oraTabName,
			List<Object> list, StringBuilder sb) {
		boolean hasMoreElement = false;
		for (TableName tbName : tbNames) {
			if (hasMoreElement) {
				sb.append(",");
			} else {
				hasMoreElement = true;
			}
			sb = tbName.regTableModifiable(oraTabName, list, sb);
		}
		sb.append(" ");
		return sb;
	}

	public boolean hasTable() {
		return tbNames.size() == 0 ? false : true;
	}

	/**
	 * ���һ��������where��������ô���ܹ���ȡ�������where���������� ��->ֵ��map
	 * 
	 * @return
	 */
	protected abstract Map<String, Comparative> getSubColumnsMap();

	/**
	 * ����ÿһ��sql�ı������֣����Ȳ�ѯ��ǰ��where������Ȼ���Ǳ����������������Ƕ�� Ҳ���������߼�����ѭ������
	 * 
	 * @param traversalSQLActions
	 */
	public void traversalSQL(List<TraversalSQLAction> traversalSQLActions,
			StatementType type) {
		if (type != null) {
			notifyAll(traversalSQLActions, this, type);
		} else {
			notifyAll(traversalSQLActions, this, StatementType.NORMAL);
		}
		WhereCondition where = getSubWhereCondition();
		if (where != null) {
			ExpressionGroup expgrp = where.getExpGroup();
			traversalExpressionGroup(expgrp, traversalSQLActions);
		}
		// ����ÿһ��SQL��Ƕ���ڲ�sqlҲ���б���
		for (TableName tbName : tbNames) {
			if (tbName instanceof TableNameSubQueryImp) {
				Select select = ((TableNameSubQueryImp) tbName).getSubSelect();
				select.traversalSQL(traversalSQLActions, StatementType.TABLE);
			}
		}
	}

	/**
	 * ��������ExpressionGroup,���������Select
	 * TODO:��δ��������ص�����,��ͨ��action������,ȴ����������,�����actionʶ��?
	 * 
	 * @param expgrp
	 * @param travelsalSQLActions
	 */
	protected void traversalExpressionGroup(ExpressionGroup expgrp,
			List<TraversalSQLAction> traversalSQLActions) {
		List<Expression> exps = expgrp.getExpressions();
		for (Expression exp : exps) {
			if (exp instanceof ExpressionGroup) {
				// ���ʽ��Ƕ�ס�
				traversalExpressionGroup((ExpressionGroup) exp,
						traversalSQLActions);
			} else if (exp instanceof ComparableExpression) {
				Object obj = ((ComparableExpression) exp).getRight();
				whereArgumentHandler(traversalSQLActions, obj);
				obj = ((ComparableExpression) exp).getLeft();
				whereArgumentHandler(traversalSQLActions, obj);
			} else if (exp instanceof InExpression) {
				Object obj = ((InExpression) exp).getRight();
				whereArgumentHandler(traversalSQLActions, obj);
				obj = ((InExpression) exp).getLeft();
				whereArgumentHandler(traversalSQLActions, obj);
				inExpressionHandle((InExpression) exp);
			} else if (exp instanceof NotInExpression) {
				Object obj = ((NotInExpression) exp).getRight();
				whereArgumentHandler(traversalSQLActions, obj);
				obj = ((NotInExpression) exp).getLeft();
				whereArgumentHandler(traversalSQLActions, obj);
			} else {
				throw new IllegalStateException("should not be here");
			}
		}
	}

	/**
	 * id in�������֪��id in��Ϣ (��Ҫ�ع�)
	 * 
	 * @param right
	 * @param left
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void inExpressionHandle(InExpression exp) {
		Column column = (Column) exp.getLeft();
		Object values = exp.getRight();

		if (values instanceof List) {
			StringBuilder expStr = new StringBuilder();
			exp.appendSQL(expStr);

			List valuesList = (List) values;
			if (valuesList.get(0) instanceof BindVar) {
				// �����п����� id in(?,?,1,2),�������,�Ͳ�֧����
				for (Object obj : valuesList) {
					if (!(obj instanceof BindVar)) {
						return;
					}
				}
				List<Integer> indexs = new ArrayList<Integer>(valuesList.size());
				List<BindVar> bvs = (List<BindVar>) valuesList;
				for (BindVar bv : bvs) {
					indexs.add(bv.getIndex());
				}
				this.inObjList.add(new InExpressionObject(column.getColumn(),
						column.getAlias(), indexs, null, expStr.toString()));
			} else {
				// �����п����� id in(?,?,1,2),�������,�Ͳ�֧����
				for (Object obj : valuesList) {
					if (obj instanceof BindVar) {
						return;
					}
				}

				List<Object> indexs = new ArrayList<Object>(valuesList.size());
				List<Object> bvs = (List<Object>) valuesList;
				for (Object bv : bvs) {
					indexs.add(bv);
				}
				this.inObjList.add(new InExpressionObject(column.getColumn(),
						column.getAlias(), null, indexs, expStr.toString()));
			}
		} else if (values instanceof Select) {
			// ��ʱ��֧��id in ���Ӳ�ѯ�еĹ���,��ԭ��������
		}
	}

	private static void whereArgumentHandler(
			List<TraversalSQLAction> traversalSQLActions, Object obj) {
		if (obj instanceof Select) {
			notifyAll(traversalSQLActions, (Select) obj, StatementType.WHERE);
		}
	}

	private static void notifyAll(List<TraversalSQLAction> travelsarSQLActions,
			DMLCommon dmlc, StatementType type) {
		for (TraversalSQLAction action : travelsarSQLActions) {
			action.actionProformed(new TraversalSQLEvent(StatementType.TABLE,
					dmlc));
		}
	}

	/**
	 * ��ȡwhere����������еĻ������л᷵��null
	 */
	public abstract WhereCondition getSubWhereCondition();

	@SuppressWarnings("unchecked")
	public Set<String> getTableName() {
		return (tableName == null ? Collections.EMPTY_SET : tableName);
	}

	/**
	 * ��������������ȡlimit m,n�е�n ��oracle rownum < ?�е�?
	 * 
	 * @param param
	 * @return
	 */
	protected int getRangeOrMax(List<Object> param) {
		int max = DEFAULT_SKIP_MAX;
		int temp = DEFAULT_SKIP_MAX;

		for (TableName tbName : tbNames) {
			// �鿴SQL��Ƕ��SQL,�����Ƿ���RangeOrMaxֵ��������Ƕ��SQL��ɵĶ��Ƕ��SQL�У���
			// ����㼶����rangeOrMax,����ѡ���ġ�
			if (tbName instanceof TableNameSubQueryImp) {
				temp = ((TableNameSubQueryImp) tbName).getSubSelect()
						.getRangeOrMax(param);
				// ������ô�Ż� maxֵ,ֵ���϶������������壬
				if (temp > max) {
					max = temp;
				}
			}
		}
		return max;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.taobao.tddl.sqlobjecttree.SqlParserResult#getSkip(java.util.List)
	 */
	public int getSkip(List<Object> param) {
		int skip = DEFAULT_SKIP_MAX;
		int temp = DEFAULT_SKIP_MAX;
		for (TableName tbName : tbNames) {
			if (tbName instanceof TableNameSubQueryImp) {
				temp = ((TableNameSubQueryImp) tbName).getSubSelect().getSkip(
						param);
				if (temp > skip) {
					skip = temp;
				}
			}
		}
		return skip;
	}

	public void appendSQL(StringBuilder sb) {
		boolean comma = false;
		for (TableName tbName : tbNames) {
			if (comma) {
				sb.append(",");
			}
			comma = true;
			tbName.appendSQL(sb);
		}
		sb.append(" ");
	}

	/**
	 * ����select�д���group function���������sql(crud������)��groupfunctionType��ΪNORMAL
	 * 
	 * @return
	 */
	public GroupFunctionType getGroupFuncType() {
		return GroupFunctionType.NORMAL;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		boolean comma = false;
		for (TableName tbName : tbNames) {
			if (comma) {
				sb.append(",");
			}
			comma = true;
			if (Constant.useToString(tbName)) {
				sb.append(tbName.toString());
			} else {
				tbName.appendSQL(sb);
			}
		}
		sb.append(" ");
		return sb.toString();
	}

	/**
	 * @param tables
	 * @param args
	 * @param skip
	 *            �����䣬���Ŀ�ʼ
	 * @param max
	 *            �����䣬����
	 * @return
	 */
	public List<SqlAndTableAtParser> getSqlReadyToRun(
			Collection<Map<String/* ������� */, String/* ��ʵ���� */>> tables,
			List<Object> args, HandlerContainer handlerContainer) {
		if (tables == null) {
			throw new IllegalArgumentException("���滻����Ϊ��");
		}
		if (modifiableList.size() == 0) {
			throw new IllegalArgumentException("δ��ʼ����sql����ֱ�����");
		}
		List<SqlAndTableAtParser> retSqls = new ArrayList<SqlAndTableAtParser>(
				tables.size());
		for (Map<String, String> table : tables) {
			Result result = process(table, args, modifiableList, tableName,
					handlerContainer);

			SqlAndTableAtParser sqlAndTableAtParser = new SqlAndTableAtParser();
			sqlAndTableAtParser.sql = result.resultSQL;
			sqlAndTableAtParser.table = table;
			sqlAndTableAtParser.modifiedMap = result.changeParam;
			retSqls.add(sqlAndTableAtParser);
		}
		return retSqls;
	}

	private static class Result {

		public Result(Map<Integer, Object> changeParam, String resultSQL) {
			super();
			this.changeParam = changeParam;
			this.resultSQL = resultSQL;
		}

		final Map<Integer, Object> changeParam;
		final String resultSQL;
	}

	public Result process(Map<String, String> table, List<Object> args,
			List<Object> modifiableTableName, Set<String> originalTable,
			HandlerContainer handlerContainer) {
		// hack.
		boolean allowChangePageNumber = handlerContainer
				.isAllowChangePageNumber();
		if (allowChangePageNumber) {
			PageWrapperCommon skipPage = null;
			// oracle��max����mysql��range�����Ա�ע�뵽�����Ϊmax��range����ͬʱ����
			// �ⲿ�ִ���������������ҵ�������ʼ��ֵ���ͽ�����ֵ��
			PageWrapperCommon maxOrMaxPage = null;
			for (Object obj : modifiableTableName) {
				if (obj instanceof SkipWrapper) {
					if (skipPage == null) {
						skipPage = (SkipWrapper) obj;
					} else if (skipPage.getVal(args) < ((SkipWrapper) obj)
							.getVal(args)) {
						// ��ǰֵ����snapshot�е�ֵʱ����mySelect,Select
						// MyUpdate,Update��������Ĳ���һ�¡�
						skipPage = (SkipWrapper) obj;
					} else {
						// ��ǰֵС�ڵ���snap�е�ֵ��ʲôҲ������
					}
				} else if (obj instanceof MaxWrapper
						|| obj instanceof RangeWrapper) {
					if (maxOrMaxPage == null) {
						maxOrMaxPage = (PageWrapperCommon) obj;
					} else if (maxOrMaxPage.getVal(args) < ((PageWrapperCommon) obj)
							.getVal(args)) {
						// ��ǰֵ����snapshot�е�ֵʱ����mySelect,Select
						// MyUpdate,Update��������Ĳ���һ��
						maxOrMaxPage = (PageWrapperCommon) obj;
					} else {
						// ��ǰֵС�ڵ���snap�е�ֵ��ʲôҲ������
					}
				} else {
					// String ���������
				}
			}
			if (skipPage != null) {
				skipPage.setCanBeChanged(true);
			}
			if (maxOrMaxPage != null) {
				maxOrMaxPage.setCanBeChanged(true);
			}

		}
		Map<Integer, Object> changeParam = null;
		// �滻�����
		StringBuilder sb = new StringBuilder();
		for (Object obj : modifiableTableName) {
			if (obj instanceof String) {
				// ����String
				// sb.append(getTable(table,originalTable));
				sb.append(obj.toString());
			} else if (obj instanceof PageWrapper) {
				// �����ҳ
				// needAppendtableName=false;
				RangePlaceHandler rangePlaceHandler = handlerContainer
						.getRangePlaceHandler();
				// ��ʼ����
				if (changeParam == null) {
					changeParam = new HashMap<Integer, Object>(2);
				}
				String str = rangePlaceHandler.changeValue((PageWrapper) obj,
						changeParam);
				sb.append(str);
			} else if (obj instanceof ReplacableWrapper) {
				PlaceHolderReplaceHandler placeHolderHandler = handlerContainer
						.getPlaceHolderPlaceHandler(obj);
				String replacedTable = placeHolderHandler.getReplacedString(
						table, (ReplacableWrapper) obj);
				// ����index������
				sb.append(replacedTable);

			} else {
				throw new IllegalStateException("should not be here");
			}

		}
		if (changeParam == null)
			changeParam = Collections.emptyMap();
		Result result = new Result(changeParam, sb.toString());
		return result;
	}

	public abstract List<OrderByEle> nestGetOrderByList();

	public abstract List<OrderByEle> nestGetGroupByList();

	// public TableName getTbName() {
	// return tbName;
	// }

	public boolean isDML() {
		return true;
	}

	public void addHint(Hint hint) {
		hints.add(hint);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.taobao.tddl.sqlobjecttree.SqlParserResult#getMax(java.util.List)
	 */
	public int getMax(List<Object> param) {
		// ��Ĭ�ϵ�����£��Ǹ�mysql��ʵ�֣�oracleʵ��������������
		// TODO:���������һ��MySQLCommon���������߼��Ƚ�������
		int skip = getSkip(param);
		int max = DEFAULT_SKIP_MAX;
		int range = getRangeOrMax(param);
		if (range != DMLCommon.DEFAULT_SKIP_MAX) {
			if (skip != DMLCommon.DEFAULT_SKIP_MAX) {
				if (range >= 0) {
					if (skip >= 0) {
						/*
						 * Ŀǰmysqlʵ���߼��Ƚ���,��Ϊmysql�����Ǵ�1��ʼ�� ��javaList�����ʵ�ִ�0��ʼ
						 * ��˶���mysql��java��list�Ķ���Ӧ����ֵ++Ȼ��--����˲���
						 * 
						 * oralce�е�maxֵ����mysql�е�limit m,n��ϵ�е�m+n��
						 */
						max = skip + range;
					} else {
						throw new IllegalArgumentException("skip������Ϊ��ֵ");
					}
				} else {
					throw new IllegalArgumentException("max��rangeֵ������Ϊ��ֵ");
				}
			} else {
				// ���û��skipֵ����max=range
				max = range;
			}
		}

		if (skip < 0 && skip != DMLCommon.DEFAULT_SKIP_MAX) {
			throw new IllegalArgumentException("skip������Ϊ��ֵ");
		}
		return max;
	}

	public ComparativeMapChoicer getComparativeMapChoicer() {
		return this;
	}

	/**
	 * FIXME�� �Ѿ���Distinct ������һ�������� Distinct �ͺ����column��һ����Ϊһ��Function column,
	 * �˴������������Distinct����Ϊһ�����࣬����Ϊ����ķֿ�ֱ�ʱ�Ĵ�����Ҫ����������������в���!
	 * ��Distinctֻ��Ϊ���࣬����Parser���������������SQL��Stringʱ�����������Distinct--add by
	 * mazhidan.pt
	 */
	protected Distinct distinct = null;

	public void setDistinct(Distinct distinct) {
		this.distinct = distinct;
	}

	@Override
	public List<String> getDistinctColumn() {
		if (null != distinct) {
			if (null != distinct.getColumns()) {
				return distinct.getColumns().getColList2Str();
			}
		}
			return null;
	}

	@Override
	public boolean hasHavingCondition() {
		return false;
	}

	@Override
	public List<InExpressionObject> getInExpressionObjectList() {
		return this.inObjList;
	}
}
