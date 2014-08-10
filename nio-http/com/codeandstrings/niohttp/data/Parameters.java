package com.codeandstrings.niohttp.data;

public class Parameters {

	private int port;
	private String serverString;
	private String serverIp;

	private void configureDefaultParameters() {
		this.port = 8888;
		this.serverString = "Java-NIO";
		this.serverIp = null;
	}

	public static Parameters getDefaultParameters() {
		Parameters r = new Parameters();
		r.configureDefaultParameters();
		return r;
	}

	public Parameters() {
		this.configureDefaultParameters();
	}

	public Parameters(int port) {
		this.port = port;
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

	public String getServerIp() {
		return serverIp;
	}

	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}

	@Override
	public String toString() {
		return "Parameters [port=" + port + ", serverString=" + serverString
				+ ", serverIp=" + serverIp + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + port;
		result = prime * result
				+ ((serverIp == null) ? 0 : serverIp.hashCode());
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
		if (serverIp == null) {
			if (other.serverIp != null)
				return false;
		} else if (!serverIp.equals(other.serverIp))
			return false;
		if (serverString == null) {
			if (other.serverString != null)
				return false;
		} else if (!serverString.equals(other.serverString))
			return false;
		return true;
	}

}
