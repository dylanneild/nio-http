nio-http
========

A non-blocking I/O HTTP server library for Java 7.

### Requirements

* Java 7
* Eclipse for build environment

### Can I help?

Sure! Please place any pull requests against the devel branch.

### Current Status

Functional for basic text requests via a StringRequestHandler (see example below) - using GET and HEAD requests over HTTP/1.0 and HTTP/1.1.

File stream responses are not yet implemented and non-GET/HEAD responses and are not tested (POST) or not implemented (PUT, DELETE, etc).

### Roadmap

* Upgrade session system to support request bodies.
* Upgrade request object to include POST or GET parameter block.
* Add and test a file response object (return a file object as a response)
* Update Server object to support multiple handlers, mounted by URI, including wildcards.
* Update Server object to support default URI's.
* Update exceptions to support bodies

### Example Server

http://sky.codeandstrings.com

This server is running demo code that shows the server up and running.

### How To Use

The server library is designed to be very easy to integrate with your project. At the simplest level, simply create an instance of a RequestHandler object (in this case, a StringRequestHandler to return simple String answers) and pass it to the server as a request handler.

	StringRequestHandler handler = new StringRequestHandler() {
	
		@Override
		public String getContentType() {
			return "text/html";
		}

		@Override
		public String handleRequest(Request request) {				
			return "Thanks for connecting from " + request.getRemoteAddr();				
		}
			
	};	
		
	Server server = new Server();
	server.setRequestHandler(handler);
		
	server.run();
