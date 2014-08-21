package com.codeandstrings.niohttp.response;

import java.io.*;
import java.nio.ByteBuffer;

import com.codeandstrings.niohttp.data.HeaderValues;
import com.codeandstrings.niohttp.data.IdealBlockSize;
import com.codeandstrings.niohttp.enums.HttpProtocol;
import com.codeandstrings.niohttp.enums.RequestMethod;
import com.codeandstrings.niohttp.request.Request;

public class Response implements Externalizable, ResponseMessage {

    private long sessionId;
    private long requestId;

	private HttpProtocol protocol;
	private RequestMethod method;

	private int code;
	private String description;
	private HeaderValues headers;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(this.sessionId);
        out.writeLong(this.requestId);
        out.writeObject(this.protocol);
        out.writeObject(this.method);
        out.writeInt(this.code);
        out.writeObject(this.description);
        out.writeObject(this.headers);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.sessionId = in.readLong();
        this.requestId = in.readLong();
        this.protocol = (HttpProtocol)in.readObject();
        this.method = (RequestMethod)in.readObject();
        this.code = in.readInt();
        this.description = (String)in.readObject();
        this.headers = (HeaderValues)in.readObject();
    }

    public Response() {}

    private void configureFromConstructor(long sessionId, HttpProtocol protocol, RequestMethod method) {
        this.sessionId = sessionId;
        this.protocol = protocol;
        this.method = method;
        this.headers = new HeaderValues(true);
    }

    public Response(long sessionId, HttpProtocol protocol, RequestMethod method) {
        this.configureFromConstructor(sessionId, protocol, method);
    }

	public Response(Request request, HttpProtocol protocol, RequestMethod method) {
        this.configureFromConstructor(request.getSessionId(), protocol, method);
        this.requestId = request.getRequestId();
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Response response = (Response) o;

        if (code != response.code) return false;
        if (description != null ? !description.equals(response.description) : response.description != null)
            return false;
        if (headers != null ? !headers.equals(response.headers) : response.headers != null) return false;
        if (method != response.method) return false;
        if (protocol != response.protocol) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = protocol != null ? protocol.hashCode() : 0;
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + code;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (headers != null ? headers.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Response{" +
                "protocol=" + protocol +
                ", method=" + method +
                ", code=" + code +
                ", description='" + description + '\'' +
                ", headers=" + headers +
                '}';
    }

    public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public byte[] getByteBuffer() {

		if (this.protocol != HttpProtocol.HTTP0_9) {

			StringBuilder r = new StringBuilder();

			r.append("HTTP/1.1 ");
			r.append(this.code);
			r.append(" ");
			r.append(this.description);
			r.append("\r\n");
			r.append(this.headers.generateResponse());
			r.append("\r\n");

			String s = r.toString();
			byte bytes[] = null;

			try {
				bytes = s.getBytes("ISO-8859-1");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			}

            return bytes;

		}
        else {
            return null;
        }

	}

}
