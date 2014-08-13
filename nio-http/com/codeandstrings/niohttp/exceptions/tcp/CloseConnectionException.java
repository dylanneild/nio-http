package com.codeandstrings.niohttp.exceptions.tcp;

public class CloseConnectionException extends Exception {

    private static final long serialVersionUID = -7199753341986886343L;

    public CloseConnectionException(Throwable cause) {
        super(cause);
    }

    public CloseConnectionException() {
        super();
    }

}
