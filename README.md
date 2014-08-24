nio-http
========

A hybrid design HTTP server library for Java 7.

### How To Use

To build the library, either checkout into Eclipse / IDEA and package your own JAR file (or use the code as-is in your project), or build from the command line using [Apache Ant](http://ant.apache.org):

	# ant package
	# file nio-http.jar 
	nio-http.jar: Java archive data (JAR)

The server library is designed to be very easy to integrate with your project. At the simplest level, simply create an instance of a RequestHandler class (in this case, a StringRequestHandler to return simple String answers):

	public class MyHandler extends StringRequestHandler {
	
		@Override
		public String getContentType() {
			return "text/html";
		}

		@Override
		public String handleRequest(Request request) {				
			return "Thanks for connecting from " + request.getRemoteAddr() + "!";			
		}
			
	};	
	
Then, pass the Class to the server library:	
	
	public class MyServer {
		
	    public static void main (String args[]) {		
            try {
                Server server = new Server();
                server.addRequestHandler("/", MyHandler.class);                
                server.run();
            }
            catch (Exception e) {
                e.printStackTrace();
             }
        }
        
    }

### Example Server

Demo code is [up and running](http://sky.codeandstrings.com) and demonstrates the library available and handling requests in it's current state.  

### Performance

For basic file serving performance, serving the same file (the Apache index.html file included in Ubuntu), NIO-HTTP compares favourably with major HTTP server implementations.

On Ubuntu 14.04, out of the box, serving 1M requests to 10,000 connections:

* Apache (event): 9,275 requests per second.
* Nginx: 11,733 requests per second.
* NIO-HTTP: 18,150 requests per second.

Nginx can, of course, scale to very high levels when fine tuned but these numbers should show that NIO-HTTP performance is already very good.

### Architecture

NIO-HTTP uses a hybrid design architecture to process requests:

##### Core Service
Client sockets are all registered against a master selector service. The master service load balances the connections onto a connection engine. Each connection engine runs in it's own thread.

##### Engine Service
The engine service is responsible for managing client connections. An engine service is responsible for reading client requests and relaying these requests to connection handlers, where responses are generated. 

##### Handler Services
Once the engine service reads client requests, handler services generate the responses. Handler services operate in their own threads and can be crafted to meet the needs of the implementing code. Each handler communicates with the engine service via a non-blocking channel, allowing handler input and output to integrate with the engine service directly.

At present, the following handler services are working/planned: 

* String (return textual response data)
* File System Server (serves files from a folder like a traditional web server)
* FastCGI (send data to a FastCGI server for further processing by PHP, Ruby, etc)
* HTTP Proxy (proxy requests onto another HTTP server)

Any combination of handlers can be used and assigned to URIs. Multiple handlers assigned to the same URI are automatically load balanced in round-robin.

By combining file system server, fast CGI and proxy handlers a full production deployable web service can be quickly created with minimal additional code.

##### Hybrid Channel / Queue
Core, Engine and Handler services interact using a hybrid channel / queue system. Channels are used between the service threads to notify of connection and/or data arrival. This way threads can receive notifications using standard non-blocking I/O calls, bundling notification messages with normal server load. Once a notification message is receive, a thread-safe queue is used to transfer actual data between threads. This avoids the traditional select(delay) behaviour of similar systems, allowing for pure event based / non-polling operation.

The end result is a server that can scale on multiple processors / cores in an almost linear fashion (tested up to 16 CPU cores).

### Requirements

* Java 7+
* Apache Ant for command line building
* Eclipse or IDEA (or the IDE of your choice) for development 

### Can I help?

Sure! Please place any pull requests against the devel branch.

### Current Status

* Functional for basic text requests via a StringRequestHandler (see example below) - using GET and HEAD requests over HTTP/1.0 and HTTP/1.1. GET/HEAD/POST requests working, as do parameter value and header value passing via GET/POST (GET and POST via x-www-form-urlencoded only; presently form/multipart requests are received but not decoded into values).

* Full file server functionality (directory index support still unimplemented) via the FileSystemRequestHandler object. This is a non-blocking I/O file service handler and in it's current (mostly unoptimized) form is capable of pushing ~10Gbps to thousands of requests from a single SSD (backed by OS level file-system caching).

* Currently in the process of implementing HTTP Proxy load balancing and FastCGI client support.

### Issues

There are lots of enhancements underway. See [the issues page](https://github.com/simplepanda/nio-http/issues) for specifics.
