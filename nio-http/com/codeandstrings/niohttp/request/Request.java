package com.codeandstrings.niohttp.request;

import java.net.URI;

import com.codeandstrings.niohttp.enums.HttpProtocol;
import com.codeandstrings.niohttp.enums.RequestMethod;

public class Request {

	private String remoteAddr;
	private RequestHeader header = null;

	public static Request generateRequest(String remoteAddr,
			RequestHeader header) {
		Request r = new Request();
		r.remoteAddr = remoteAddr;
		r.header = header;
		return r;
	}

	public String getRemoteAddr() {
		return remoteAddr;
	}

	public URI getRequestURI() {
		return header.getUri();
	}

	public RequestMethod getRequestMethod() {
		return header.getMethod();
	}

	public HttpProtocol getRequestProtocol() {
		return header.getProtocol();
	}

	@Override
	public String toString() {
		return "Request [remoteAddr=" + remoteAddr + ", header=" + header + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((header == null) ? 0 : header.hashCode());
		result = prime * result
				+ ((remoteAddr == null) ? 0 : remoteAddr.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Request other = (Request) obj;
		if (header == null) {
			if (other.header != null)
				return false;
		} else if (!header.equals(other.header))
			return false;
		if (remoteAddr == null) {
			if (other.remoteAddr != null)
				return false;
		} else if (!remoteAddr.equals(other.remoteAddr))
			return false;
		return true;
	}

}
