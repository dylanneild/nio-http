package com.codeandstrings.niohttp;

import com.codeandstrings.niohttp.exceptions.http.*;
import com.codeandstrings.niohttp.handlers.StringRequestHandler;
import com.codeandstrings.niohttp.request.Request;

public class Debug {

	public static void main (String args[]) {
	
		StringRequestHandler handler = new StringRequestHandler() {
			
			@Override
			public String getContentType() {
				return "text/html";
			}

			@Override
			public String handleRequest(Request request) throws HttpException {			
				return "Thanks for connecting from " + request.getRemoteAddr() + ":" + request.getRemotePort();				
			}
			
		};	
		
		Server server = new Server();
		server.setRequestHandler(handler);
		
		server.run();
		
	}
	
}
