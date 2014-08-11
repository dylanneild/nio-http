package com.codeandstrings.niohttp.request;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.codeandstrings.niohttp.data.NameValuePair;
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
	
	private List<NameValuePair> getGetParameterNameValuePairs() {
		
		URI uri = this.getRequestURI();
		String query = uri.getQuery();
		ArrayList<NameValuePair> r = new ArrayList<NameValuePair>();
		
		if (query == null) {
			return r;
		}
		
		String[] tokens = query.split("&");
		
		for (int i = 0; i < tokens.length; i++) {
			
			String single = tokens[i];
			String[] extracted = single.split("=");
			
			String name = extracted[0];
						
			if (extracted.length == 2) {
				try {
					String value = URLDecoder.decode(extracted[1], "UTF-8");
					r.add(new NameValuePair(name, value));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			} else {
				r.add(new NameValuePair(name, ""));
			}
			
		}
				
		return r;
		
	}
	
	private List<String> getGetParameters(String name) {
		
		List<NameValuePair> list = getGetParameterNameValuePairs();
		ArrayList<String> r = new ArrayList<String>();
		
		for (NameValuePair nvp : list) {			
			if (nvp.getName().equals(name)) {
				r.add(nvp.getValue());
			}			
		}
		
		return r;
		
	}
	
	private HashSet<String> getGetParameterNames() {
		
		List<NameValuePair> list = getGetParameterNameValuePairs();
		HashSet<String> h = new HashSet<String>();
		
		for (NameValuePair nvp : list) {
			h.add(nvp.getName());
		}
				
		return h;
	}
	
	public List<String> getParameters(String name) {		
		ArrayList<String> r = new ArrayList<String>();
		r.addAll(this.getGetParameters(name));
		return r;		
	}
	
	public String getParameter(String name) {
		List<String> r = getParameters(name);
		if (r.size() == 1)
			return r.get(0);
		else
			return null;
	}
	
	public Set<String> getParameterNames() {		
		HashSet<String> r = new HashSet<String>();
		r.addAll(getGetParameterNames());
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

	public Set<String> getHeaderNames() {
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
