package com.codeandstrings.niohttp.request;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import com.codeandstrings.niohttp.enums.HttpProtocol;
import com.codeandstrings.niohttp.enums.RequestMethod;

public class Request {

	private String remoteAddr;
	private int remotePort;
	private RequestHeader header = null;

	public static Request generateRequest(String remoteAddr, int remotePort,
			RequestHeader header) {

		Request r = new Request();

		r.remoteAddr = remoteAddr;
		r.remotePort = remotePort;
		r.header = header;

		return r;
	}

	public int getRemotePort() {
		return remotePort;
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

	public String getHeader(String name) {
		return header.getHeader(name);
	}

	public List<String> getHeaders(String name) {
		return header.getHeaders(name);
	}

	public Iterator<String> getHeaderNames() {
		return header.getHeaderNames();
	}

	@Override
	public String toString() {
		return "Request [remoteAddr=" + remoteAddr + ", remotePort="
				+ remotePort + ", header=" + header + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((header == null) ? 0 : header.hashCode());
		result = prime * result
				+ ((remoteAddr == null) ? 0 : remoteAddr.hashCode());
		result = prime * result + remotePort;
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
		if (remotePort != other.remotePort)
			return false;
		return true;
	}

}
