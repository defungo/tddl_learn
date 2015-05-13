package com.taobao.tddl.sqlobjecttree.oracle;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.taobao.tddl.common.sqlobjecttree.Column;
import com.taobao.tddl.common.sqlobjecttree.SQLFragment;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.interact.sqljep.ComparativeAND;
import com.taobao.tddl.interact.sqljep.ComparativeOR;
import com.taobao.tddl.sqlobjecttree.DMLCommon;
import com.taobao.tddl.sqlobjecttree.Expression;
import com.taobao.tddl.sqlobjecttree.RowJepVisitor;
import com.taobao.tddl.sqlobjecttree.Select;
import com.taobao.tddl.sqlobjecttree.TableName;
import com.taobao.tddl.sqlobjecttree.common.TableNameSubQueryImp;
import com.taobao.tddl.sqlobjecttree.common.expression.ComparableExpression;
import com.taobao.tddl.sqlobjecttree.common.expression.ExpressionGroup;
import com.taobao.tddl.sqlobjecttree.common.expression.OrExpressionGroup;

/**
 * ����oracle skip ��maxֵ�Ĺ�����
 * @author shenxun
 *
 */
public class SkipMaxUtils {
	
    /**
     * mysql: limit 0,20
     * ʵ�����������ݿ��е�ȡ��: x>0 and x<=20
     * ��Ӧjava��limitTo limitFrom:lTo=1-1 lFrom=21-1(��Ϊmysql��һ�����ݶ�Ӧjava�����0)
     * @param co
     * @return
     */
    public static int getRowNumSkipToInt(Comparative co) {
    	if(co==null){
    		return DMLCommon.DEFAULT_SKIP_MAX;
    	}
        if (co instanceof ComparativeAND) {
            int snapSkip = DMLCommon.DEFAULT_SKIP_MAX;
            ComparativeAND coAnd = (ComparativeAND) co;
            List<Comparative> compList = coAnd.getList();

            for (Comparative c : compList) {
                int tempRowNum = getRowNumSkipToInt(c);
                if (tempRowNum != DMLCommon.DEFAULT_SKIP_MAX && tempRowNum > snapSkip) {
                    snapSkip = tempRowNum;
                }
            }
            return snapSkip;
        } else if (co instanceof ComparativeOR) {
            throw new IllegalArgumentException("��֧��һ��sql��rownum����or�����");
        } else if (co.getComparison() == Comparative.Equivalent || co.getComparison() == 0) {
            throw new IllegalArgumentException("rownumĿǰ��֧��=��ϵ");
        } else if (co.getComparison() == Comparative.GreaterThan) {
            //> �����
            return getComparativeToInt(co);
        } else if (co.getComparison() == Comparative.GreaterThanOrEqual) {
            //>=�����
            return getComparativeToInt(co) - 1;
        }
        return DMLCommon.DEFAULT_SKIP_MAX;
//        else {
//          //������ǩ�����
//        }
    }
   public static int getRowNumMaxToInt(Comparative co) {
		if(co==null){
    		return DMLCommon.DEFAULT_SKIP_MAX;
    	}
        if (co instanceof ComparativeAND) {
            int snapSkip = DMLCommon.DEFAULT_SKIP_MAX;
            ComparativeAND coAnd = (ComparativeAND) co;
            List<Comparative> compList = coAnd.getList();

            for (Comparative c : compList) {
                int tempRowNum = getRowNumMaxToInt(c);
                if (tempRowNum != DMLCommon.DEFAULT_SKIP_MAX && tempRowNum > snapSkip) {
                    snapSkip = tempRowNum;
                }
            }
            return snapSkip;
        } else if (co instanceof ComparativeOR) {
            throw new IllegalArgumentException("��֧��һ��sql��rownum����or�����");
        } else if (co.getComparison() == Comparative.Equivalent || co.getComparison() == 0) {
            throw new IllegalArgumentException("rownumĿǰ��֧��=��ϵ");
        } else if (co.getComparison() == Comparative.LessThan) {
            //< �����
            return getComparativeToInt(co)-1;
        } else if (co.getComparison() == Comparative.LessThanOrEqual) {
            //<=�����
            return getComparativeToInt(co);
        } 
        return DMLCommon.DEFAULT_SKIP_MAX;
//        else {
//		  �����ıȽ����
//            throw new IllegalStateException("��Ӧ����˴�");
//        }
    }
    private static int getComparativeToInt(Comparative co) {
        Comparable<?> ctemp = co.getValue();
        if (ctemp instanceof Integer) {
            return (Integer) ctemp;
        }
        if (ctemp instanceof BigDecimal) {
            return ((BigDecimal) ctemp).intValueExact();
        } else {
            throw new IllegalArgumentException("Ŀǰֻ֧��bigDecimal��integer���͵�rownum����,��ǰ����Ϊ:"+ctemp.getClass()+"|"+ctemp);
        }
    }

	/**
	 * �ҵ����еİ���rownum��������Ĺ�ϵ���飬�ŵ�һ��ExpressionGroup��
	 */
	protected  static ComparativeAND buildRownumGroup(OrExpressionGroup orExpressionGroup,
			List<TableName> tbNames,Map<String, SQLFragment>  aliasToSQLFragementMap){
	
		ExpressionGroup rownum = new ExpressionGroup();
		
		nestedBuildRownumGroup(orExpressionGroup, tbNames, rownum,aliasToSQLFragementMap);
		
		RowJepVisitor visitor = new RowJepVisitor();

		rownum.eval(visitor, true);
		
		Map<String, Comparative> map = visitor.getComparable();
		
		ComparativeAND and=new ComparativeAND();
		
		for(Comparative com:map.values()){
			//��Ϊ�б���ָ��rownum�����Բ��ܼ򵥵�ʹ��map.get("rownum")����ȡĿ��Comparative
			and.addComparative(com);
		}
		
		return and;
	}
	
    
    /**
     * ���������ѭ�������������������������ȡrownum�ı��ʽ
     * ����rownum�ı�����֯���ʽ����rownum rn,Ȼ���������ط�rn <=10������Ҳ���ҳ���
     * @param source
     * @param tabNames
     * @param targetRownumGroup
     * @param aliasMap
     */
    public static void nestedBuildRownumGroup(ExpressionGroup source,
			List<TableName> tabNames, ExpressionGroup targetRownumGroup,Map<String,SQLFragment> aliasMap) {
		if (targetRownumGroup != null) {
			for (TableName tname : tabNames) {
				//���ұ����Ǹ�select�ġ�
				if (tname instanceof TableNameSubQueryImp) {
					Select select = ((TableNameSubQueryImp) tname)
							.getSubSelect();
					nestedBuildRownumGroup(select.getWhere().getExpGroup(), select
							.getTbNames(), targetRownumGroup,aliasMap);
				}
				// else{
				// //�򵥱�����������
				// }
			}
			putRowNumIntoRownumExpGroup(source, targetRownumGroup,aliasMap);
		}
	}

	/**
	 * ר�Ŵ���һ��select�����е�where����
	 * ƴװ������ֻ������and��ϵ����Ϊrownum��α�У��벻����ʲô�������rownum���select����rownum >2 or rownum<2����������
	 * @param source
	 * @param targetRowNumGroup
	 * @param aliasMap ����Map,��ű���->sqlԪ�ص�ӳ�䡣
	 */
	protected static void putRowNumIntoRownumExpGroup(ExpressionGroup source,
			ExpressionGroup targetRowNumGroup,Map<String,SQLFragment> aliasMap) {
		List<Expression> exps = source.getExpressions();
		for (Expression exp : exps) {
			if (exp instanceof ExpressionGroup) {
				// ���ʽ��Ƕ�ס�ѭ�������ڲ�����
				putRowNumIntoRownumExpGroup(((ExpressionGroup) exp),
						targetRowNumGroup,aliasMap);
			} else if (exp instanceof ComparableExpression) {
				Object left = ((ComparableExpression) exp).getLeft();
				if (left instanceof Column) {
					//���һ����˵��������������������Ĭ�����صĹ���
					//TODO:�����ҵߵ���Ҫ����취�ڲ�Ӱ�쵱ǰ���ܵ�������׳��쳣���׵�ʱ��ҪС��col=col+1���������
					String colName = ((Column) left).getColumn();
					if (colName != null) {
						putRownumColumnToRownumExpression(targetRowNumGroup,
								exp, colName);
						//����rownum���������
						SQLFragment fragement = aliasMap.get(colName
								.toUpperCase());
						if (fragement instanceof Column) {
							String tempCol = ((Column) fragement).getColumn();
							//��һ�±�������Ӧ�����������Ƿ���һ��rownum�С�����Ǿ���ӵ�rownum������
							if (tempCol != null) {
								putRownumColumnToRownumExpression(
										targetRowNumGroup, exp, tempCol);
							}
						}
						// else{
						// //������߲��ַ�column����������ԣ���Ҫ�����ֿ��ܣ���һ�������ҵߵ����ڶ����Ƿ�rownum��������
						// }
					}
				}
				Object right = ((ComparableExpression) exp).getRight();
				if (right instanceof Select) {
					// ����ұ���һ��Select
					// �ݹ�Ĵ�select�г�ȡExpGroup
					Select select = ((Select) right);
					nestedBuildRownumGroup(select.getWhere().getExpGroup(), select
							.getTbNames(), targetRowNumGroup,aliasMap);
				}
				// else{
				// //�����������󶨱�����������������ԡ�
				// }
			}
		}
	}

	private static void putRownumColumnToRownumExpression(
			ExpressionGroup targetRowNumGroup, Expression exp, String colName) {
		if (colName.equalsIgnoreCase("rownum")) {
			// �����rownum,����RowNumר��ExpGroup��.
			// ��Ϊrownum��α�У��������������Ĵ�������
			targetRowNumGroup.addExpression(exp);
			((ComparableExpression)exp).setRownum(true);
		}
	}
}
