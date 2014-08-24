package com.codeandstrings.niohttp.handlers.base;

import com.codeandstrings.niohttp.exceptions.HandlerInitException;
import com.codeandstrings.niohttp.wire.*;

import java.nio.channels.Selector;

public abstract class RequestHandler extends Thread {

    protected ChannelQueue handlerQueue;
    protected ChannelQueue engineQueue;
    protected Selector selector;

    public RequestHandler() throws HandlerInitException {

        try {
            this.selector = Selector.open();
            this.handlerQueue = new ChannelQueue(this.selector);

        } catch (Exception e) {
            throw new HandlerInitException(e);
        }

    }

    public void setEngineQueueSelector(Selector selector) throws HandlerInitException {
        try {
            this.engineQueue = new ChannelQueue(selector);
            this.engineQueue.setWriteSelector(this.selector);
            this.handlerQueue.setWriteSelector(selector);
        } catch (Exception e) {
            throw new HandlerInitException(e);
        }
    }

    public ChannelQueue getHandlerQueue() {
        return handlerQueue;
    }

    public ChannelQueue getEngineQueue() {
        return engineQueue;
    }

    @Override
    public void run() {
        this.currentThread().setName("NIO-HTTP Handler " + Thread.currentThread().getId());
        this.listenForRequests();
    }

    protected abstract void listenForRequests();

    protected abstract String getHandlerDescription();

}