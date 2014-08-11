package com.codeandstrings.niohttp.request;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class RequestBodyFactory {

	private byte[] bodyData;
	private int bytesReceived;

	public void resize(int size) {
		this.bodyData = new byte[size];
		this.bytesReceived = 0;
	}

	public void addBytes(byte bytes[]) {

		for (int i = 0; i < bytes.length; i++) {
			if (bytesReceived == bodyData.length)
				return;

			bodyData[bytesReceived] = bytes[i];
			bytesReceived++;
		}

	}
	
	public void addBytes(byte bytes[], int start, int length) {
		
		for (int i = start; i < start + length; i++) {
			if (bytesReceived == bodyData.length)
				return;

			bodyData[bytesReceived] = bytes[i];
			bytesReceived++;
		}
		
	}

	public boolean isFull() {
		
		if (bytesReceived == bodyData.length) {
			return true;
		} else {
			return false;
		}
	}

	public RequestBody build() {
		return new RequestBody(this.bodyData);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bodyData);
		result = prime * result + bytesReceived;
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
		RequestBodyFactory other = (RequestBodyFactory) obj;
		if (!Arrays.equals(bodyData, other.bodyData))
			return false;
		if (bytesReceived != other.bytesReceived)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RequestBodyFactory [bodyData=" + Arrays.toString(bodyData)
				+ ", bytesReceived=" + bytesReceived + "]";
	}

}
