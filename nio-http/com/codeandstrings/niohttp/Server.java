package com.codeandstrings.niohttp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;

import com.codeandstrings.niohttp.data.Parameters;
import com.codeandstrings.niohttp.exceptions.http.HttpException;
import com.codeandstrings.niohttp.exceptions.http.NotImplementedException;
import com.codeandstrings.niohttp.exceptions.tcp.CloseConnectionException;
import com.codeandstrings.niohttp.handlers.service.RequestHandler;
import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.BufferContainer;
import com.codeandstrings.niohttp.response.ExceptionResponseFactory;
import com.codeandstrings.niohttp.response.Response;

public class Server implements Runnable {

	private Parameters parameters;
	private InetSocketAddress socketAddress;
	private ServerSocketChannel serverSocketChannel;
	private RequestHandler requestHandler;
    private Thread handlerThread;
    private HashMap<Long,Session> sessions;

	public Server() {
		this.parameters = Parameters.getDefaultParameters();
        this.sessions = new HashMap<Long,Session>();
        Thread.currentThread().setName("NIO-HTTP Selection Thread");
	}

	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}

	private void configureSocketAddress() {
		if (this.parameters.getServerIp() != null) {
			this.socketAddress = new InetSocketAddress(this.parameters.getServerIp(), this.parameters.getPort());
		} else {
			this.socketAddress = new InetSocketAddress(this.parameters.getPort());
		}
	}

	public void setRequestHandler(RequestHandler requestHandler) {

        this.requestHandler = requestHandler;
        this.handlerThread = new Thread(this.requestHandler);
        this.handlerThread.start();

	}

	private void configureServerSocketChannel() throws IOException {
		this.serverSocketChannel = ServerSocketChannel.open();
		this.serverSocketChannel.configureBlocking(false);
		this.serverSocketChannel.bind(this.socketAddress);
	}

	@Override
	public void run() {

		try {

			this.configureSocketAddress();
			this.configureServerSocketChannel();

			Selector selector = Selector.open();

			this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            this.requestHandler.getEngineSource().register(selector, SelectionKey.OP_READ);

			while (true) {

				int keys = selector.select();

				if (keys == 0) {
					continue;
				}

				Iterator<SelectionKey> ki = selector.selectedKeys().iterator();

				while (ki.hasNext()) {

					SelectionKey key = ki.next();

					if (key.isAcceptable()) {

						/*
						 * Accept a connection register it as non-blocking and
						 * part of the master selector's selection chain (for
						 * OP_READ functions).
						 */
						SocketChannel connection = ((ServerSocketChannel) key.channel()).accept();
						connection.configureBlocking(false);
						connection.register(selector, SelectionKey.OP_READ);

					} else if (key.isReadable()) {

						SelectableChannel channel = key.channel();

						if (channel instanceof SocketChannel) {

                            Session session = (Session) key.attachment();

                            if (session == null) {
								/*
								 * The selector has triggered because a socket
								 * has generated a read event and there is
								 * presently no session available to handle it.
								 * Make a new one and ask it to handle the
								 * session.
								 */
								session = new Session((SocketChannel) channel,
										selector, this.parameters);

                                this.sessions.put(session.getSessionId(), session);

                                key.attach(session);

                                // store the session in a session store for retrieval;
							}

							try {

                                Request request = session.socketReadEvent();

                                if (request != null) {

                                    session.reset();

                                    if (this.requestHandler == null) {
                                        throw new NotImplementedException();
                                    } else {
                                        this.requestHandler.sendRequest(request);
                                        this.requestHandler.getEngineSink().register(selector, SelectionKey.OP_WRITE);
                                    }
                                }

                            } catch (CloseConnectionException e) {

                                this.sessions.remove(session.getSessionId());
                                session.getChannel().close();

                            } catch (HttpException e) {

                                Response r = (new ExceptionResponseFactory(e)).create(this.parameters);

                                BufferContainer container = new BufferContainer(session.getSessionId(),
                                        -1, r.getByteBuffer(), true, true);

                                session.queueBuffer(container);

                            }

						} else if (channel instanceof Pipe.SourceChannel) {

                            Pipe.SourceChannel sourceChannel = (Pipe.SourceChannel)channel;

                            BufferContainer container = (BufferContainer)this.requestHandler.executeBufferReadEvent();

                            if (container != null) {
                                Session session = this.sessions.get(container.getSessionId());

                                if (session != null) {
                                    session.queueBuffer(container);
                                }
                            }

						}

					} else if (key.isWritable()) {

                        SelectableChannel channel = key.channel();

                        if (channel instanceof SocketChannel) {

                            Session session = (Session) key.attachment();

                            try {
                                session.socketWriteEvent();
                            }
                            catch (CloseConnectionException e) {
                                this.sessions.remove(session.getSessionId());
                                session.getChannel().close();
                            }

                        } else if (channel instanceof Pipe.SinkChannel) {

                            if (!this.requestHandler.executeRequestWriteEvent()) {
                                channel.register(selector, 0);
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
