package com.roofstone.utils;

public interface ILog {
	
	public static final int ERROR=5;
	public static final int WARN=7;
	public static final int INFO=9;
	public static final int DEBUG=11;
	public static final int TRACE=13;
	
	default String levelName(int level) {
		if(level>=TRACE) return "[trc]";
		if(level>=DEBUG) return "[dbg]";
		if(level>=INFO) return "[inf]";
		if(level>=WARN) return "[wrn]";
		return "[ERR]";
	}
		
	boolean is(int level);
	
	default boolean isDebug() {
		return is(DEBUG);
	}
	
	void log(int level, Txt msg);
	
	default void error(Txt msg) {
		log(ERROR, msg);
	}
	
	default void info(Txt msg) {
		log(INFO, msg);
	}
	
	default void debug(Txt msg) {
		log(DEBUG, msg);
	}
}
