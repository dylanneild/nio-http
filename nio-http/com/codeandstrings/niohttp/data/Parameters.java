package com.codeandstrings.niohttp.data;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Parameters implements Externalizable {

    private static final long serialVersionUID = 9070633037021307477L;

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
		this.serverString = "NIO-HTTP v0.1";
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Parameters that = (Parameters) o;

        if (maximumPostSize != that.maximumPostSize) return false;
        if (port != that.port) return false;
        if (serverIp != null ? !serverIp.equals(that.serverIp) : that.serverIp != null) return false;
        if (serverString != null ? !serverString.equals(that.serverString) : that.serverString != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = port;
        result = 31 * result + (serverString != null ? serverString.hashCode() : 0);
        result = 31 * result + (serverIp != null ? serverIp.hashCode() : 0);
        result = 31 * result + maximumPostSize;
        return result;
    }

    @Override
    public String toString() {
        return "Parameters{" +
                "port=" + port +
                ", serverString='" + serverString + '\'' +
                ", serverIp='" + serverIp + '\'' +
                ", maximumPostSize=" + maximumPostSize +
                '}';
    }
}
