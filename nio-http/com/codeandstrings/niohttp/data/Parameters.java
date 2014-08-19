package com.codeandstrings.niohttp.data;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Parameters implements Externalizable {

    private static final long serialVersionUID = -3428150652513350665L;

    private int port;
	private String serverString;
	private String serverIp;
	private int maximumPostSize;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(this.port);
        out.writeObject(this.serverString == null ? "" : this.serverString);
        out.writeObject(this.serverIp == null ? "" : this.serverIp);
        out.writeInt(this.maximumPostSize);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.port = in.readInt();
        this.serverString = (String)in.readObject();
        this.serverIp = (String)in.readObject();
        this.maximumPostSize = in.readInt();
    }

    private void configureDefaultParameters() {
		this.port = 8888;
		this.serverString = "NIO-HTTP";
		this.serverIp = null;
		this.maximumPostSize = (8 * 1024 * 1024);
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
        this.configureDefaultParameters();
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

	public int getMaximumPostSize() {
		return maximumPostSize;
	}

	public void setMaximumPostSize(int maximumPostSize) {
		this.maximumPostSize = maximumPostSize;
	}

	@Override
	public String toString() {
		return "Parameters [port=" + port + ", serverString=" + serverString
				+ ", serverIp=" + serverIp + ", maximumPostSize="
				+ maximumPostSize + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + maximumPostSize;
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
		if (maximumPostSize != other.maximumPostSize)
			return false;
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
