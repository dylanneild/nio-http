package com.codeandstrings.niohttp.exceptions.http;

public class MethodNotAllowedException extends HttpException {

	private String method;
	private static final long serialVersionUID = 8434405527351005098L;
	
	public MethodNotAllowedException(String method) {
		super(405, "Method Not Allowed");
		this.method = method;
	}

	@Override
	public String toString() {
		return "MethodNotAllowedException [method=" + method + "],  " + super.toString();
	}		

}
