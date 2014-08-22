package com.codeandstrings.niohttp.exceptions;

public class EngineInitException extends Exception {

    private static final long serialVersionUID = -4011918840492773491L;

    public EngineInitException() {
        super();
    }

    public EngineInitException(Throwable cause) {
        super(cause);
    }

    public EngineInitException(String message) {
        super(message);
    }
}
