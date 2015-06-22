package com.ggikko.chattingserver;

import java.util.StringTokenizer;

public class Test {
	
	private int hi = 123;
	String hi2 = "babo|456|789|kkkk";
	
	
	public static void main(String[] args) {
		
		Test test = new Test();
		Integer integer = new Integer(test.hi);
		
		StringTokenizer st = new StringTokenizer(test.hi2, "|");
		String gogo = st.nextToken();
		int gogo2 = Integer.parseInt(st.nextToken());
		int gogo3 = integer.parseInt(st.nextToken());
		
		Test test7 = new Test();
		Test test2 = new Test();
		Test test3 = new Test();
		Test test4 = new Test();
		Test test5 = new Test();
		
		System.out.println(test);
		System.out.println(test7);
		System.out.println(test3);
		System.out.println(test5);
		System.out.println(test4);
		System.out.println(test5);
		
		
		
		
	}

}
