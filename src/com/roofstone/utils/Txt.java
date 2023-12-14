package com.roofstone.utils;

public class Txt {

	private int id;
	private String template;
	private Object[] args;

	public Txt(int id, String template, Object ... args) {
		this.id = id;
		this.template = template;
		this.args = args;
	}
	
	public String getMessage() {
		return TextFormatter.f(template, args);
	}
	
	public int getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return getMessage();
	}
}
