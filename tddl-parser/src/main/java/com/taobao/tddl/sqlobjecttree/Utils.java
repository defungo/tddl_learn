package com.taobao.tddl.sqlobjecttree;

import java.util.List;
import java.util.Set;

import com.taobao.tddl.common.sqlobjecttree.SQLFragment;
import com.taobao.tddl.common.sqlobjecttree.Value;
import com.taobao.tddl.sqlobjecttree.common.TableNameFunctionImp;
import com.taobao.tddl.sqlobjecttree.common.TableNameImp;
import com.taobao.tddl.sqlobjecttree.common.TableNameSubQueryImp;
import com.taobao.tddl.sqlobjecttree.common.value.UnknowValueObject;



public class Utils {
	private final static Object[] ARRAYOBJ = new Object[] {};


	public static void appendSQL(Object obj, StringBuilder sb) {
		if(obj instanceof Select){
			sb.append("(");
			((SQLFragment) obj).appendSQL(sb);
			sb.append(")");
		}else if (obj instanceof SQLFragment) {
			((SQLFragment) obj).appendSQL(sb);
		} else if(obj instanceof String) {
			sb.append("'").append(obj).append("'");
		} else {
			sb.append(obj);
		}
	}

	public static Comparable<?> getVal(List<Object> args,Object target){
		if(target instanceof Value){
			return ((Value) target).getVal(args);
		}else if(target instanceof Comparable){
			return (Comparable<?>)target;
		}else{
			//Column ����ġ�ֱ�ӷ��ز��ɱȽ϶���
			return UnknowValueObject.getUnknowValueObject();
		}
	}
	public static StringBuilder appendSQLWithList(Set<String> oraTabName,Object obj, StringBuilder sb,List<Object> list) {
		if(obj instanceof Select){
			sb.append("(");
			sb=((SQLFragment) obj).regTableModifiable(oraTabName, list, sb);
			sb.append(")");
		}else if (obj instanceof SQLFragment) {
			sb=((SQLFragment) obj).regTableModifiable(oraTabName, list, sb);
		} else if(obj instanceof String) {
			sb.append("'").append(obj).append("'");
		} else {
			sb.append(obj);
		}
		return sb;
	}

	/**
	 * ��������ListValueObject
	 * 
	 * @param obj
	 * @param sb
	 */
	@SuppressWarnings("rawtypes")
	public static StringBuilder appendSQLListWithList(Set<String> oraTabName,Object obj, StringBuilder sb,List<Object> list) {

		if (obj instanceof List) {
			boolean splider = false;
			sb.append("(");
			for (Object innerObj : (List) obj) {

				if (splider) {
					sb.append(",");
				} else {
					splider = true;
				}
				sb=appendSQLListWithList(oraTabName,innerObj, sb,list);
			}
			sb.append(")");
		} else if (obj != null
				&& ARRAYOBJ.getClass().isAssignableFrom(obj.getClass())) {
			boolean splider = false;
			for (Object innerObj : (Object[]) obj) {
				if (splider) {
					sb.append(",");
				} else {
					splider = true;
				}
				sb=appendSQLWithList(oraTabName,innerObj, sb,list);
			}
		} else {
			sb=appendSQLWithList(oraTabName,obj, sb,list);
		}
		return sb;
	}
	/**
	 * ��������ListValueObject
	 * 
	 * @param obj
	 * @param sb
	 */
	@SuppressWarnings("unchecked")
	public static void appendSQLList(Object obj, StringBuilder sb) {

		if (obj instanceof List) {
			boolean splider = false;
			sb.append("(");
			for (Object innerObj : (List<Object>) obj) {

				if (splider) {
					sb.append(",");
				} else {
					splider = true;
				}
				appendSQLList(innerObj, sb);
			}
			sb.append(")");
		} else if (obj != null
				&& ARRAYOBJ.getClass().isAssignableFrom(obj.getClass())) {
			boolean splider = false;
			for (Object innerObj : (Object[]) obj) {
				if (splider) {
					sb.append(",");
				} else {
					splider = true;
				}
				appendSQL(innerObj, sb);
			}
		} else {
			appendSQL(obj, sb);
		}
	}
	
	public static void toString(Object obj, StringBuilder sb) {
		if (obj instanceof SQLFragment) {
			if(Constant.useToString(obj)) {
				sb.append(obj.toString());
			} else {
				((SQLFragment) obj).appendSQL(sb);
			}
		} else if(obj==null){
			throw new RuntimeException("expression�е�ֵ����Ϊnull,�����ʹ��null��ʹ��DBFunctions�ṩ��NULL");
		} else if(obj instanceof String) {
			sb.append("'").append(obj).append("'");
		} else {
			sb.append(obj);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void listToString(Object obj, StringBuilder sb) {
		if (obj instanceof List) {
			boolean splider = false;
			for (Object innerObj : (List<Object>) obj) {

				if (splider) {
					sb.append(",");
				} else {
					splider = true;
				}
				listToString(innerObj, sb);
			}
		} else if (obj != null
				&& ARRAYOBJ.getClass().isAssignableFrom(obj.getClass())) {
			boolean splider = false;
			for (Object innerObj : (Object[]) obj) {
				if (splider) {
					sb.append(",");
				} else {
					splider = true;
				}
				toString(innerObj, sb);
			}
		} else {
			toString(obj, sb);
		}
	}

	/**
	 * ���һ��tableName������Ϊ���ڴ���sql��walker�����е��� ��˱����Ϊ���̰߳�ȫ�����������ܡ� �����ڳ���sql
	 * walker����������ķ������������
	 * 
	 * @param tableName
	 * @param schemaName
	 * @param alias
	 */
	public static TableName getTableNameAndSchemaName(String tableName, String schemaName,
			String alias) {
		TableNameImp temp = new TableNameImp();
		temp.setTablename(tableName);
		temp.setSchemaName(schemaName);
		temp.setAlias(alias);
		return temp;
	}	
	public static TableName getTableNameAndSchemaName(String tableName, String schemaName,
			String alias, boolean isOracle) {
		TableNameImp temp = new TableNameImp(isOracle);
		temp.setTablename(tableName);
		temp.setSchemaName(schemaName);
		temp.setAlias(alias);
		return temp;
	}
	/**
	 * ���һ��query��tableName���� not thread safe ��Ҫ�ڳ�mysql walker����ĵط����ô˷���
	 * 
	 * @param select
	 * @param alias
	 */
	public static TableName getTableSubQuery(Select select, String alias) {
		TableNameSubQueryImp tab = new TableNameSubQueryImp();
		tab.setSubSelect(select);
		tab.setAlias(alias);
		return tab;
	}
	public static TableName getTableFunction(Function func, String alias) {
		TableNameFunctionImp tab = new TableNameFunctionImp(func, alias);
		return tab;
	}
	public static TableName getTableSubQuery(Select select, String alias, boolean isOracle) {
		TableNameSubQueryImp tab = new TableNameSubQueryImp(isOracle);
		tab.setSubSelect(select);
		tab.setAlias(alias);
		return tab;
	}
	public static TableName getTableFunction(Function func, String alias, boolean isOracle) {
		TableNameFunctionImp tab = new TableNameFunctionImp(func, alias, isOracle);
		return tab;
	}
}
