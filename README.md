nio-http
========

An non-blocking I/O HTTP server library for Java 7.

### Requirements

* Java 7
* Eclipse for build environment

### Can I help?

Sure! Please place any pull requests against the devel branch.

### Current Status

Generally non-functional; clients can connect and submit an initial request. Internal processing has begun but is presently not completed enough to deliver full responses to clients.

Basic error responses are currently generated - bad requests, unsupported methods, etc all trigger propper HTTP/1.1 responses and are returned to the client.

