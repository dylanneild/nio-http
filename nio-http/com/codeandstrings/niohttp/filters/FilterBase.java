package com.codeandstrings.niohttp.filters;

import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.Response;
import com.codeandstrings.niohttp.response.ResponseContent;

public abstract class FilterBase {

    public abstract boolean shouldFilter(Request request, Response response);
    public abstract void filter(Request request, Response response);
    public abstract void filter(Request request, ResponseContent content);

}
