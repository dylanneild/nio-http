package com.codeandstrings.niohttp;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class Server implements Runnable {

	private Parameters parameters;
	private InetSocketAddress socketAddress;
	private ServerSocketChannel serverSocketChannel;

	public Parameters getParameters() {
		return parameters;
	}

	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}

	private void configureSocketAddress() throws Exception {
		this.socketAddress = new InetSocketAddress(this.parameters.getPort());
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

						Session session = (Session) key.attachment();
						
						if (session == null) {
							session = new Session((SocketChannel)key.channel());
							key.attach(session);
						} 

						try {
							Request request = session.readEvent();
						} catch (Exception e) {
							e.printStackTrace();
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
