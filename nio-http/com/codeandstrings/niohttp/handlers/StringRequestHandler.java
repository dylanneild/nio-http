package com.codeandstrings.niohttp.handlers;

import com.codeandstrings.niohttp.request.Request;

public abstract class StringRequestHandler implements RequestHandler {
	public abstract String getContentType();
	public abstract String handleRequest(Request request);	
}
