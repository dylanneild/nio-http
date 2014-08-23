package com.codeandstrings.niohttp;

import com.codeandstrings.niohttp.data.Parameters;
import com.codeandstrings.niohttp.exceptions.EngineInitException;
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
        this.requestHandlerBroker = new RequestHandlerBroker();

        try {
            this.selector = Selector.open();
            this.channelQueue = new ChannelQueue(this.selector);
        } catch (IOException e) {
            throw new EngineInitException(e);
        }

	}

    public ChannelQueue getEngineChannelQueue() {
        return this.channelQueue;
    }

	public void addRequestHandler(String path, Class handler) throws InvalidHandlerException {
        this.requestHandlerBroker.addHandler(path, handler);
	}

	@Override
	public void run() {

        Thread.currentThread().setName("NIO-HTTP Engine Thread " + Thread.currentThread().getId());

        try {

            this.requestHandlerBroker.setSelectorReadHandler(this.selector);

			while (true) {

                int keys = this.selector.select();

				if (keys == 0) {
                    continue;
				}

				Iterator<SelectionKey> ki = this.selector.selectedKeys().iterator();

				while (ki.hasNext()) {

					SelectionKey key = ki.next();

					if (key.isReadable()) {

						SelectableChannel channel = key.channel();

                        if (this.channelQueue.isThisChannel(channel)) {

                            if (this.channelQueue.shouldReadObject()) {
                                SocketChannel newChannel = (SocketChannel) this.channelQueue.getNextObject();

                                if (newChannel != null) {
                                    newChannel.configureBlocking(false);
                                    newChannel.register(this.selector, SelectionKey.OP_READ);
                                }
                            }

                        } else if (channel instanceof SocketChannel) {

                            HttpSession session = (HttpSession) key.attachment();

                            if (session == null) {
								/*
								 * The selector has triggered because a socket
								 * has generated a read event and there is
								 * presently no session available to handle it.
								 * Make a new one and ask it to handle the
								 * session.
								 */
								session = new HttpSession((SocketChannel) channel,
										this.selector, this.parameters);

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
                                        requestHandler.sendRequest(request);
                                        requestHandler.getEngineSink().register(this.selector, SelectionKey.OP_WRITE);
                                    }

                                }

                            } catch (CloseConnectionException e) {
                                this.sessions.remove(session.getSessionId());
                                session.getChannel().close();
                            } catch (HttpException e) {
                                Response r = (new ExceptionResponseFactory(e)).create(session.getSessionId(), this.parameters);
                                session.queueMessage(r);
                            }

						} else if (channel instanceof Pipe.SourceChannel) {

                            Pipe.SourceChannel sourceChannel = (Pipe.SourceChannel)channel;
                            RequestHandler requestHandler = (RequestHandler)key.attachment();

                            if (requestHandler == null) {
                                requestHandler = this.requestHandlerBroker.getHandlerForEngineSourceChannel((Pipe.SourceChannel)channel);
                                key.attach(requestHandler);
                            }

                            ResponseMessage container = requestHandler.executeBufferReadEvent();

                            if (container != null) {
                                Session session = this.sessions.get(container.getSessionId());

                                if (session != null) {
                                    session.queueMessage(container);
                                }
                            }

						}

					}
                    else if (key.isWritable()) {

                        SelectableChannel channel = key.channel();

                        if (channel instanceof SocketChannel) {

                            HttpSession session = (HttpSession) key.attachment();

                            // TODO: I assume this channel attachment will be dead on a second request
                            // TODO: as resolving a socket to a session may not be possible once it's disassociated
                            // TODO: with the selector. Right now this is somewhat moot because all connections
                            // TODO: Die after one request (no keepalive).

                            try {
                                session.socketWriteEvent();
                            }
                            catch (Exception e) {
                                this.sessions.remove(session.getSessionId());
                                session.getChannel().close();
                            }

                        } else if (channel instanceof Pipe.SinkChannel) {

                            RequestHandler requestHandler = this.requestHandlerBroker.getHandlerForEngineSinkChannel((Pipe.SinkChannel) channel);
                            key.attach(requestHandler);

                            // TODO: This attachment never works... probably because we unregister below...
                            // TODO: We need to make the pathing between the Server and the handler a little
                            // TODO: Faster than this potentially.

                            if (!requestHandler.executeRequestWriteEvent()) {
                                channel.register(this.selector, 0);
                            }

                        }

					}

					ki.remove();

				}

			}

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }


	}

}
