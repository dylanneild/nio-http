package com.codeandstrings.niohttp.response;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ResponseContent implements Externalizable, ResponseMessage {

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(this.bufferHeader);

        if (this.buffer == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(this.buffer.length);
            out.write(this.buffer, 0, this.buffer.length);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        this.bufferHeader = (ResponseContentHeader)in.readObject();
        int arraySize = in.readInt();

        if (arraySize==-1) {
            this.buffer = null;
        } else {
            this.buffer = new byte[arraySize];

            int arrayReadLocation = 0;

            while (true) {
                int readBytes = in.read(this.buffer, arrayReadLocation, arraySize - arrayReadLocation);

                if (arraySize - arrayReadLocation == 0) {
                    break;
                } else {
                    arrayReadLocation = arrayReadLocation + readBytes;
                }
            }
        }

    }

    private ResponseContentHeader bufferHeader;
	private byte[] buffer;

    public ResponseContent(long sessionId, long requestId, byte[] buffer, boolean lastBufferForRequest) {
        this.bufferHeader = new ResponseContentHeader(sessionId, requestId, lastBufferForRequest);
        this.buffer = buffer;
    }

    public ResponseContent(ResponseContentHeader bufferHeader, byte[] buffer) {
        this.bufferHeader = bufferHeader;
        this.buffer = buffer;
    }

    public ResponseContent() {}

    public byte[] getBuffer() {
		return buffer;
	}

	public void setBuffer(byte[] buffer) {
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

    public void setLastBufferForRequest(boolean lastBufferForRequest) {
        bufferHeader.setLastBufferForRequest(lastBufferForRequest);
    }

    public void setRequestId(long requestId) {
        bufferHeader.setRequestId(requestId);
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
