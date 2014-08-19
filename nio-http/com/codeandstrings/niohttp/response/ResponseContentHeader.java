package com.codeandstrings.niohttp.response;

import java.io.*;
import java.nio.ByteBuffer;

public class ResponseContentHeader implements Externalizable {

    private long sessionId;
    private long requestId;
    private long sequenceId;
    private boolean lastBufferForRequest;
    private int bufferSize;

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.sessionId = in.readLong();
        this.requestId = in.readLong();
        this.sequenceId = in.readLong();
        this.lastBufferForRequest = in.readBoolean();
        this.bufferSize = in.readInt();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(this.sessionId);
        out.writeLong(this.requestId);
        out.writeLong(this.sequenceId);
        out.writeBoolean(this.lastBufferForRequest);
        out.writeInt(this.bufferSize);
    }

    public ResponseContentHeader() {}

    public ResponseContentHeader(long sessionId, long requestId, long sequenceId, boolean lastBufferForRequest) {
        this.sessionId = sessionId;
        this.requestId = requestId;
        this.sequenceId = sequenceId;
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

    public static ResponseContentHeader fromByteBuffer(ByteBuffer buffer) throws IOException, ClassNotFoundException {

        ByteArrayInputStream bais = new ByteArrayInputStream(buffer.array());
        ObjectInputStream ois = new ObjectInputStream(bais);

        ResponseContentHeader r = (ResponseContentHeader)ois.readObject();

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

    public long getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(long sequenceId) {
        this.sequenceId = sequenceId;
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
        if (o == null || getClass() != o.getClass()) return false;

        ResponseContentHeader that = (ResponseContentHeader) o;

        if (bufferSize != that.bufferSize) return false;
        if (lastBufferForRequest != that.lastBufferForRequest) return false;
        if (requestId != that.requestId) return false;
        if (sequenceId != that.sequenceId) return false;
        if (sessionId != that.sessionId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (sessionId ^ (sessionId >>> 32));
        result = 31 * result + (int) (requestId ^ (requestId >>> 32));
        result = 31 * result + (int) (sequenceId ^ (sequenceId >>> 32));
        result = 31 * result + (lastBufferForRequest ? 1 : 0);
        result = 31 * result + bufferSize;
        return result;
    }

    @Override
    public String toString() {
        return "BufferContainerHeader{" +
                "sessionId=" + sessionId +
                ", requestId=" + requestId +
                ", sequenceId=" + sequenceId +
                ", lastBufferForRequest=" + lastBufferForRequest +
                ", bufferSize=" + bufferSize +
                '}';
    }
}
