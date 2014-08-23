nio-http
========

A hybrid design HTTP server library for Java 7.

### How To Use

To build the library, either checout into Eclipse and package your own JAR file (or use the code as-is in your project), or build from the command line using [Apache Ant](http://ant.apache.org):

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

Demo code is [up and running](http://sky.codeandstrings.com) and demonstrates the library available and handling requests in it's current state. This server is a simple 2 CPU [DigitalOcean](http://www.digitalocean.com) droplet and is capable of handling several thousand requests per second using only NIO-HTTP and a few lines of startup code (see: com.codeandstrings.niohttp.debug for example code).  

On 16 CPU compute instances the current version of NIO-HTTP can consume ~1500% CPU power and deliver ~40k requests per second to thousands of connections. Performance continues to increase dramatically as development proceeds.

### Architecture

NIO-HTTP uses a hybrid design architecture to process requests: 

##### Core Service
Server and Client sockets are all registered against a master selector and data is read/written (and connections are accepeted) using an non-blocking event system with buffered reading and writing. The core of NIO-HTTP can handle thousands of simultaneous connections with minimal CPU load. In addition to TCP socket management, the core service handles connectivity with handler services, where HTTP responses are generated. In effect, the core service is a master connection routing binding HTTP clients to handler services.

##### Handler Services
Once the core services reads client requests, handler services generate the responses. Handler services operate in their own threads and can be crafted to meet the needs of the implementing code. Each handler communicates with the core service via a non-blocking channel, allowing handler input and output to integrate with the core service directly.

At present, the following handler services are working/planned: 

* String (return textual response data)
* File System Server (serves files from a folder like a traditional web server)
* FastCGI (send data to a FastCGI server for further processing by PHP, Ruby, etc)
* HTTP Proxy (proxy requests onto another HTTP server)

Any combination of handlers can be used and assigned to URIs. Multiple handlers assigned to the same URI are automatically load balanced in round-robin.

By combining file system server, fast CGI and proxy handlers a full production deployable web service can be quickly created with minimal additional code.

### Requirements

* Java 7
* Apache Ant for command line building
* Eclipse or IDEA (or the IDE of your choice) for development 

### Can I help?

Sure! Please place any pull requests against the devel branch.

### Current Status

Functional for basic text requests via a StringRequestHandler (see example below) - using GET and HEAD requests over HTTP/1.0 and HTTP/1.1. GET/HEAD/POST requests working, as do parameter value and header value passing via GET/POST (GET and POST via x-www-form-urlencoded only; presently form/multipart requests are received but not decoded into values).

Full file server functionality (directory index support still unimplemented) via the FileSystemRequestHandler object. This is a non-blocking I/O file service handler and in it's current (mostly unoptimized) form is capable of pushing close to 5Gbps to thousands of requests from a single SSD.

### Issues

There are lots of enhancements underway. See [the issues page](https://github.com/simplepanda/nio-http/issues) for specifics.
