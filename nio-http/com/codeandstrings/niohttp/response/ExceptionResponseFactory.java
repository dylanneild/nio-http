package com.codeandstrings.niohttp.response;

import com.codeandstrings.niohttp.exceptions.http.HttpException;

public class ExceptionResponseFactory {

	private HttpException e;

	public ExceptionResponseFactory(HttpException e) {
		super();
		this.e = e;
	} 
	
	public Response create() {
		
		Response r = new Response();
		
		r.setCode(this.e.getCode());
		r.setDescription(this.e.getDescription());
		
		r.addHeader("Content-Length", "0");
		r.addHeader("Connecton", "close");
		
		return r;
		
		
	}
	
}
