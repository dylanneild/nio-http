package com.codeandstrings.niohttp.data;

public class Parameters {

	private int port;

	public static Parameters getDefaultParameters() {
		
		Parameters r = new Parameters();
		r.setPort(8888);
		
		return r;
		
	}
	
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String toString() {
		return "Parameters [port=" + port + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + port;
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
		return true;
	}

}
