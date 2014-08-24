package com.codeandstrings.niohttp.filters.impl;

import com.codeandstrings.niohttp.enums.HttpProtocol;
import com.codeandstrings.niohttp.filters.HttpFilter;
import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.Response;
import com.codeandstrings.niohttp.response.ResponseContent;
import com.codeandstrings.niohttp.response.ResponseMessage;

import java.nio.charset.Charset;

public class ChunkedTransferHttpFilter extends HttpFilter {

    @Override
    public boolean shouldFilter(Request request, Response response) {

        if (response.getHeaderCaseInsensitive("content-length") != null) {
            return false;
        }
        else {
            return true;
        }

    }

    private void filterResponse(Request request, Response response) {
        if (request.getRequestProtocol() == HttpProtocol.HTTP0_9 || request.getRequestProtocol() == HttpProtocol.HTTP1_0) {
            // no transfer-chunked support; just make sure the response is connection: closed.
            if (response.getHeaderCaseInsensitive("connection") != null) {
                response.removeHeader("connection");
                response.addHeader("Connection", "close");
            }
        }
        else {
            // chunk-it
            response.removeHeader("transfer-encoding");
            response.addHeader("Transfer-Encoding", "chunked");
        }
    }

    private void filterContent(Request request, ResponseContent content) {

        if (request.getRequestProtocol() == HttpProtocol.HTTP0_9 || request.getRequestProtocol() == HttpProtocol.HTTP1_0) {
            return;
        }

        // add encoded header
        int contentSize = content.getBuffer() != null ? content.getBuffer().length : 0;
        String sizeHex = Integer.toHexString(contentSize);
        byte sizeHexBytes[] = sizeHex.getBytes(Charset.forName("ASCII"));
        byte headerBytes[] = new byte[sizeHexBytes.length + 2];

        for (int i = 0; i < sizeHexBytes.length; i++) {
            headerBytes[i] = sizeHexBytes[i];
        }

        headerBytes[headerBytes.length-2] = 13;
        headerBytes[headerBytes.length-1] = 10;

        content.setHeaderBuffer(headerBytes);

        // add footer
        if (content.isLastBufferForRequest()) {
            byte footer[] = { 13, 10, 48, 13, 10, 13, 10 };
            content.setFooterBuffer(footer);
        } else {
            byte footer[] = { 13, 10 };
            content.setFooterBuffer(footer);
        }

    }

    @Override
    public void filter(Request request, ResponseMessage message) {

        if (message instanceof Response) {
            this.filterResponse(request, (Response)message);
        } else {
            this.filterContent(request, (ResponseContent)message);
        }

    }

}
