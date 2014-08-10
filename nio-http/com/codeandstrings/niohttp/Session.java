package com.codeandstrings.niohttp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import com.codeandstrings.niohttp.exceptions.http.HttpException;
import com.codeandstrings.niohttp.exceptions.http.InternalServerErrorException;
import com.codeandstrings.niohttp.exceptions.http.RequestEntityTooLargeException;
import com.codeandstrings.niohttp.handlers.RequestHandler;
import com.codeandstrings.niohttp.response.BufferContainer;
import com.codeandstrings.niohttp.response.ExceptionResponseFactory;
import com.codeandstrings.niohttp.response.Response;

class Session$Line {

	public String line;
	public int start;
	public int nextStart;

	public Session$Line(String line, int start, int nextStart) {
		this.line = line;
		this.start = start;
		this.nextStart = nextStart;
	}

	@Override
	public String toString() {
		return "Session$Line [line=" + line + ", start=" + start
				+ ", nextStart=" + nextStart + "]";
	}

}

public class Session {

	/*
	 * Our channel and selector
	 */
	private SocketChannel channel;
	private Selector selector;

	/*
	 * Request acceptance data
	 */
	private int maxRequestSize;
	private byte[] requestHeaderData;
	private int requestHeaderMarker;
	private ArrayList<Session$Line> requestHeaderLines;
	private int lastHeaderByteLocation;
	private ByteBuffer readBuffer;
	private RequestHandler requestHandler;

	/*
	 * Response Management
	 */
	private ArrayList<BufferContainer> outputQueue;

	/**
	 * Constructor.
	 * 
	 * @param channel
	 * @param selector
	 */
	public Session(SocketChannel channel, Selector selector, RequestHandler handler) {
		this.channel = channel;
		this.selector = selector;
		this.maxRequestSize = IdealBlockSize.VALUE;
		this.readBuffer = ByteBuffer.allocate(128);
		this.outputQueue = new ArrayList<BufferContainer>();
		this.requestHandler = handler;
		this.reset();
	}

	private void reset() {
		this.requestHeaderData = new byte[maxRequestSize];
		this.requestHeaderMarker = 0;
		this.requestHeaderLines = new ArrayList<Session$Line>();
		this.lastHeaderByteLocation = 0;
	}

	public SocketChannel getChannel() {
		return this.channel;
	}

	private void analyze() throws HttpException {

		if (this.requestHeaderLines.size() == 0)
			return;

		RequestHeaderFactory headerFactory = new RequestHeaderFactory();

		for (Session$Line sessionLine : this.requestHeaderLines) {
			headerFactory.addLine(sessionLine.line);
		}

	}

	private void extractLines() {

		for (int i = this.lastHeaderByteLocation; i < this.requestHeaderMarker; i++) {

			if (i == 0) {
				continue;
			}

			if (this.requestHeaderData[i] == 10
					&& this.requestHeaderData[i - 1] == 13) {

				String line = null;

				if ((i - this.lastHeaderByteLocation - 1) == 0) {
					line = new String();
				} else {
					line = new String(this.requestHeaderData,
							this.lastHeaderByteLocation, i
									- this.lastHeaderByteLocation - 1);
				}

				this.requestHeaderLines.add(new Session$Line(line,
						this.lastHeaderByteLocation, i + 1));

				this.lastHeaderByteLocation = (i + 1);
			}

		}

	}

	private void closeChannel() throws IOException {
	
		System.err.println("Connection closed " + this.channel);
		this.channel.close();

	}
	
	private void setSelectionRequest(boolean write)
			throws ClosedChannelException {

		int ops;

		if (write) {
			ops = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
		} else {
			ops = SelectionKey.OP_READ;
		}

		this.channel.register(this.selector, ops, this);
	}

	public void socketWriteEvent() throws IOException {

		boolean closedConnection = false;
		
		if (this.outputQueue.size() > 0) {
			
			BufferContainer container = this.outputQueue.remove(0);
			
			this.channel.write(container.getBuffer());
			
			// kill the connection?
			if (container.isCloseOnTransmission()) {
				this.closeChannel();	
				closedConnection = true;
			}
		}
		
		if (this.outputQueue.size() == 0 && !closedConnection) {
			this.setSelectionRequest(false);
		}

	}

	private void generateResponseException(HttpException e)
			throws IOException {

		Response r = (new ExceptionResponseFactory(e)).create();
		this.outputQueue.add(new BufferContainer(r.getByteBuffer(), true));
		this.setSelectionRequest(true);
		this.socketWriteEvent();

	}

	public void socketReadEvent() throws IOException {

		try {
			this.readBuffer.clear();
			int bytesRead = this.channel.read(this.readBuffer);

			if (bytesRead == -1) {
				this.closeChannel();
			} else {

				byte[] bytes = new byte[bytesRead];

				this.readBuffer.flip();
				this.readBuffer.get(bytes);

				for (int i = 0; i < bytesRead; i++) {

					if (this.requestHeaderMarker >= (this.maxRequestSize - 1)) {
						throw new RequestEntityTooLargeException();
					}

					this.requestHeaderData[this.requestHeaderMarker] = bytes[i];
					this.requestHeaderMarker++;
				}

				// packet has been injested
				this.extractLines();
				this.analyze();

			}
		} catch (IOException e) {
			generateResponseException(new InternalServerErrorException());
		} catch (HttpException e) {
			generateResponseException(e);
		}

	}

}
