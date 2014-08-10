package com.codeandstrings.niohttp.request;

import java.net.URI;

import com.codeandstrings.niohttp.data.HeaderValues;
import com.codeandstrings.niohttp.enums.HttpProtocol;
import com.codeandstrings.niohttp.enums.RequestMethod;

public class RequestHeader {

	private RequestMethod method;
	private URI uri;
	private HttpProtocol protocol;
	private HeaderValues headers;

	public RequestMethod getMethod() {
		return method;
	}

	public void setHeaders(HeaderValues headers) {
		this.headers = headers;
	}

	public void addHeader(String name, String value) {
		headers.addHeader(name, value);
	}

	public HeaderValues getHeaders() {
		return headers;
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
				+ ", protocol=" + protocol + ", headers=" + headers + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((headers == null) ? 0 : headers.hashCode());
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result
				+ ((protocol == null) ? 0 : protocol.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
		RequestHeader other = (RequestHeader) obj;
		if (headers == null) {
			if (other.headers != null)
				return false;
		} else if (!headers.equals(other.headers))
			return false;
		if (method != other.method)
			return false;
		if (protocol != other.protocol)
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

}
