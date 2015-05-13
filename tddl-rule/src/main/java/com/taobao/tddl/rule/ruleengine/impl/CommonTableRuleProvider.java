package com.taobao.tddl.rule.ruleengine.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tddl.common.exception.checked.CantFindPositionByParamException;
import com.taobao.tddl.common.exception.checked.ComparativeArraysOutOfBoundsException;
import com.taobao.tddl.common.exception.checked.ParseSQLJEPException;
import com.taobao.tddl.common.exception.checked.TDLCheckedExcption;
import com.taobao.tddl.common.exception.runtime.CantFindTargetTabRuleTypeException;
import com.taobao.tddl.common.exception.runtime.NotSupportException;
import com.taobao.tddl.common.sequence.Config;
import com.taobao.tddl.common.util.NestThreadLocalMap;
import com.taobao.tddl.interact.sqljep.Comparative;
import com.taobao.tddl.interact.sqljep.ComparativeAND;
import com.taobao.tddl.interact.sqljep.ComparativeBaseList;
import com.taobao.tddl.interact.sqljep.ComparativeOR;
import com.taobao.tddl.rule.ruleengine.TableRuleProvider;
import com.taobao.tddl.rule.ruleengine.TableRuleType;
import com.taobao.tddl.rule.ruleengine.entities.inputvalue.TabRule;
import com.taobao.tddl.rule.ruleengine.impl.type.TableNameTypeHandler;
import com.taobao.tddl.rule.ruleengine.impl.type.TypeRegister;

/**
 * ���󹫹����࣬��Ҫ���ڴ���date���͵Ķ���
 * @author shenxun
 *
 */
public abstract class CommonTableRuleProvider implements TableRuleProvider {
	protected final static String CALENDAR="CALENDAR";
	private static final Log log = LogFactory.getLog(CommonTableRuleProvider.class);
	protected final static  int LESS_GREAT=1;
	protected final static  int LESS_OR_EQUAL_GREAT_OR_EQUAL=0;
	/**
	 * ��д�˷����������ֱ�ӵ��ö�Ӧcalendar����ĳ�����
	 * 
	 * @return
	 */
	protected  int getCalendarType(){
		return -1000;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.taobao.tdl.client.ruleEngine.tableDetector.TableRuleProvider#getTables
	 * (java.lang.Comparable, java.util.Map,
	 * com.taobao.tdl.client.ruleEngine.entities.inputValue.TabRule,
	 * java.lang.String)
	 * �ɷ�����ֻ���ڲ����ˡ�
	 */
	public Set<String> getTables(Comparable<?>[] row,
			Map<String, Integer> position, TabRule tab, String vTabName)
			throws CantFindPositionByParamException, ParseSQLJEPException,
			ComparativeArraysOutOfBoundsException {
		log.debug("Thread is in TabProvider's getTables method now");

		validTabRule(tab);
		Integer posInt = position.get(tab.getParameter());
	
		if (posInt == null) {
			throw new CantFindPositionByParamException(tab.getParameter());
		}
		Comparable<?> comparable = row[posInt.intValue()];
		return parseTableNameObj(tab, vTabName, comparable,null);

	}
	public Set<String> getTables(Map<String, Comparative> map, TabRule tab, String tabName, Config config)
			throws TDLCheckedExcption {
		validTabRule(tab);
		
		Comparable<?> comparable= null;
			comparable = map.get(tab.getParameter());
		return parseTableNameObj(tab, tabName, comparable,config);
	}
	protected Set<String> parseTableNameObj(TabRule tab, String vTabName,
			Comparable<?> comparable,Config config) throws ParseSQLJEPException {
		if (comparable instanceof Comparative) {
			//if current input is a subType of Comparative
			return analyzeComparative(tab, vTabName, comparable,config);

		} else if (comparable == null) {
			//if comparable is null,return DefaultTable()
			Set<String> temp = getDefaultTabSet(tab);
			return temp;
		} else {
			throw new NotSupportException("��֧�ֳ���Comparative���ͺ��������͵��������");
		}
	}

	protected Set<String> getDefaultTabSet(TabRule tab) {
		Set<String> temp = new HashSet<String>();
		temp.addAll(getDefaultTabCollection(tab));
		return temp;
	}

	protected Collection<String> getDefaultTabCollection(TabRule tab) {
		return Collections.emptySet();
	}

	/**
	 * analyze a instance of Comparative .temporary we now only support 
	 * @param tab
	 * @param vTabName
	 * @param comparable
	 * @return
	 * @throws ParseSQLJEPException
	 */
	private Set<String> analyzeComparative(TabRule tab, String vTabName,
			Comparable<?> comparable,Config config) throws ParseSQLJEPException {
		Comparative comparative;
		comparative = (Comparative) comparable;
		if (comparative instanceof ComparativeAND) {
			log.debug("comparative is a instance of and ");
			// and����򵥵ĵ��������ڣ���ֱ�Ӹ㶨
			ComparativeAND and = (ComparativeAND) comparative;
			List<Comparative> list = and.getList();
			Set<String> temp = new HashSet<String>();
			getXxxfixedByAndRange(temp,list, tab,vTabName);
			return temp;
		} else if (comparative.getComparison() == Comparative.Equivalent) {
			Set<String> temp = addAEqComparabToXXXFix(tab, vTabName,
					comparative,tab.getOffset(), config);
			return temp;
		} else if (comparative instanceof ComparativeOR) {
			ComparativeOR or=(ComparativeOR)comparative;

			Set<String> temp = new HashSet<String>();
			
			List<Comparative> list=or.getList();
			for(Comparative comp:list){
				temp.addAll(analyzeComparative(tab, vTabName, comp,config));
			}
			log.info("ComparativeOr�����");
			return temp;
			//bigFix by shenxun 5 25 :�����з��ֻ��п���һ��comparative.getvalue()��ȡ����Ȼ��comparative������������������bugfix
		}else if(comparative.getValue() instanceof Comparative){
			return parseTableNameObj(tab, vTabName,comparative.getValue(), config);
		}else {
			Set<String> temp = getDefaultTabSet(tab);
			return temp;
		}
	}

	/**
	 * ��д�˷���ʱҪע��offset�Ĵ���
	 * @param tab
	 * @param vTabName
	 * @param comparative
	 * @param offset
	 * @param config 
	 * @return
	 */
	protected Set<String> addAEqComparabToXXXFix(TabRule tab,
			String vTabName, Comparative comparative,int offset, Config config) {
		// =�����Ҳ�ܸ㶨
		Date date = getDateFromComparative(comparative);
		Integer calType = getCalendarType();
		if (calType == null) {
			throw new CantFindTargetTabRuleTypeException(tab
					.getExpFunction());
		}
//		//Ϊ��������ܣ�����һ��map����Calendar
//		Calendar cal=(Calendar)NestThreadLocalMap.get("CTRP_Calendar");
//		if(cal==null){
//			cal = Calendar.getInstance();
//			NestThreadLocalMap.put("CTRP_Calendar", cal);
//		}
		Calendar cal = getCalendarInThreadLocalMap();
		cal.setTime(date);
		int retInt = getReturnInt(cal, calType);
		
		Set<String> temp = new HashSet<String>();
		String n=processOne(retInt+offset, tab, vTabName);
		if(n!=null){
			temp.add(n);
		}
		return temp;
	}

	protected Date getDateFromComparative(Comparative comparative) {
		Date date = null;
		Comparable<?> comp = comparative.getValue();
		// FIXME:��Ȼд�����ˣ�������ҪΪ�������������������ع�
//		if (comp instanceof CompableBindValue) {
//			CompableBindValue var = (CompableBindValue) comp;
//			date = (Date) var.getBindVal();
//		} else {
			date = (Date) comp;
//		}
		return date;
	}

	protected void validTabRule(TabRule tab) {
		if (tab.getTableType() == null) {
			throw new IllegalArgumentException(
					"�����ҵ�tableRule��tableType���ԣ���������Ǳ����");
		}
	}

	protected String processOne(Object xxxfix,TabRule tab,String logicTab){
		String vTab = logicTab;
		String physicsTab = null;
		TableRuleType tableType = tab.getTableType();
		TableNameTypeHandler handler = TypeRegister
				.getTableNameHandler(tableType);

		physicsTab = handler.buildOnePhsicTab(xxxfix, tab, vTab);

		return physicsTab;
	}
	protected void processes( Set<String> tabs,List<Object> xxxfixes, TabRule tab,
			String logicTab) {
		String vTab = logicTab;
		List<String> physicsTab = null;
		TableRuleType tableType = tab.getTableType();
		TableNameTypeHandler handler = TypeRegister
				.getTableNameHandler(tableType);

		physicsTab = handler.buildPhysicTab(xxxfixes, tab, vTab);
		tabs.addAll(physicsTab);
	}

	/**
	 * ֻ֧����һ�����ڻ���ڵ��ڣ���һ��С�ڻ�С�ڵ��ڵ���������� xxxfix=suffix or prefix.
	 * 
	 * @param comps
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private void getXxxfixedByAndRange(Set<String> temp,List<Comparative> comps,
			TabRule tab,String vTabName) throws ParseSQLJEPException {
		Comparative start = null;
		Comparative end = null;
		long time = 0;
		if (log.isDebugEnabled()) {
			time = System.currentTimeMillis();
		}
		// ����start��end
		if (comps.size() == 2) {
			
			for (Comparative c : comps) {
				if(c instanceof ComparativeBaseList){
					throw new IllegalArgumentException("����������and����3��������and ( or )������������Ϊ�ֱ�����");
				}
				if (c.getComparison() == Comparative.GreaterThan
						|| c.getComparison() == Comparative.GreaterThanOrEqual) {
					if (start == null) {

						start = c;
					} else {
						temp.addAll(getDefaultTabCollection(tab));
						// ������Ǵ��ڻ���ڵ�����Default
						log.info("�����ڻ���ڵ������ֿ��������");
						return ;
					}
				} else if (c.getComparison() == Comparative.LessThan
						|| c.getComparison() == Comparative.LessThanOrEqual) {
					if (end == null) {

						end = c;
					} else {
						temp.addAll(getDefaultTabCollection(tab));
						//�����С�ڻ�С�ڵ��ڵ���r���tdefault
						log.info("��С�ڻ�С�ڵ������ֿ���������");
						return ;
					}
				} else {
					throw new NotSupportException("and ��������������������һ������Ϊ���ڡ�");
				}
			}
		} else {
			throw new NotSupportException("and������������");
		}
		Comparable st = start.getValue();

		Comparable ed = end.getValue();
//		if (st instanceof CompableBindValue) {
//			st = ((CompableBindValue) st).getBindVal();
//		}
//		if (ed instanceof CompableBindValue) {
//			ed = ((CompableBindValue) ed).getBindVal();
//		}
		openRangeCheck(tab,st, ed);
		log.debug("start:" + start.getValue() + " comparative signal:"
				+ start.getComparison() + " end: " + end.getValue()
				+ " comparative signal:" + start.getComparison());
		List<Object> retInt=getXxxfixlist(start, end,tab.getOffset(), tab);
		processes(temp, retInt, tab, vTabName);
		if (log.isDebugEnabled()) {
			log.debug("calculation xxxfix finish,elapsed time:"
					+ (System.currentTimeMillis() - time));
			log.debug("ret xxxfix tabSize" + temp.size());
		}
		return ;

	}

	@SuppressWarnings("unchecked")
	protected void openRangeCheck(TabRule tabRule,Comparable st, Comparable ed) {
		if (st.compareTo(ed) > 0) {
			log.info("�������ֵ��С����Сֵ�Ŀ���������");
			return ;
		}
	}

	/**
	 * ��ͨ����д�˷���֧�ָ���Ļ���startһ��ֵ��endһ��ֵ��β׺����߼�����д�˷�����Ҫע���offset�Ĵ���
	 * @param start
	 * @param end
	 * @param tab TODO
	 * @param temp
	 */
	protected List<Object> getXxxfixlist(Comparative start, Comparative end,int offset, TabRule tab) {

		TreeSet<Integer> ret = new TreeSet<Integer>();
		Calendar cal = getCalendarInThreadLocalMap();
		int startType = getType(start);
		int endType = getType(end);
		// ����Ҳ�����޸������Ǻܺÿ�
		Date endDate = getDateFromComparative(end);
		int calType = getCalendarType();
		Date st = getDateFromComparative(start);
		int compRes=st.compareTo(endDate);
		if(compRes==0){
			if(startType==LESS_OR_EQUAL_GREAT_OR_EQUAL&&endType==LESS_OR_EQUAL_GREAT_OR_EQUAL){
				//�����������£��н���
				List<Object> li=new ArrayList<Object>(1);
				cal.setTime(st);
				li.add(getReturnInt(cal, calType)+offset);
				return li;
			}else{
				//���գ��������޽���
				return Collections.emptyList();
			}
		}else if(compRes>0){
			return Collections.emptyList();
		}
		cal.setTime(st);
	
		//���stDate
		ret.add(getReturnInt(cal, calType)+offset);
		//������Ӻ��ʱ��
		Calendar anotherCal=(Calendar)cal.clone();
		//��ȡ��ǰʱ�����ڵ�������.��׼��
		anotherCal.clear();
		anotherCal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
		
		int max = cal.getMaximum(calType)+1;
		// ��calendar��ȡʱ���
		for (int i = 0; i < max; i++) {
			anotherCal.add(calType, 1);
			//�������1���ʱ��>��ǰʱ��
			int compResult=anotherCal.getTime().compareTo(endDate);
			if (compResult>0) {
					//����endֵ
					cal.setTime(endDate);
					//����TreeSet��֤��ֵ��Ψһ�ԡ�
					ret.add(getReturnInt(cal, calType)+offset);
					break;
			}else if(compResult == 0 ){
				if(endType == LESS_OR_EQUAL_GREAT_OR_EQUAL){
					//��׼ֵ����endֵ,���endֵ��ȥ
					cal.setTime(endDate);
					ret.add(getReturnInt(cal, calType)+offset);
				}
//				else{
//					endDateС�ڻ�׼ʱ�䣬ͬʱ��׼ʱ����һ����¿�ʼ�����Բ��������µ�һ�졣	
//				}
				
				break;
			}else{
				//������С�ڵ�ǰʱ��
				ret.add(getReturnInt(anotherCal, calType)+offset);
			}
		}
		List<Object> temp=new ArrayList<Object>(ret.size());
		for (Integer i : ret) {
			temp.add(i);
		}
		return temp;
	}
	protected int getReturnInt(Calendar cal,int calType){
		int ret = cal.get(calType);
		return ret;
	}
	protected Calendar getCalendarInThreadLocalMap() {
		Calendar cal;
		cal = (Calendar)NestThreadLocalMap.get(CALENDAR);
		if(cal==null){
			cal = Calendar.getInstance();
			NestThreadLocalMap.put(CALENDAR, cal);
		}
		return cal;
	}

	/**
	 * 1Ϊ>��< 0Ϊ>=��<=
	 * 
	 * @param compDate
	 * @return
	 */
	protected int getType(Comparative compDate) {
		int type = -100;
		if (compDate.getComparison() == Comparative.GreaterThanOrEqual
				|| compDate.getComparison() == Comparative.LessThanOrEqual) {
			type = LESS_OR_EQUAL_GREAT_OR_EQUAL;
		} else {
			type = LESS_GREAT;
		}
		return type;
	}
}
