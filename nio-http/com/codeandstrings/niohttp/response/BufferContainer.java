package com.codeandstrings.niohttp.response;

import java.io.IOException;
import java.nio.ByteBuffer;

public class BufferContainer {

    private BufferContainerHeader bufferHeader;
	private ByteBuffer buffer;

    public BufferContainer(long sessionId, long requestId, ByteBuffer buffer, boolean closeOnTransmission, boolean lastBufferForRequest) {
        this.bufferHeader = new BufferContainerHeader(sessionId, requestId, closeOnTransmission, lastBufferForRequest);
        this.buffer = buffer;
    }

    public BufferContainer(BufferContainerHeader bufferHeader, ByteBuffer buffer) {
        this.bufferHeader = bufferHeader;
        this.buffer = buffer;
    }

    public ByteBuffer getBuffer() {
		return buffer;
	}

    public ByteBuffer getHeaderAsByteBuffer() throws IOException {
        this.bufferHeader.setBufferSize(this.buffer.remaining());
        return this.bufferHeader.getAsByteBuffer();
    }

	public void setBuffer(ByteBuffer buffer) {
		this.buffer = buffer;
	}

    public long getSessionId() {
        return bufferHeader.getSessionId();
    }

    public boolean isLastBufferForRequest() {
        return bufferHeader.isLastBufferForRequest();
    }

    public void setSessionId(long sessionId) {
        bufferHeader.setSessionId(sessionId);
    }

    public long getRequestId() {
        return bufferHeader.getRequestId();
    }

    public boolean isCloseOnTransmission() {
        return bufferHeader.isCloseOnTransmission();
    }

    public void setCloseOnTransmission(boolean closeOnTransmission) {
        bufferHeader.setCloseOnTransmission(closeOnTransmission);
    }

    public void setLastBufferForRequest(boolean lastBufferForRequest) {
        bufferHeader.setLastBufferForRequest(lastBufferForRequest);
    }

    public void setRequestId(long requestId) {
        bufferHeader.setRequestId(requestId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BufferContainer)) return false;

        BufferContainer that = (BufferContainer) o;

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
