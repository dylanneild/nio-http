package com.codeandstrings.niohttp.response;

import com.codeandstrings.niohttp.data.Parameters;
import com.codeandstrings.niohttp.enums.HttpProtocol;
import com.codeandstrings.niohttp.enums.RequestMethod;
import com.codeandstrings.niohttp.exceptions.http.HttpException;
import com.codeandstrings.niohttp.request.Request;

public class ExceptionResponseFactory {

	private HttpException e;

	public ExceptionResponseFactory(HttpException e) {
		super();
		this.e = e;
	} 
	
	public Response create(long sessionId, Parameters parameters) {
		
		Response r = new Response(sessionId, HttpProtocol.HTTP1_1, RequestMethod.GET);

		r.setCode(this.e.getCode());
		r.setDescription(this.e.getDescription());
        r.addHeader("Server", parameters.getServerString());
        r.addHeader("Vary", "Accept-Encoding");
		r.addHeader("Content-Length", "0");
		r.addHeader("Connecton", "close");

		return r;

	}
	
}
