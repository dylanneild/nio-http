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
import java.util.*;

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

    /* Maps */
    protected Map<Long,Request> requestMap;
    protected Map<Long,Response> responseMap;

    /* Constructor */
    protected Session(SocketChannel channel, Selector selector, Parameters parameters) {

        this.sessionId = Session.lastSessionId;
        this.channel = channel;
        this.selector = selector;
        this.parameters = parameters;

        Session.lastSessionId++;

        this.maxRequestSize = IdealBlockSize.VALUE;
        this.nextRequestId = 0;

        this.requestMap = new HashMap<>();
        this.responseMap = new HashMap<>();

        this.requestQueue = new LinkedList<>();
        this.contentQueue = new LinkedList<>();
        this.responseQueue = new LinkedList<>();

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

        if (this.channel == null)
            return;

        if (!this.channel.isOpen() || !this.channel.isConnected())
            return;

        if (message instanceof Response) {

            Response response = (Response)message;
            this.responseQueue.add(response);

            if (response.getRequest() != null) {
                this.responseMap.put(response.getRequest().getRequestId(), response);
            }

        } else {
            this.contentQueue.add((ResponseContent)message);
        }

        this.setSelectionRequest(true);
    }

    public void removeRequest(Request request) {
        this.requestQueue.remove(request);
        this.requestMap.remove(request.getRequestId());
    }

    public Request getRequest(long requestId) {
        return this.requestMap.get(requestId);
    }

    public Response getResponse(long requestId) { return this.responseMap.get(requestId); }

    public abstract void resetHeaderReads();
    public abstract void socketWriteEvent() throws IOException, CloseConnectionException;
    public abstract Request socketReadEvent() throws IOException, CloseConnectionException, HttpException;
}
