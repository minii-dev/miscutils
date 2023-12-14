package com.roofstone.utils;

public class HttpData {

	String string;
	int chunkSize = 128000;
	String httpCharset = "UTF-8";
	String contentType;
	
	boolean isSkipReading;
	
	private String loginData;

	public HttpData(String string) {
		this.string = string;
	}

	public void setLoginData(String loginData) {
		this.loginData = loginData;
	}
	
	public String getLoginData() {
		return loginData;
	}
	

	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	public void setHttpCharset(String httpCharset) {
		this.httpCharset = httpCharset;
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	public void setSkipReading(boolean isIgnoreReading) {
		this.isSkipReading = isIgnoreReading;
	}
}
