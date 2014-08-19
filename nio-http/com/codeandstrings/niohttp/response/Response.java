package com.codeandstrings.niohttp.response;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.codeandstrings.niohttp.data.HeaderValues;
import com.codeandstrings.niohttp.data.IdealBlockSize;
import com.codeandstrings.niohttp.data.Parameters;
import com.codeandstrings.niohttp.enums.HttpProtocol;
import com.codeandstrings.niohttp.enums.RequestMethod;

public class Response {

	private HttpProtocol protocol;
	private RequestMethod method;

	private int code;
	private String description;
	private HeaderValues headers;
	private ByteBuffer body;

	public Response(HttpProtocol protocol, RequestMethod method) {
		this.protocol = protocol;
		this.method = method;
		this.headers = new HeaderValues(true);
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

	public void setBody(ByteBuffer body) {
		this.body = body;
	}

	@Override
	public String toString() {
		return "Response [protocol=" + protocol + ", method=" + method
				+ ", code=" + code + ", description=" + description
				+ ", headers=" + headers + ", body=" + body + "]";
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Response)) return false;

        Response response = (Response) o;

        if (code != response.code) return false;
        if (body != null ? !body.equals(response.body) : response.body != null) return false;
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
        result = 31 * result + (body != null ? body.hashCode() : 0);
        return result;
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

	public ByteBuffer getByteBuffer() {

		ByteBuffer buffer = ByteBuffer.allocate(IdealBlockSize.VALUE);

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

			buffer.put(bytes);

		}

		// should we be sending a body?
		boolean responseBodyNeeded = true;

		if (this.method == RequestMethod.HEAD) {
			responseBodyNeeded = false;
		}

		if (responseBodyNeeded && this.body != null) {
			buffer.put(this.body);
		}

		buffer.flip();

		return buffer;

	}

}
