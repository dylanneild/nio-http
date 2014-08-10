package com.codeandstrings.niohttp.exceptions.http;

public class HttpVersionNotSupported extends HttpException {
	
	private static final long serialVersionUID = 8703568506084371031L;
	private String protocol;

	public HttpVersionNotSupported(String protocol) {
		super(505, "HTTP Version Not Supported");
		this.protocol = protocol;
	}

	@Override
	public String toString() {
		return "HttpVersionNotSupported [protocol=" + protocol + "], " + super.toString();
	}
	
}
