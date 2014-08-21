package com.codeandstrings.niohttp.sessions;

import com.codeandstrings.niohttp.data.IdealBlockSize;
import com.codeandstrings.niohttp.data.Parameters;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

abstract class Session {

    /*
     * Session ID Management
     */
    protected static long lastSessionId = 0;
    protected long sessionId;
    /*
     * Our channel and selector
     */
    protected SocketChannel channel;
    protected Selector selector;
    protected Parameters parameters;
    protected long nextRequestId;

    /*
     * Request acceptance data
     */
    protected int maxRequestSize;

    protected Session(SocketChannel channel, Selector selector, Parameters parameters) {

        this.sessionId = Session.lastSessionId;
        this.channel = channel;
        this.selector = selector;
        this.parameters = parameters;

        Session.lastSessionId++;

        this.maxRequestSize = IdealBlockSize.VALUE;
        this.nextRequestId = 0;
    }

    public long getSessionId() {
        return sessionId;
    }

    protected long getNextRequestId() {
        long r = this.nextRequestId;
        this.nextRequestId++;
        return r;
    }
}
