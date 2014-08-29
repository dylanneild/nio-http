package com.codeandstrings.niohttp.response;

import com.codeandstrings.niohttp.request.Request;

import java.util.Arrays;

public class ResponseContent implements ResponseMessage {

    private Request request;
    private Response response;
    private boolean lastBufferForRequest;
	private byte[] buffer;
    private byte[] headerBuffer;
    private byte[] footerBuffer;

    public ResponseContent(Request request, byte[] buffer, boolean lastBufferForRequest) {
        this.request = request;
        this.lastBufferForRequest = lastBufferForRequest;
        this.buffer = buffer;
    }

    public Response getReponse() {
        return this.response;
    }

    public void setResponse(Response response) {
        this.response = response;
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

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
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
        if (!Arrays.equals(buffer, that.buffer)) return false;
        if (!Arrays.equals(footerBuffer, that.footerBuffer)) return false;
        if (!Arrays.equals(headerBuffer, that.headerBuffer)) return false;
        if (request != null ? !request.equals(that.request) : that.request != null) return false;
        if (response != null ? !response.equals(that.response) : that.response != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = request != null ? request.hashCode() : 0;
        result = 31 * result + (response != null ? response.hashCode() : 0);
        result = 31 * result + (lastBufferForRequest ? 1 : 0);
        result = 31 * result + (buffer != null ? Arrays.hashCode(buffer) : 0);
        result = 31 * result + (headerBuffer != null ? Arrays.hashCode(headerBuffer) : 0);
        result = 31 * result + (footerBuffer != null ? Arrays.hashCode(footerBuffer) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ResponseContent{" +
                "request=" + request +
                ", response=" + response +
                ", lastBufferForRequest=" + lastBufferForRequest +
                ", buffer=" + Arrays.toString(buffer) +
                ", headerBuffer=" + Arrays.toString(headerBuffer) +
                ", footerBuffer=" + Arrays.toString(footerBuffer) +
                '}';
    }
}
