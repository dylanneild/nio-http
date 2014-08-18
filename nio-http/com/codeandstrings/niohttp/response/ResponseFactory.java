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

            if (request.isKeepAlive()) {
                r.addHeader("Connection", "Keep-Alive");
            } else {
                r.addHeader("Connection", "close");
            }
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

        Response r = createResponse(contentType, content.length(), request);

        byte bytes[] = content.getBytes();
		ByteBuffer contentBuffer = ByteBuffer.allocate(bytes.length);
		contentBuffer.put(bytes);

		contentBuffer.flip();

		r.setBody(contentBuffer);

		return r;

	}

	public static Response createResponse(HttpException e, Parameters parameters) {
		return (new ExceptionResponseFactory(e)).create(parameters);
	}

}
