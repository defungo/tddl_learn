package com.taobao.tddl.sqlobjecttree;

import java.math.BigDecimal;
import java.util.List;

import com.taobao.tddl.sqlobjecttree.common.value.BindVar;

public abstract class PageWrapperCommon implements PageWrapper
{
	// ���Ա��滻��ֵ
	protected boolean canBeChanged;
	protected Integer index;
	// ��ǰ������List�е�ֵ
	protected Number value;

	public PageWrapperCommon(Object obj)
	{
		if (obj instanceof BindVar)
		{
			index = ((BindVar) obj).getIndex();
		} else if (obj instanceof Number)
		{
			this.value = (Number) obj;
		} else
		{
			throw new IllegalStateException("����ͨ���󶨱�����sql��ȡlimit����");
		}
	}

	public String toString()
	{
		if (index != null)
		{
			return "?";
		} else if (value != null)
		{
			return value.toString();
		} else
		{
			throw new IllegalStateException("��Ӧ�ó���û��ֱֵ��д��sql,��Ҳû��index�����");
		}
	}

	public Long getVal(List<Object> argument)
	{
		Number temp = null;
		if (argument == null)
		{
			throw new IllegalArgumentException("����Ϊ��");
		}
		if (index != null)
		{
			Object obj = argument.get(index);
			if (obj instanceof Number)
			{
				temp = (Number) obj;
			} else
			{
				throw new IllegalArgumentException("index ֵΪ" + index
						+ "�Ĳ�����Ϊnumber����");
			}
		} else if (value != null)
		{
			if (value != null)
			{
				temp = value;
			} else
			{
				throw new IllegalStateException("��Ӧ�ó���û��ֱֵ��д��sql,��Ҳû��index�����");
			}
		} else
		{
			throw new IllegalStateException("��ҳ������ֵ");
		}
		if (temp instanceof Long || temp instanceof Integer)
		{
			return temp.longValue();
		} else if (temp instanceof BigDecimal)
		{
			return ((BigDecimal) temp).longValueExact();
		} else
		{
			throw new IllegalArgumentException("��ҳ����ֻ֧��int long");
		}
	}

	public Number getValue()
	{
		return value;
	}

	public void setValue(Number value)
	{
		this.value = value;
	}

	public boolean canBeChange()
	{
		return canBeChanged;
	}

	public void setCanBeChanged(boolean canBeChanged)
	{
		this.canBeChanged = canBeChanged;
	}

	public Integer getIndex()
	{
		return index;
	}

	public void setIndex(Integer index)
	{
		this.index = index;
	}

}
