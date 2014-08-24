package com.codeandstrings.niohttp.request;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.activation.MimeType;

import com.codeandstrings.niohttp.data.NameValuePair;
import com.codeandstrings.niohttp.data.Parameters;
import com.codeandstrings.niohttp.enums.HttpProtocol;
import com.codeandstrings.niohttp.enums.RequestMethod;

public class Request {

    private long sessionId;
    private long requestId;
    private long timestamp;
	private String remoteAddr;
	private int remotePort;
	private RequestHeader header = null;
	private RequestBody body = null;
    private Parameters serverParameters;

    public Request() {}

    public static Request generateRequest(long sessionId, long requestId, String remoteAddr,
                                          int remotePort, RequestHeader header,
                                          RequestBody body, Parameters parameters) {

		Request r = new Request();

        r.sessionId = sessionId;
        r.requestId = requestId;
        r.timestamp = System.currentTimeMillis();
		r.remoteAddr = remoteAddr;
		r.remotePort = remotePort;
		r.header = header;
		r.body = body;
        r.serverParameters = parameters;

		return r;
	}

    public boolean isKeepAlive() {

        String connection = this.header.getHeaderCaseInsensitive("connection");

        if (connection == null) {
            if (this.header.getProtocol() == HttpProtocol.HTTP1_1) {
                return true;
            } else {
                return false;
            }
        }
        else if (connection.equalsIgnoreCase("keep-alive"))
            return true;
        else if (connection.equalsIgnoreCase("close"))
            return false;
        else
            return false;

    }
		
	private List<NameValuePair> getFormEncodedNameValuePairs(String from) {
		
		ArrayList<NameValuePair> r = new ArrayList<NameValuePair>();
		
		String[] tokens = from.split("&");
		
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
	
	@SuppressWarnings("unchecked")
    private List<NameValuePair> getGetParameterNameValuePairs() {
		
		URI uri = this.getRequestURI();
		String query = uri.getQuery();
		
		if (query == null)
			return new ArrayList();
		else 
			return this.getFormEncodedNameValuePairs(query);
		
	}
	
	private List<NameValuePair> getPostParameterNameValuePairs() {
		
		String contentType = this.header.getContentType();
		ArrayList<NameValuePair> r = new ArrayList<NameValuePair>();
		
		if (contentType == null) {
			return r;
		}

		MimeType mimeType = null;

		try {
			mimeType = new MimeType(contentType);
		} catch (Exception e) {
			e.printStackTrace();
			return r;
		}
		
		String primary = mimeType.getBaseType();		
		
		if (primary == null)
			return r;
		if (!primary.equalsIgnoreCase("application/x-www-form-urlencoded"))
			return r;
						
		String extractedCharacterSet = "ISO-8859-1";
		
		for (Enumeration e = mimeType.getParameters().getNames(); e.hasMoreElements(); ) {
			String next = (String)e.nextElement();
			
			if (next.equalsIgnoreCase("charset")) {
				extractedCharacterSet = mimeType.getParameter(next);
				break;
			}
		}
		
		String query = this.body.getBytesAsString(extractedCharacterSet);
		
		if (query == null) {
			return r;
		} else {
			return this.getFormEncodedNameValuePairs(query);
		}
		
	}
	
	public List<String> getParameters(String name) {
		
		List<NameValuePair> list = getGetParameterNameValuePairs();
		list.addAll(getPostParameterNameValuePairs());

		ArrayList<String> r = new ArrayList<String>();

		for (NameValuePair nvp : list) {
			if (nvp.getName().equals(name)) {
				r.add(nvp.getValue());
			}
		}

		return r;

	}
	
	public Set<String> getParameterNames() {
		
		List<NameValuePair> list = getGetParameterNameValuePairs();
		list.addAll(getPostParameterNameValuePairs());
		
		HashSet<String> h = new HashSet<String>();
		
		for (NameValuePair nvp : list) {
			h.add(nvp.getName());
		}
				
		return h;
	}
	
	public String getParameter(String name) {
		List<String> r = getParameters(name);
		if (r.size() == 1)
			return r.get(0);
		else
			return null;
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

    public String getHeaderCaseInsensitive(String name) {
        return header.getHeaderCaseInsensitive(name);
    }

    public List<String> getHeaders(String name) {
		return header.getHeaders(name);
	}

	public List<String> getHeaderNames() {
		return header.getHeaderNames();
	}
	
	public String getContentType() {
		return getHeaderCaseInsensitive("Content-Type");
	}

    public long getRequestId() {
        return this.requestId;
    }

    public long getSessionId() {
        return sessionId;
    }

    public Parameters getServerParameters() {
        return serverParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Request request = (Request) o;

        if (remotePort != request.remotePort) return false;
        if (requestId != request.requestId) return false;
        if (sessionId != request.sessionId) return false;
        if (timestamp != request.timestamp) return false;
        if (body != null ? !body.equals(request.body) : request.body != null) return false;
        if (header != null ? !header.equals(request.header) : request.header != null) return false;
        if (remoteAddr != null ? !remoteAddr.equals(request.remoteAddr) : request.remoteAddr != null) return false;
        if (serverParameters != null ? !serverParameters.equals(request.serverParameters) : request.serverParameters != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (sessionId ^ (sessionId >>> 32));
        result = 31 * result + (int) (requestId ^ (requestId >>> 32));
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (remoteAddr != null ? remoteAddr.hashCode() : 0);
        result = 31 * result + remotePort;
        result = 31 * result + (header != null ? header.hashCode() : 0);
        result = 31 * result + (body != null ? body.hashCode() : 0);
        result = 31 * result + (serverParameters != null ? serverParameters.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Request{" +
                "sessionId=" + sessionId +
                ", requestId=" + requestId +
                ", timestamp=" + timestamp +
                ", remoteAddr='" + remoteAddr + '\'' +
                ", remotePort=" + remotePort +
                ", header=" + header +
                ", body=" + body +
                ", serverParameters=" + serverParameters +
                '}';
    }
}
