package com.codeandstrings.niohttp.handlers;

import com.codeandstrings.niohttp.request.Request;

public class RequestHandlerMount {

    private String mountPoint;
    private RequestHandler handler;
    private boolean next;

    public RequestHandlerMount(String mountPoint, RequestHandler handler) {
        this.mountPoint = mountPoint;
        this.handler = handler;
    }

    public boolean matches(Request request) {
        return request.getRequestURI().getPath().matches(this.mountPoint);
    }

    public RequestHandler getHandler() {
        return handler;
    }

    public boolean isNext() {
        return next;
    }

    public void setNext(boolean next) {
        this.next = next;
    }
}
