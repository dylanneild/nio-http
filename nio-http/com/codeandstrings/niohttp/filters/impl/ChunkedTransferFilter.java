package com.codeandstrings.niohttp.filters.impl;

import com.codeandstrings.niohttp.enums.HttpProtocol;
import com.codeandstrings.niohttp.filters.FilterBase;
import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.Response;
import com.codeandstrings.niohttp.response.ResponseContent;

public class ChunkedTransferFilter extends FilterBase {

    @Override
    public boolean shouldFilter(Request request, Response response) {

        if (request.getRequestProtocol() == HttpProtocol.HTTP1_0 || request.getRequestProtocol() == HttpProtocol.HTTP0_9) {
            return false;
        }

        return response.getHeaderCaseInsensitive("content-length") == null;

    }

    @Override
    public void filter(Response response) {
        response.addHeader("Transfer-Encoding", "chunked");
    }

    @Override
    public void filter(ResponseContent content) {
        String contentSizeHex = Integer.toHexString(content.getBuffer().length);
    }
}
