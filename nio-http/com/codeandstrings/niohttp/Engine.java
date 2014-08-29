package com.codeandstrings.niohttp;

import com.codeandstrings.niohttp.data.Parameters;
import com.codeandstrings.niohttp.exceptions.EngineInitException;
import com.codeandstrings.niohttp.exceptions.HandlerInitException;
import com.codeandstrings.niohttp.exceptions.InvalidHandlerException;
import com.codeandstrings.niohttp.exceptions.http.HttpException;
import com.codeandstrings.niohttp.exceptions.http.NotFoundException;
import com.codeandstrings.niohttp.exceptions.tcp.CloseConnectionException;
import com.codeandstrings.niohttp.filters.HttpFilter;
import com.codeandstrings.niohttp.handlers.base.RequestHandler;
import com.codeandstrings.niohttp.handlers.broker.RequestHandlerBroker;
import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.ExceptionResponseFactory;
import com.codeandstrings.niohttp.response.Response;
import com.codeandstrings.niohttp.response.ResponseContent;
import com.codeandstrings.niohttp.response.ResponseMessage;
import com.codeandstrings.niohttp.sessions.HttpSession;
import com.codeandstrings.niohttp.sessions.Session;
import com.codeandstrings.niohttp.wire.ChannelQueue;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Engine extends Thread {

	private Parameters parameters;
    private RequestHandlerBroker requestHandlerBroker;
    private List<HttpFilter> filters;
    private ChannelQueue channelQueue;
    private Selector selector;
    private HashSet<Session> sessions;

	public Engine(Parameters parameters) throws EngineInitException {

        this.parameters = parameters.copy();
        this.filters = new ArrayList<>();
        this.sessions = new HashSet<>();

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

    public void addFilter(HttpFilter filter) {
        this.filters.add(filter);
    }

    private void cleanupFilters(long sessionId) {
        for (HttpFilter filter : this.filters) {
            filter.cleanup(sessionId);
        }
    }

    private void cleanupAndCloseSession(Session session) throws IOException {
        this.cleanupFilters(session.getSessionId());
        this.sessions.remove(session);
        session.getChannel().close();
    }

    private void executeChannelReadFromServer(SelectableChannel channel) throws IOException {
        if (this.channelQueue.shouldReadObject()) {

            SocketChannel newChannel = (SocketChannel) this.channelQueue.getNextObject();

            if (newChannel != null) {
                newChannel.configureBlocking(false);
                newChannel.setOption(StandardSocketOptions.TCP_NODELAY, this.parameters.isTcpNoDelay());
                newChannel.register(this.selector, SelectionKey.OP_READ);
            }
        }
    }

    private void executeChannelReadFromNetwork(SelectionKey key, SelectableChannel channel) throws IOException, InterruptedException {

        HttpSession session = (HttpSession) key.attachment();

        if (session == null) {
            session = new HttpSession((SocketChannel) channel, this.selector, this.parameters);
            sessions.add(session);
            key.attach(session);
        }

        try {
            Request request = session.socketReadEvent();

            if (request != null) {
                session.resetHeaderReads();

                RequestHandler requestHandler = this.requestHandlerBroker.getHandlerForRequest(request);

                if (requestHandler == null) {
                    Response ex = (new ExceptionResponseFactory(new NotFoundException())).create(request);
                    session.queueMessage(ex);
                } else {
                    requestHandler.getHandlerQueue().sendObject(request);
                }

            }

        } catch (CloseConnectionException e) {
            this.cleanupAndCloseSession(session);
        } catch (HttpException e) {
            Response r = (new ExceptionResponseFactory(e)).create(session.getSessionId(), this.parameters);
            session.queueMessage(r);
        }
    }

    private void executeChannelReadFromHandler(SelectionKey key, SelectableChannel channel) throws IOException {
        // response is from a handler
        ChannelQueue queue = (ChannelQueue) key.attachment();

        if (queue == null) {
            queue = this.requestHandlerBroker.getHandlerForEngineReceive(channel).getEngineQueue();
            key.attach(queue);
        }

        if (queue.shouldReadObject()) {

            ResponseMessage container = (ResponseMessage)queue.getNextObject();
            Request request = container == null ? null : container.getRequest();
            Session session = request == null ? null : request.getSession();

            if (container == null || request == null || session == null) {
                System.err.println("Unusual situation: received object lacks reference chain:");
                Thread.dumpStack();
                return;
            }

            Response response = null;

            if (container instanceof Response) {
                response = (Response) container;
            } else {
                response = ((ResponseContent) container).getReponse();

                if (response == null) {
                    response = session.getResponse(request.getRequestId());
                }
            }

            // run any applicable filters
            if (request != null && response != null) {
                for (HttpFilter filter : this.filters) {
                    if (filter.shouldFilter(request, response))
                        filter.filter(request, container);
                }

                session.queueMessage(container);
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

            Session session = (Session) key.attachment();

            try {
                session.socketWriteEvent();
            } catch (Exception e) {
                this.cleanupAndCloseSession(session);
            }
        }
        else  {

            ChannelQueue queue = (ChannelQueue) key.attachment();

            if (queue == null) {
                queue = this.requestHandlerBroker.getHandlerForEngineSend(channel).getHandlerQueue();
            }

            queue.sendObject(null);
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
