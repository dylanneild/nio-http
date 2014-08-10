package com.codeandstrings.niohttp;

public class Debug {

	public static void main (String args[]) {
	
		Parameters parameters = new Parameters();
		parameters.setPort(8888);
			
		Server server = new Server();
		server.setParameters(parameters);
		
		server.run();
		
	}
	
}
