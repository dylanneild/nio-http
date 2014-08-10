package com.codeandstrings.niohttp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import com.codeandstrings.niohttp.exceptions.http.HttpException;
import com.codeandstrings.niohttp.handlers.RequestHandler;
import com.codeandstrings.niohttp.handlers.StringRequestHandler;
import com.codeandstrings.niohttp.response.ResponseFactory;

public class Server implements Runnable {

	private Parameters parameters;
	private InetSocketAddress socketAddress;
	private ServerSocketChannel serverSocketChannel;
	private RequestHandler requestHandler;

	public Parameters getParameters() {
		return parameters;
	}

	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}

	private void configureSocketAddress() throws Exception {
		this.socketAddress = new InetSocketAddress(this.parameters.getPort());
	}
	
	public void setRequestHandler(RequestHandler requestHandler) {
		this.requestHandler = requestHandler;
	}

	private void configureServerSocketChannel() throws Exception {
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

						SocketChannel connection = ((ServerSocketChannel) key.channel()).accept();
						connection.configureBlocking(false);
						connection.register(selector, SelectionKey.OP_READ);

						System.out.println("Connection opened " + connection);

					} else if (key.isReadable()) {

						SocketChannel channel = (SocketChannel)key.channel();
						Session session = (Session) key.attachment();
						
						if (session == null) {
							session = new Session(channel);
							key.attach(session);
						} 

						try {
							
							/* beyond not complete - just spits out text */
							Request request = session.readEvent();
							
							if (request != null) {
								String response = ((StringRequestHandler)this.requestHandler).handleRequest(request);
								ByteBuffer output = ByteBuffer.allocate(response.length());
								output.put(response.getBytes());
								session.getChannel().write(output);
								
							}
							
						} catch (HttpException e) {						
							e.printStackTrace();
							ResponseFactory.createResponse(e).write(channel);
							channel.close();
						}
						

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
