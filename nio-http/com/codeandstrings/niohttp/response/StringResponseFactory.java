package com.codeandstrings.niohttp.response;

import com.codeandstrings.niohttp.request.Request;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class StringResponseFactory {

    private Request request;
    private String contentType;
    private String response;

    private ResponseContent header;
    private ResponseContent body;

    public ResponseContent getHeader() {
        return header;
    }

    public ResponseContent getBody() {
        return body;
    }

    private void fabricateHeader() {

        Response r = ResponseFactory.createResponse(this.contentType, this.body.getBuffer().length, this.request);

        if (r.getByteBuffer() != null) {
            this.header = new ResponseContent(this.request.getSessionId(), this.request.getRequestId(), r.getByteBuffer(), false);
        }

    }

    private void fabricateBody() {

        byte bytes[] = this.response.getBytes(Charset.forName("UTF-8"));
        this.body = new ResponseContent(this.request.getSessionId(), this.request.getRequestId(), bytes, true);

    }

    private void fabricate() {
        this.fabricateBody();
        this.fabricateHeader();
    }

    public StringResponseFactory(Request request, String contentType, String response) {

        this.request = request;
        this.contentType = contentType;
        this.response = response;

        this.fabricate();

    }


}
