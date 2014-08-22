package com.codeandstrings.niohttp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.LinkedList;

import com.codeandstrings.niohttp.data.Parameters;
import com.codeandstrings.niohttp.exceptions.InvalidHandlerException;

public class Server implements Runnable {

	private Parameters parameters;
	private InetSocketAddress socketAddress;
	private ServerSocketChannel serverSocketChannel;
    private LinkedList<Engine> engineSchedule;
    private int concurrency;
    private ByteBuffer singleByteNotification;

	public Server(int concurrency) {

        this.parameters = Parameters.getDefaultParameters();
        this.engineSchedule = new LinkedList<Engine>();
        this.concurrency = concurrency;
        this.singleByteNotification = ByteBuffer.allocateDirect(1);
        this.singleByteNotification.put((byte)0x1);

        Thread.currentThread().setName("NIO-HTTP Server Thread");

        try {
            for (int i = 0; i < concurrency; i++) {
                engineSchedule.add(new Engine());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

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

	public void addRequestHandler(String path, Class handler) throws InvalidHandlerException {
        for (Engine engine : this.engineSchedule) {
            engine.addRequestHandler(path, handler);
        }
	}

	private void configureServerSocketChannel() throws IOException {
		this.serverSocketChannel = ServerSocketChannel.open();
		this.serverSocketChannel.configureBlocking(false);
		this.serverSocketChannel.bind(this.socketAddress, 2048);
	}

	@Override
	public void run() {

        for (Engine engine : this.engineSchedule) {
            engine.start();
        }

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
						 * Accept a connection register it as non-blocking and
						 * part of the master selector's selection chain (for
						 * OP_READ functions).
						 */
						SocketChannel connection = ((ServerSocketChannel) key.channel()).accept();
                        Engine nextEngine = this.engineSchedule.poll();
                        nextEngine.getSocketQueue().add(connection);

                        Pipe.SinkChannel engineChannel = nextEngine.getEngineNotificationChannel();

                        this.singleByteNotification.flip();

                        if (engineChannel.write(singleByteNotification) == 0) {
                            engineChannel.register(selector, SelectionKey.OP_WRITE);
                        }

                        this.engineSchedule.add(nextEngine);
					}
                    else if (key.isWritable()) {
                        Pipe.SinkChannel engineChannel = (Pipe.SinkChannel)key.channel();
                        this.singleByteNotification.flip();

                        if (engineChannel.write(singleByteNotification) == 1) {
                            engineChannel.register(selector, 0);
                        }
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
