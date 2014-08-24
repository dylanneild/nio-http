package com.codeandstrings.niohttp.request;

import java.io.*;
import java.util.Arrays;

public class RequestBody  {

	private byte[] bytes;

    public byte[] getBytes() {
		return bytes;
	}
	
	public String getBytesAsString(String charset) {
		try {
			return new String(getBytes(), charset);
		} catch (UnsupportedEncodingException e) {			
			e.printStackTrace();
			return null;
		}
	}
	
	public int getSize() {
		if (this.bytes == null) 
			return -1;
		else 
			return bytes.length;
	}
	
	public boolean hasBytes() {
		return this.bytes == null ? false : true;
	}

	public RequestBody(byte[] bytes) {
		super();
		this.bytes = bytes;
	}

    public RequestBody() {}

	@Override
	public String toString() {
		return "RequestBody [bytes=" + Arrays.toString(bytes) + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bytes);
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
		RequestBody other = (RequestBody) obj;
		if (!Arrays.equals(bytes, other.bytes))
			return false;
		return true;
	}

}
