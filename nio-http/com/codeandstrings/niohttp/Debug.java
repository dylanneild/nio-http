package com.codeandstrings.niohttp;

import com.codeandstrings.niohttp.handlers.StringRequestHandler;

public class Debug {

	public static void main (String args[]) {
	
		Parameters parameters = new Parameters();
		parameters.setPort(8888);
					
		StringRequestHandler handler = new StringRequestHandler() {
			
			@Override
			public String getContentType() {
				return "text/html";
			}

			@Override
			public String handleRequest(Request request) {
				
				return "Thanks for connecting from " + request.getRemoteAddr();
				
				// return "Here is your data:<br><br> Debugging for your request: " + request.toString();
				
				
			}
			
		};	
		
		Server server = new Server();
		server.setParameters(parameters);
		server.setRequestHandler(handler);
		
		server.run();
		
	}
	
}
