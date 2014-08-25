package com.codeandstrings.niohttp.response;

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;

import com.codeandstrings.niohttp.data.HeaderValues;
import com.codeandstrings.niohttp.enums.HttpProtocol;
import com.codeandstrings.niohttp.enums.RequestMethod;
import com.codeandstrings.niohttp.request.Request;

public class Response implements ResponseMessage {

    private WeakReference<Request> request;
    private long sessionId;

    private boolean bodyIncluded;

	private HttpProtocol protocol;
	private RequestMethod method;

	private int code;
	private String description;
	private HeaderValues headers;

    private void configureFromConstructor(long sessionId, HttpProtocol protocol, RequestMethod method) {
        this.request = null;
        this.sessionId = sessionId;
        this.protocol = protocol;
        this.method = method;
        this.headers = new HeaderValues(true);
        this.bodyIncluded = true;
    }

    public Response(long sessionId, HttpProtocol protocol, RequestMethod method) {
        this.configureFromConstructor(sessionId, protocol, method);
    }

	public Response(Request request) {
        this.configureFromConstructor(request.getSession().getSessionId(), request.getRequestProtocol(), request.getRequestMethod());
        this.request = new WeakReference<>(request);
	}

    @Override
    public Request getRequest() {
        return request == null ? null : request.get();
    }

    public boolean isBodyIncluded() {
        return bodyIncluded;
    }

    public void setBodyIncluded(boolean bodyIncluded) {
        this.bodyIncluded = bodyIncluded;
    }

    public void removeHeader(String name) {
        headers.removeHeader(name);
    }

    public boolean isChunkedTransfer() {
        String te = headers.getSingleValueCaseInsensitive("transfer-encoding");

        if (te != null && te.equalsIgnoreCase("chunked")) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isConnectionClosed() {
        String te = headers.getSingleValueCaseInsensitive("connection");

        if (te != null && te.equalsIgnoreCase("close")) {
            return true;
        } else {
            return false;
        }
    }

    public String getHeaderCaseInsensitive(String header) {
        return this.headers.getSingleValueCaseInsensitive(header);
    }

	public void addHeader(String name, String value) {

        if (name != null && name.equalsIgnoreCase("transfer-encoding") && value != null && value.equalsIgnoreCase("chunked")) {
            // remove content-length if this reponse is transfer-encoding: chunked;
            // these are mutually exclusive
            headers.removeHeader("content-length");
        } else if (name != null && name.equalsIgnoreCase("content-length")) {
            // likewise, if we'ere getting a content-length past make sure no transfer-encoding: chunked exists
            if (this.isChunkedTransfer()) {
                headers.removeHeader("transfer-encoding");
            }
        }

		headers.addHeader(name, value);
	}

    @Override
    public String toString() {
        return "Response{" +
                "request=" + request +
                ", sessionId=" + sessionId +
                ", bodyIncluded=" + bodyIncluded +
                ", protocol=" + protocol +
                ", method=" + method +
                ", code=" + code +
                ", description='" + description + '\'' +
                ", headers=" + headers +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Response response = (Response) o;

        if (bodyIncluded != response.bodyIncluded) return false;
        if (code != response.code) return false;
        if (sessionId != response.sessionId) return false;
        if (description != null ? !description.equals(response.description) : response.description != null)
            return false;
        if (headers != null ? !headers.equals(response.headers) : response.headers != null) return false;
        if (method != response.method) return false;
        if (protocol != response.protocol) return false;
        if (request != null ? !request.equals(response.request) : response.request != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = request != null ? request.hashCode() : 0;
        result = 31 * result + (int) (sessionId ^ (sessionId >>> 32));
        result = 31 * result + (bodyIncluded ? 1 : 0);
        result = 31 * result + (protocol != null ? protocol.hashCode() : 0);
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + code;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (headers != null ? headers.hashCode() : 0);
        return result;
    }

    public void setCode(int code) {
		this.code = code;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public byte[] getByteRepresentation() {
        if (this.protocol != HttpProtocol.HTTP0_9) {
            StringBuilder r = new StringBuilder();

            if (this.protocol == HttpProtocol.HTTP1_0) {
                r.append("HTTP/1.0 ");
            } else {
                r.append("HTTP/1.1 ");
            }

            r.append(this.code);
            r.append(" ");
            r.append(this.description);
            r.append("\r\n");
            r.append(this.headers.generateResponse());
            r.append("\r\n");

            String s = r.toString();
            return s.getBytes(Charset.forName("ISO-8859-1"));
        } else {
            return null;
        }
	}

}
