package com.codeandstrings.niohttp.response;

import java.nio.ByteBuffer;
import java.util.Date;

import com.codeandstrings.niohttp.data.DateUtils;
import com.codeandstrings.niohttp.data.Parameters;
import com.codeandstrings.niohttp.enums.HttpProtocol;
import com.codeandstrings.niohttp.enums.RequestMethod;
import com.codeandstrings.niohttp.exceptions.http.HttpException;
import com.codeandstrings.niohttp.request.Request;

public class ResponseFactory {

    private static void addVaryTransferEncoding(Response r) {

        String vary = r.getHeaderCaseInsensitive("vary");

        // no vary
        if (vary == null) {
            r.addHeader("Vary", "Accept-Encoding");
            return;
        }

        vary = vary.trim();

        // already exists
        if (vary.toLowerCase().indexOf("Accept-Encoding") != -1) {
            return;
        }

        r.removeHeader("vary");

        StringBuilder replacementVary = new StringBuilder();

        if (vary.length() > 0) {
            replacementVary.append(vary);
            replacementVary.append(", ");
        }

        replacementVary.append("Accept-Encoding");

        r.addHeader("Vary", replacementVary.toString());

    }

    public static Response createResponseNotModified(Request request, Date lastModified, String etag) {

        HttpProtocol protocol = request.getRequestProtocol();
        RequestMethod method = request.getRequestMethod();

        if (protocol == HttpProtocol.HTTP0_9) {
            return null;
        }

        Response r = new Response(protocol, method);

        r.setCode(304);
        r.setDescription("Not Modified");
        r.addHeader("Date", DateUtils.getRfc822DateStringGMT(new Date()));
        r.addHeader("Last-Modified", DateUtils.getRfc822DateStringGMT(lastModified));
        r.addHeader("ETag", etag);
        r.addHeader("Server", request.getServerParameters().getServerString());

        ResponseFactory.addVaryTransferEncoding(r);

        if (request.isKeepAlive()) {
            r.addHeader("Connection", "Keep-Alive");
        } else {
            r.addHeader("Connection", "close");
        }

        return r;
    }

    private static Response createBasicResponse(Request request) {

        HttpProtocol protocol = request.getRequestProtocol();
        RequestMethod requestMethod = request.getRequestMethod();

        Response r = new Response(protocol, requestMethod);

        if (protocol != HttpProtocol.HTTP0_9) {
            r.setCode(200);
            r.setDescription("OK");
            r.addHeader("Date", DateUtils.getRfc822DateStringGMT(new Date()));
            r.addHeader("Server", request.getServerParameters().getServerString());

            ResponseFactory.addVaryTransferEncoding(r);

            if (request.isKeepAlive()) {
                r.addHeader("Connection", "keep-alive");
            } else {
                r.addHeader("Connection", "close");
            }
        }

        return r;

    }

    public static Response createStreamingResponse(String contentType, Request request) {

        Response r = createBasicResponse(request);

        if (request.getRequestProtocol() != HttpProtocol.HTTP1_1) {
            // for non 1.1 requests we must close after the connection as
            // we can't rely on chunked to mark the end of the request
            r.removeHeader("Connection");
            r.addHeader("Connection", "close");
            r.addHeader("Content-Type", contentType);
        }
        else {
            r.addHeader("Transfer-Encoding", "chunked");
        }

        return r;

    }

    public static Response createResponse(String contentType, long contentSize, Request request) {

        HttpProtocol protocol = request.getRequestProtocol();
        Response r = createBasicResponse(request);

        if (protocol != HttpProtocol.HTTP0_9) {
            r.setCode(200);
            r.setDescription("OK");
            r.addHeader("Content-Type", contentType);
            r.addHeader("Content-Length", String.valueOf(contentSize));
        }

        return r;

    }

	public static Response createResponse(String content, String contentType,
			Request request) {

        byte bytes[] = content.getBytes();
		ByteBuffer contentBuffer = ByteBuffer.allocate(bytes.length);
		contentBuffer.put(bytes);

		contentBuffer.flip();

        Response r = createResponse(contentType, bytes.length, request);
		r.setBody(contentBuffer);

		return r;

	}

	public static Response createResponse(HttpException e, Parameters parameters) {
		return (new ExceptionResponseFactory(e)).create(parameters);
	}

}
