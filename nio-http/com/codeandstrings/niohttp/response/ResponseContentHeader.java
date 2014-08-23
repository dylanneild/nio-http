package com.codeandstrings.niohttp.response;

public class ResponseContentHeader {

    private long sessionId;
    private long requestId;
    private boolean lastBufferForRequest;

    public ResponseContentHeader(long sessionId, long requestId, boolean lastBufferForRequest) {
        this.sessionId = sessionId;
        this.requestId = requestId;
        this.lastBufferForRequest = lastBufferForRequest;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResponseContentHeader that = (ResponseContentHeader) o;

        if (lastBufferForRequest != that.lastBufferForRequest) return false;
        if (requestId != that.requestId) return false;
        if (sessionId != that.sessionId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (sessionId ^ (sessionId >>> 32));
        result = 31 * result + (int) (requestId ^ (requestId >>> 32));
        result = 31 * result + (lastBufferForRequest ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ResponseContentHeader{" +
                "sessionId=" + sessionId +
                ", requestId=" + requestId +
                ", lastBufferForRequest=" + lastBufferForRequest +
                '}';
    }
}
