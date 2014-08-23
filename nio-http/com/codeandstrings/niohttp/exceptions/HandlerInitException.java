package com.codeandstrings.niohttp.exceptions;

public class HandlerInitException extends Exception {

    private static final long serialVersionUID = -2640902412834751564L;

    public HandlerInitException() {
        super();
    }

    public HandlerInitException(Throwable cause) {
        super(cause);
    }

    public HandlerInitException(String message) {
        super(message);
    }
}
