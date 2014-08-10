package com.codeandstrings.niohttp;

import java.net.URI;

import com.codeandstrings.niohttp.enums.HttpProtocol;
import com.codeandstrings.niohttp.enums.RequestMethod;

public class RequestHeader {

	private RequestMethod method;
	private URI uri;
	private HttpProtocol protocol;

	public RequestMethod getMethod() {
		return method;
	}

	public void setMethod(RequestMethod method) {
		this.method = method;
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public HttpProtocol getProtocol() {
		return protocol;
	}

	public void setProtocol(HttpProtocol protocol) {
		this.protocol = protocol;
	}

	@Override
	public String toString() {
		return "RequestHeader [method=" + method + ", uri=" + uri
				+ ", protocol=" + protocol + "]";
	}

}
