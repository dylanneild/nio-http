package com.codeandstrings.niohttp.exceptions.http;

public class NotImplementedException extends HttpException {

    private static final long serialVersionUID = 7231599349122433837L;

    public NotImplementedException() {
        super(501, "Not Implemented");
    }

}
