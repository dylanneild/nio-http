package com.codeandstrings.niohttp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;

import com.codeandstrings.niohttp.data.Parameters;
import com.codeandstrings.niohttp.exceptions.EngineInitException;
import com.codeandstrings.niohttp.exceptions.HandlerInitException;
import com.codeandstrings.niohttp.exceptions.InsufficientConcurrencyException;
import com.codeandstrings.niohttp.exceptions.InvalidHandlerException;
import com.codeandstrings.niohttp.filters.HttpFilter;
import com.codeandstrings.niohttp.filters.impl.ChunkedTransferHttpFilter;

public class Server implements Runnable {

    private Selector selector;
	private Parameters parameters;
	private InetSocketAddress socketAddress;
	private ServerSocketChannel serverSocketChannel;
    private Engine[] engineSchedule;
    private int engineConcurrency;
    private int enginePointer;

	public Server(int concurrency) throws InsufficientConcurrencyException, EngineInitException {

        if (concurrency < 1) {
            throw new InsufficientConcurrencyException();
        }

        this.parameters = Parameters.getDefaultParameters();
        this.engineSchedule = new Engine[concurrency];

        this.engineConcurrency = concurrency;
        this.enginePointer = 0;

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new EngineInitException(e);
        }

        for (int i = 0; i < this.engineConcurrency; i++) {
            this.engineSchedule[i] = new Engine(this.parameters);
            this.engineSchedule[i].getEngineChannelQueue().setWriteSelector(this.selector);
        }

        Thread.currentThread().setName("NIO-HTTP Server Thread");

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

	public void addRequestHandler(String path, Class handler) throws InvalidHandlerException, HandlerInitException {
        for (Engine engine : this.engineSchedule) {
            engine.addRequestHandler(path, handler);
        }
	}

    public void addFilter(HttpFilter filter) {
        for (Engine engine : this.engineSchedule) {
            engine.addFilter(filter);
        }
    }

	private void configureServerSocketChannel() throws IOException {
		this.serverSocketChannel = ServerSocketChannel.open();
		this.serverSocketChannel.configureBlocking(false);
		this.serverSocketChannel.bind(this.socketAddress, this.parameters.getConnectionBacklog());
	}

	@Override
	public void run() {

        /* Add mandatory filters */
        for (Engine engine : this.engineSchedule) {
            engine.addFilter(new ChunkedTransferHttpFilter());
        }

        /* start engine threads */
        for (Engine engine : this.engineSchedule) {
            engine.start();
        }

        try {

			this.configureSocketAddress();
			this.configureServerSocketChannel();

			this.serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);

			while (true) {

				int keys = this.selector.select();

				if (keys == 0) {
					continue;
				}

				Iterator<SelectionKey> ki = this.selector.selectedKeys().iterator();

				while (ki.hasNext()) {

					SelectionKey key = ki.next();

					if (key.isAcceptable()) {

						/*
						 * Accept a connection...
						 */
						SocketChannel connection = ((ServerSocketChannel) key.channel()).accept();
                        Engine nextEngine = this.engineSchedule[this.enginePointer];
                        nextEngine.getEngineChannelQueue().sendObject(connection);

                        this.enginePointer++;

                        if (this.enginePointer == this.engineSchedule.length)
                            this.enginePointer=0;

					}
                    else if (key.isWritable()) {

                        SelectableChannel channel = key.channel();
                        Engine engine = (Engine)key.attachment();


                        // attach the engine the selection key for later.
                        if (engine == null) {
                            for (int i = 0; i < this.engineSchedule.length; i++) {
                                if (this.engineSchedule[i].getEngineChannelQueue().isThisWriteChannel(channel)) {
                                    engine = this.engineSchedule[i];
                                    key.attach(engine);
                                }
                            }
                        }

                        engine.getEngineChannelQueue().sendObject(null);

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
