package com.codeandstrings.niohttp.data;

public class Parameters {

    private int port;
	private String serverString;
	private String serverIp;
	private int maximumPostSize;
    private int connectionBacklog;
    private boolean tcpNoDelay;

    public Parameters copy() {
        Parameters r = new Parameters();
        r.port = this.port;
        r.serverString = String.valueOf(this.serverString);
        r.serverIp = String.valueOf(this.serverIp);
        r.maximumPostSize = this.maximumPostSize;
        r.connectionBacklog = this.connectionBacklog;
        r.tcpNoDelay = this.tcpNoDelay;
        return r;
    }

    private void configureDefaultParameters() {
		this.port = 8888;
		this.serverString = "NIO-HTTP v0.1";
		this.serverIp = null;
		this.maximumPostSize = (8 * 1024 * 1024);
        this.connectionBacklog = 16 * 1024;
        this.tcpNoDelay = true;
	}

	public static Parameters getDefaultParameters() {
		Parameters r = new Parameters();
		r.configureDefaultParameters();
		return r;
	}

	public Parameters() {}

    public Parameters(int port) {
        this.configureDefaultParameters();
        this.port = port;
	}

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public int getConnectionBacklog() {
        return connectionBacklog;
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
