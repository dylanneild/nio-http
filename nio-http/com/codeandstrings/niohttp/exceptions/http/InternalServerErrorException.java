package com.codeandstrings.niohttp.exceptions.http;

public class InternalServerErrorException extends HttpException {
	
	private static final long serialVersionUID = 7094810938188896707L;

	public InternalServerErrorException() {
		super(500, "Internal Server Error");
	}

}
