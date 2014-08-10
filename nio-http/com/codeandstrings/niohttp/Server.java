package com.codeandstrings.niohttp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import com.codeandstrings.niohttp.data.Parameters;
import com.codeandstrings.niohttp.handlers.RequestHandler;

public class Server implements Runnable {

	private Parameters parameters;
	private InetSocketAddress socketAddress;
	private ServerSocketChannel serverSocketChannel;
	private RequestHandler requestHandler;

	public Server() {
		this.parameters = Parameters.getDefaultParameters();
	}
	
	public Parameters getParameters() {
		return parameters;
	}

	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}

	private void configureSocketAddress() {
		this.socketAddress = new InetSocketAddress(this.parameters.getPort());
	}
	
	public void setRequestHandler(RequestHandler requestHandler) {
		this.requestHandler = requestHandler;
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
						 * Accept a connection register it as non-blocking and part of
						 * the master selector's selection chain (for OP_READ functions).
						 */
						SocketChannel connection = ((ServerSocketChannel) key.channel()).accept();
						connection.configureBlocking(false);
						connection.register(selector, SelectionKey.OP_READ);

						System.err.println("Connection opened " + connection);

					} else if (key.isReadable()) {

						SelectableChannel channel = key.channel();
						Session session = (Session) key.attachment();
						
						if (channel instanceof SocketChannel) {
							if (session == null) {
								/*
								 * The selector has triggered because a socket has generated a read
								 * event and there is presently no session available to handle it.
								 * Make a new one and ask it to handle the session.
								 */
								session = new Session((SocketChannel)channel, selector, this.requestHandler);
								key.attach(session);
							}
							
							session.socketReadEvent();
						}
						else {
							// this is unimplemented and would be another stream read event, indicating 
							// that some form of response stream data is ready to be read.
						}
					} 
					else if (key.isWritable()) {
						
						// we don't care if this is anything other than a socketchannel
						// because we only write to socket channels (server does no file manipulation)
						
						// if this triggers NullPointerException something horribly wrong has happened.
						
						Session session = (Session) key.attachment();
						session.socketWriteEvent();
						
					}

					ki.remove();

				}

			}

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

}
