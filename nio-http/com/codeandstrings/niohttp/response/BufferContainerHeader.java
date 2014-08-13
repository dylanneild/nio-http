package com.codeandstrings.niohttp.response;

import java.io.*;
import java.nio.ByteBuffer;

public class BufferContainerHeader implements Serializable {

    private long sessionId;
    private long requestId;
    private boolean closeOnTransmission;
    private boolean lastBufferForRequest;
    private int bufferSize;

    public BufferContainerHeader(long sessionId, long requestId, boolean closeOnTransmission, boolean lastBufferForRequest) {
        this.sessionId = sessionId;
        this.requestId = requestId;
        this.closeOnTransmission = closeOnTransmission;
        this.lastBufferForRequest = lastBufferForRequest;
    }

    public ByteBuffer getAsByteBuffer() throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(this);

        oos.flush();
        oos.close();

        byte bytes[] = baos.toByteArray();

        return ByteBuffer.allocate(bytes.length).put(bytes);

    }

    public static BufferContainerHeader fromByteBuffer(ByteBuffer buffer) throws IOException, ClassNotFoundException {

        ByteArrayInputStream bais = new ByteArrayInputStream(buffer.array());
        ObjectInputStream ois = new ObjectInputStream(bais);

        BufferContainerHeader r = (BufferContainerHeader)ois.readObject();

        ois.close();

        return r;

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

    public boolean isCloseOnTransmission() {
        return closeOnTransmission;
    }

    public void setCloseOnTransmission(boolean closeOnTransmission) {
        this.closeOnTransmission = closeOnTransmission;
    }

    public boolean isLastBufferForRequest() {
        return lastBufferForRequest;
    }

    public void setLastBufferForRequest(boolean lastBufferForRequest) {
        this.lastBufferForRequest = lastBufferForRequest;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BufferContainerHeader)) return false;

        BufferContainerHeader that = (BufferContainerHeader) o;

        if (bufferSize != that.bufferSize) return false;
        if (closeOnTransmission != that.closeOnTransmission) return false;
        if (lastBufferForRequest != that.lastBufferForRequest) return false;
        if (requestId != that.requestId) return false;
        if (sessionId != that.sessionId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (sessionId ^ (sessionId >>> 32));
        result = 31 * result + (int) (requestId ^ (requestId >>> 32));
        result = 31 * result + (closeOnTransmission ? 1 : 0);
        result = 31 * result + (lastBufferForRequest ? 1 : 0);
        result = 31 * result + bufferSize;
        return result;
    }

    @Override
    public String toString() {
        return "BufferContainerHeader{" +
                "sessionId=" + sessionId +
                ", requestId=" + requestId +
                ", closeOnTransmission=" + closeOnTransmission +
                ", lastBufferForRequest=" + lastBufferForRequest +
                ", bufferSize=" + bufferSize +
                '}';
    }
}
