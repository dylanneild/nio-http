package com.codeandstrings.niohttp.response;

import com.codeandstrings.niohttp.exceptions.http.HttpException;

public class ResponseFactory {
	
	public static Response createResponse(String content, String contentType) {
		return null;
	}
	
	public static Response createResponse(HttpException e) {
		return (new ExceptionResponseFactory(e)).create();
	}
	
}
