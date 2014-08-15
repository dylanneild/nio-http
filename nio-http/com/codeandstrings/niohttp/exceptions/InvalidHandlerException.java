package com.codeandstrings.niohttp.exceptions;

public class InvalidHandlerException extends Exception {

    private static final long serialVersionUID = 2245298650688165015L;

    public InvalidHandlerException(Throwable cause) {
        super(cause);
    }

    public InvalidHandlerException() {
        super();
    }

    public InvalidHandlerException(String message) {
        super(message);
    }

}
