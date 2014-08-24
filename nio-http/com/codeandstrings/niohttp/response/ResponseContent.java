package com.codeandstrings.niohttp.response;

public class ResponseContent implements ResponseMessage {

    private ResponseContentHeader bufferHeader;
	private byte[] buffer;

    public ResponseContent(long sessionId, long requestId, byte[] buffer, boolean lastBufferForRequest) {
        this.bufferHeader = new ResponseContentHeader(sessionId, requestId, lastBufferForRequest);
        this.buffer = buffer;
    }

    public byte[] getBuffer() {
		return buffer;
	}

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    @Override
    public long getSessionId() {
        return bufferHeader.getSessionId();
    }

    public boolean isLastBufferForRequest() {
        return bufferHeader.isLastBufferForRequest();
    }

    @Override
    public long getRequestId() {
        return bufferHeader.getRequestId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResponseContent)) return false;

        ResponseContent that = (ResponseContent) o;

        if (buffer != null ? !buffer.equals(that.buffer) : that.buffer != null) return false;
        if (bufferHeader != null ? !bufferHeader.equals(that.bufferHeader) : that.bufferHeader != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = bufferHeader != null ? bufferHeader.hashCode() : 0;
        result = 31 * result + (buffer != null ? buffer.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BufferContainer{" +
                "bufferHeader=" + bufferHeader +
                ", buffer=" + buffer +
                '}';
    }
}
