package com.codeandstrings.niohttp.exceptions.http;

public class RequestEntityTooLargeException extends HttpException {
	
	private static final long serialVersionUID = 6676247325789548938L;
	private int requestSize;
	
	public RequestEntityTooLargeException(int requestSize) {
		super(413, "Request Entity Too Large");
	}
	
	public RequestEntityTooLargeException() {
		this(0);
	}

	@Override
	public String toString() {
		return "RequestEntityTooLargeException [requestSize=" + requestSize
				+ "], + " + super.toString();
	}
		
}
