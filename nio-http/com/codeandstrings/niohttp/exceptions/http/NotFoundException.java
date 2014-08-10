package com.codeandstrings.niohttp.exceptions.http;

public class NotFoundException extends HttpException {
	
	private static final long serialVersionUID = -2115517559186741760L;

	public NotFoundException() {
		super(404, "Not Found");
	}

}
