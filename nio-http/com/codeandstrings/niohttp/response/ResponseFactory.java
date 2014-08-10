package com.codeandstrings.niohttp.response;

import java.nio.ByteBuffer;

import com.codeandstrings.niohttp.data.Parameters;
import com.codeandstrings.niohttp.enums.HttpProtocol;
import com.codeandstrings.niohttp.enums.RequestMethod;
import com.codeandstrings.niohttp.exceptions.http.HttpException;

public class ResponseFactory {

	public static Response createResponse(String content, String contentType,
			HttpProtocol protocol, RequestMethod method, Parameters parameters) {

		Response r = new Response(protocol, method, parameters);

		if (protocol == HttpProtocol.HTTP1_1) {
			r.setCode(200);
			r.setDescription("OK");
			r.addHeader("Content-Type", contentType);
			r.addHeader("Content-Length", String.valueOf(content.length()));
		}		

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
