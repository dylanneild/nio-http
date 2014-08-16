package com.codeandstrings.niohttp.exceptions.http;

public class ForbiddenException extends HttpException {

    private static final long serialVersionUID = 775725977887192523L;

    public ForbiddenException() {
        super(403, "Forbidden");
    }

}
