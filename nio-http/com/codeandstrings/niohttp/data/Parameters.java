package com.codeandstrings.niohttp.data;

public class Parameters {

	private int port;
	private String serverString;

	public static Parameters getDefaultParameters() {

		Parameters r = new Parameters();

		r.port = 8888;
		r.serverString = "Java-NIO";

		return r;

	}

	public String getServerString() {
		return serverString;
	}

	public void setServerString(String serverString) {
		this.serverString = serverString;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String toString() {
		return "Parameters [port=" + port + ", serverString=" + serverString
				+ "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + port;
		result = prime * result
				+ ((serverString == null) ? 0 : serverString.hashCode());
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
		Parameters other = (Parameters) obj;
		if (port != other.port)
			return false;
		if (serverString == null) {
			if (other.serverString != null)
				return false;
		} else if (!serverString.equals(other.serverString))
			return false;
		return true;
	}

}
