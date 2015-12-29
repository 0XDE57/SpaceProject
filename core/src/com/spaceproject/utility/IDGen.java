package com.spaceproject.utility;

public class IDGen {
	static int id = 0;
	
	public static int get() {
		return id++;
	}
}
