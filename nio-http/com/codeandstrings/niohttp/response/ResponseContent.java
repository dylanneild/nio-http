package com.codeandstrings.niohttp.response;

import java.util.Arrays;

public class ResponseContent implements ResponseMessage {

    private long sessionId;
    private long requestId;
    private boolean lastBufferForRequest;
	private byte[] buffer;
    private byte[] headerBuffer;
    private byte[] footerBuffer;

    public ResponseContent(long sessionId, long requestId, byte[] buffer, boolean lastBufferForRequest) {
        this.sessionId = sessionId;
        this.requestId = requestId;
        this.lastBufferForRequest = lastBufferForRequest;
        this.buffer = buffer;
    }

    public byte[] getHeaderBuffer() {
        return headerBuffer;
    }

    public void setHeaderBuffer(byte[] headerBuffer) {
        this.headerBuffer = headerBuffer;
    }

    public byte[] getFooterBuffer() {
        return footerBuffer;
    }

    public void setFooterBuffer(byte[] footerBuffer) {
        this.footerBuffer = footerBuffer;
    }

    public byte[] getBuffer() {
		return buffer;
	}

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public boolean isLastBufferForRequest() {
        return lastBufferForRequest;
    }

    public void setLastBufferForRequest(boolean lastBufferForRequest) {
        this.lastBufferForRequest = lastBufferForRequest;
    }

    public int getTotalBufferSize() {
        int h = this.headerBuffer == null ? 0 : this.headerBuffer.length;
        int f = this.footerBuffer == null ? 0 : this.footerBuffer.length;
        int b = this.buffer == null ? 0 : this.buffer.length;
        return h+f+b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResponseContent that = (ResponseContent) o;

        if (lastBufferForRequest != that.lastBufferForRequest) return false;
        if (requestId != that.requestId) return false;
        if (sessionId != that.sessionId) return false;
        if (!Arrays.equals(buffer, that.buffer)) return false;
        if (!Arrays.equals(footerBuffer, that.footerBuffer)) return false;
        if (!Arrays.equals(headerBuffer, that.headerBuffer)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (sessionId ^ (sessionId >>> 32));
        result = 31 * result + (int) (requestId ^ (requestId >>> 32));
        result = 31 * result + (lastBufferForRequest ? 1 : 0);
        result = 31 * result + (buffer != null ? Arrays.hashCode(buffer) : 0);
        result = 31 * result + (headerBuffer != null ? Arrays.hashCode(headerBuffer) : 0);
        result = 31 * result + (footerBuffer != null ? Arrays.hashCode(footerBuffer) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ResponseContent{" +
                "sessionId=" + sessionId +
                ", requestId=" + requestId +
                ", lastBufferForRequest=" + lastBufferForRequest +
                ", buffer=" + Arrays.toString(buffer) +
                ", headerBuffer=" + Arrays.toString(headerBuffer) +
                ", footerBuffer=" + Arrays.toString(footerBuffer) +
                '}';
    }
}
