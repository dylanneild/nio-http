package com.codeandstrings.niohttp.response;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ResponseContent {

    private ResponseContentHeader bufferHeader;
	private ByteBuffer buffer;
//    private ByteBuffer chunkHeader;
//    private ByteBuffer chunkFooter;

    public ResponseContent(long sessionId, long requestId, ByteBuffer buffer, boolean lastBufferForRequest) {
        this.bufferHeader = new ResponseContentHeader(sessionId, requestId, lastBufferForRequest);
        this.buffer = buffer;
    }

    public ResponseContent(ResponseContentHeader bufferHeader, ByteBuffer buffer) {
        this.bufferHeader = bufferHeader;
        this.buffer = buffer;
    }

//    private byte[] getChunkHeader() {
//        try {
//            int size = this.buffer.remaining();
//            byte s[] = Integer.toHexString(size).getBytes("ASCII");
//            byte r[] = new byte[s.length + 2];
//
//            for (int i = 0; i < s.length; i++) {
//                r[i] = s[i];
//            }
//
//            r[r.length - 2] = 13;
//            r[r.length - 1] = 10;
//
//            return r;
//        } catch (Exception e) {
//            return null;
//        }
//
//    }
//
//    private byte[] getChunkFooter() {
//
//        if (this.isLastBufferForRequest()) {
//            byte r[] = {13, 10, 48, 13, 10, 13, 10};
//            return r;
//        } else {
//            byte r[] = {13, 10};
//            return r;
//        }
//
//    }
//
//    public void chunkBuffer() {
//
//        byte[] chunkHeader = getChunkHeader();
//        byte[] chunkFooter = getChunkFooter();
//
//        ByteBuffer replacement = ByteBuffer.allocate(this.getBuffer().remaining() + chunkHeader.length + chunkFooter.length);
//
//        replacement.put(chunkHeader);
//        replacement.put(this.buffer);
//        replacement.put(chunkFooter);
//
//        replacement.flip();
//
//        this.buffer = replacement;
//
//    }

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
