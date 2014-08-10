package com.codeandstrings.niohttp.exceptions.http;

public class RequestEntityTooLargeException extends HttpException {
	private static final long serialVersionUID = 6676247325789548938L;
	
	public RequestEntityTooLargeException() {
		super(413, "Request Entity Too Large");
	}
}
