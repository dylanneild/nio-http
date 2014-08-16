package com.codeandstrings.niohttp.exceptions.http;

import com.codeandstrings.niohttp.request.Request;

public abstract class HttpException extends Exception {

	private static final long serialVersionUID = 6003211652655022577L;
	private int code;
	private String description;

    public HttpException(int code, String description) {
        this.code = code;
        this.description = description;
    }

	public int getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return "HttpException [code=" + code + ", description=" + description
				+ "]";
	}

}