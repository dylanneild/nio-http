package com.codeandstrings.niohttp.exceptions.http;

public class BadRequestException extends HttpException {
	private static final long serialVersionUID = -3394743250952717129L;

	public BadRequestException() {
		super(400, "Bad Request");
	}
	
}
