package com.codeandstrings.niohttp.response;

import com.codeandstrings.niohttp.data.Parameters;
import com.codeandstrings.niohttp.enums.HttpProtocol;
import com.codeandstrings.niohttp.enums.RequestMethod;
import com.codeandstrings.niohttp.exceptions.http.HttpException;

public class ExceptionResponseFactory {

	private HttpException e;

	public ExceptionResponseFactory(HttpException e) {
		super();
		this.e = e;
	} 
	
	public Response create(Parameters parameters) {
		
		Response r = new Response(HttpProtocol.HTTP1_1, RequestMethod.GET, parameters);
		
		r.setCode(this.e.getCode());
		r.setDescription(this.e.getDescription());
		
		r.addHeader("Content-Length", "0");
		r.addHeader("Connecton", "close");

        System.out.println("Sending Exception: " + r.toString() + " from " + this.e.toString());
		
		return r;

	}
	
}
