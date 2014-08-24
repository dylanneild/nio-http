package com.codeandstrings.niohttp.response;

import com.codeandstrings.niohttp.data.DateUtils;
import com.codeandstrings.niohttp.data.Parameters;
import com.codeandstrings.niohttp.enums.HttpProtocol;
import com.codeandstrings.niohttp.enums.RequestMethod;
import com.codeandstrings.niohttp.exceptions.http.HttpException;
import com.codeandstrings.niohttp.request.Request;

import java.util.Date;

public class ExceptionResponseFactory {

	private HttpException e;

	public ExceptionResponseFactory(HttpException e) {
		super();
		this.e = e;
	} 

    private Response configure(boolean keepAlive, Response r, Parameters parameters) {

        r.setCode(this.e.getCode());
        r.setDescription(this.e.getDescription());
        r.addHeader("Server", parameters.getServerString());
        r.addHeader("Date", DateUtils.getRfc822DateStringGMT(new Date()));
        r.addHeader("Vary", "Accept-Encoding");
        r.addHeader("Content-Length", "0");

        if (keepAlive) {
            r.addHeader("Connection", "keep-alive");
        } else {
            r.addHeader("Connection", "close");
        }

        r.setBodyIncluded(false);

        return r;
    }

	public Response create(long sessionId, Parameters parameters) {
		Response r = new Response(sessionId, HttpProtocol.HTTP1_1, RequestMethod.GET);
        return this.configure(false, r, parameters);
	}

    public Response create(Request request) {
        Response r = new Response(request);
        return this.configure(request.isKeepAlive(), r, request.getServerParameters());
    }
	
}
