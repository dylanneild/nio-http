package com.codeandstrings.niohttp;

import com.codeandstrings.niohttp.data.Parameters;
import com.codeandstrings.niohttp.exceptions.EngineInitException;
import com.codeandstrings.niohttp.exceptions.HandlerInitException;
import com.codeandstrings.niohttp.exceptions.InvalidHandlerException;
import com.codeandstrings.niohttp.exceptions.http.HttpException;
import com.codeandstrings.niohttp.exceptions.http.NotFoundException;
import com.codeandstrings.niohttp.exceptions.tcp.CloseConnectionException;
import com.codeandstrings.niohttp.handlers.base.RequestHandler;
import com.codeandstrings.niohttp.handlers.broker.RequestHandlerBroker;
import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.ExceptionResponseFactory;
import com.codeandstrings.niohttp.response.Response;
import com.codeandstrings.niohttp.response.ResponseMessage;
import com.codeandstrings.niohttp.sessions.HttpSession;
import com.codeandstrings.niohttp.sessions.Session;
import com.codeandstrings.niohttp.wire.ChannelQueue;

import java.io.IOException;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;

public class Engine extends Thread {

	private Parameters parameters;
    private HashMap<Long,Session> sessions;
    private RequestHandlerBroker requestHandlerBroker;
    private ChannelQueue channelQueue;
    private Selector selector;

	public Engine(Parameters parameters) throws EngineInitException {

        this.parameters = parameters.copy();
        this.sessions = new HashMap<Long,Session>();

        try {
            this.selector = Selector.open();
            this.channelQueue = new ChannelQueue(this.selector);
            this.requestHandlerBroker = new RequestHandlerBroker(this.selector);
        } catch (IOException e) {
            throw new EngineInitException(e);
        }

	}

    public ChannelQueue getEngineChannelQueue() {
        return this.channelQueue;
    }

	public void addRequestHandler(String path, Class handler) throws InvalidHandlerException, HandlerInitException {
        this.requestHandlerBroker.addHandler(path, handler);
	}

    private void executeChannelReadFromServer(SelectableChannel channel) throws IOException {
        if (this.channelQueue.shouldReadObject()) {
            SocketChannel newChannel = (SocketChannel) this.channelQueue.getNextObject();

            if (newChannel != null) {
                newChannel.configureBlocking(false);
                newChannel.register(this.selector, SelectionKey.OP_READ);
            }
        }
    }

    private void executeChannelReadFromNetwork(SelectionKey key, SelectableChannel channel) throws IOException, InterruptedException {

        HttpSession session = (HttpSession) key.attachment();

        if (session == null) {
            session = new HttpSession((SocketChannel) channel, this.selector, this.parameters);
            this.sessions.put(session.getSessionId(), session);
            key.attach(session);
        }

        try {
            Request request = session.socketReadEvent();

            if (request != null) {
                session.resetHeaderReads();

                RequestHandler requestHandler = this.requestHandlerBroker.getHandlerForRequest(request);

                if (requestHandler == null) {
                    session.removeRequest(request);
                    throw new NotFoundException();
                } else {
                    requestHandler.getHandlerQueue().sendObject(request);
                }

            }

        } catch (CloseConnectionException e) {
            this.sessions.remove(session.getSessionId());
            session.getChannel().close();
        } catch (HttpException e) {
            Response r = (new ExceptionResponseFactory(e)).create(session.getSessionId(), this.parameters);
            session.queueMessage(r);
        }
    }

    private void executeChannelReadFromHandler(SelectionKey key, SelectableChannel channel) throws IOException {
        // response is from a handler
        RequestHandler handler = (RequestHandler) key.attachment();

        if (handler == null) {
            handler = this.requestHandlerBroker.getHandlerForEngineReceive(channel);
            key.attach(handler);
        }

        ChannelQueue queue = handler.getEngineQueue();

        if (queue.shouldReadObject()) {
            ResponseMessage container = (ResponseMessage)queue.getNextObject();

            if (container != null) {
                Session session = this.sessions.get(container.getSessionId());

                if (session != null) {
                    session.queueMessage(container);
                }
            }
        }
    }

    private void executeChannelRead(SelectionKey key) throws IOException, InterruptedException {
        /* Key Read */
        SelectableChannel channel = key.channel();

        if (this.channelQueue.isThisReadChannel(channel)) {
            this.executeChannelReadFromServer(channel);
        } else if (channel instanceof SocketChannel) {
            this.executeChannelReadFromNetwork(key, channel);
        } else {
            this.executeChannelReadFromHandler(key, channel);
        }
    }

    private void executeChannelWrite(SelectionKey key) throws IOException, InterruptedException {

        SelectableChannel channel = key.channel();

        if (channel instanceof SocketChannel) {

            HttpSession session = (HttpSession) key.attachment();

            try {
                session.socketWriteEvent();
            } catch (Exception e) {
                this.sessions.remove(session.getSessionId());
                session.getChannel().close();
            }
        }
        else  {
            this.requestHandlerBroker.getHandlerForEngineSend(channel).getHandlerQueue().sendObject(null);
        }

    }

	@Override
	public void run() {

        Thread.currentThread().setName("NIO-HTTP Engine Thread " + Thread.currentThread().getId());

        try {

            while (true) {

                int keys = this.selector.select();

                if (keys == 0) {
                    continue;
                }

                Iterator<SelectionKey> ki = this.selector.selectedKeys().iterator();

                while (ki.hasNext()) {

                    SelectionKey key = ki.next();

                    if (key.isReadable()) {
                        this.executeChannelRead(key);
                    }
                    else if (key.isWritable()) {
                        this.executeChannelWrite(key);
                    }

                    ki.remove();
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

	}

}
