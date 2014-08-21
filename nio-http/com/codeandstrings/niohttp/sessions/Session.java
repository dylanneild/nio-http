package com.codeandstrings.niohttp.sessions;

import com.codeandstrings.niohttp.data.IdealBlockSize;
import com.codeandstrings.niohttp.data.Parameters;
import com.codeandstrings.niohttp.exceptions.http.HttpException;
import com.codeandstrings.niohttp.exceptions.tcp.CloseConnectionException;
import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.Response;
import com.codeandstrings.niohttp.response.ResponseContent;
import com.codeandstrings.niohttp.response.ResponseMessage;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

public abstract class Session {

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

    /* Response Management */
    protected Queue<Request> requestQueue;
    protected Queue<Response> responseQueue;
    protected Queue<ResponseContent> contentQueue;

    protected Session(SocketChannel channel, Selector selector, Parameters parameters) {

        this.sessionId = Session.lastSessionId;
        this.channel = channel;
        this.selector = selector;
        this.parameters = parameters;

        Session.lastSessionId++;

        this.maxRequestSize = IdealBlockSize.VALUE;
        this.nextRequestId = 0;
        this.requestQueue = new LinkedList<Request>();
        this.responseQueue = new LinkedList<Response>();
        this.contentQueue = new LinkedList<ResponseContent>();
    }

    public long getSessionId() {
        return sessionId;
    }

    protected long getNextRequestId() {
        long r = this.nextRequestId;
        this.nextRequestId++;
        return r;
    }

    public SocketChannel getChannel() {
        return this.channel;
    }


    protected void setSelectionRequest(boolean write)
            throws ClosedChannelException {

        int ops;

        if (write) {
            ops = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
        } else {
            ops = SelectionKey.OP_READ;
        }

        this.channel.register(this.selector, ops, this);

    }

    public void queueMessage(ResponseMessage message) throws IOException {

        if (message instanceof Response) {
            this.responseQueue.add((Response)message);
        } else {
            this.contentQueue.add((ResponseContent)message);
        }

        this.setSelectionRequest(true);

    }

    public void removeRequest(Request request) {
        this.requestQueue.remove(request);
    }

    public abstract void resetHeaderReads();
    public abstract void socketWriteEvent() throws IOException, CloseConnectionException;
    public abstract Request socketReadEvent() throws IOException, CloseConnectionException, HttpException;
}
