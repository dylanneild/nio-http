package com.codeandstrings.niohttp.response;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.codeandstrings.niohttp.data.HeaderValues;
import com.codeandstrings.niohttp.data.IdealBlockSize;
import com.codeandstrings.niohttp.enums.HttpProtocol;

public class Response {

	private HttpProtocol protocol;
	private int code;
	private String description;
	private HeaderValues headers;
	private ByteBuffer body;

	public Response(HttpProtocol protocol) {
		this.protocol = protocol;
		this.headers = new HeaderValues();
		this.headers.addHeader("Server", "Java-NIO");
	}

	public void addHeader(String name, String value) {
		headers.addHeader(name, value);
	}

	public void setBody(ByteBuffer body) {
		this.body = body;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + code;
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((headers == null) ? 0 : headers.hashCode());
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
		Response other = (Response) obj;
		if (code != other.code)
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (headers == null) {
			if (other.headers != null)
				return false;
		} else if (!headers.equals(other.headers))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Response [code=" + code + ", description=" + description
				+ ", headers=" + headers + "]";
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

		// this needs overhauling - right now we're just reading a simple
		// header but as data gets larger and includes streams
		// we'll need to make this more complex - indicate to the channel
		// that we're here and working, etc.

		ByteBuffer buffer = ByteBuffer.allocate(IdealBlockSize.VALUE);

		if (this.protocol == HttpProtocol.HTTP1_1) {
			
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
				bytes = s.getBytes("US-ASCII");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}

			buffer.put(bytes);
			
		}

		if (this.body != null) {		
			buffer.put(this.body);
		}
		
		buffer.flip();

		return buffer;		

	}

}
