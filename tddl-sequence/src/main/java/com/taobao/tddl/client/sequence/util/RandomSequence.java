package com.taobao.tddl.client.sequence.util;

import java.util.Random;

import com.taobao.tddl.client.sequence.exception.SequenceException;


public class RandomSequence {
	/**
	 * ��������0~n-1��n����ֵ���������
	 * @param n
	 * @return
	 * @throws SequenceException 
	 */
	public static int [] randomIntSequence(int n) throws SequenceException
	{	
		if(n<=0)
		{
			throw new SequenceException("����������з�ΧֵС�ڵ���0");
		}
		int num[] =new int[n];
		for(int i=0;i<n;i++)
		{
			num[i]=i;
		}
		if(n==1)
		{
			return num;
		}
		Random random=new Random();
		if(n==2 && random.nextInt(2)==1) //50%�ĸ��ʻ�һ��
		{
			int temp=num[0];
			num[0]=num[1];
			num[1]=temp;
		}
		
		for(int i=0;i<n+10;i++)
		{
			int rindex=random.nextInt(n);//����0~n-1�������
			int mindex=random.nextInt(n);
			int temp=num[mindex];
			num[mindex]=num[rindex];
			num[rindex]=temp;
		}
		return num;
	}
}
