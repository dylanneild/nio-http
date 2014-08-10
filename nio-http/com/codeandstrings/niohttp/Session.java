package com.codeandstrings.niohttp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import com.codeandstrings.niohttp.exceptions.http.HttpException;
import com.codeandstrings.niohttp.exceptions.http.InternalServerErrorException;
import com.codeandstrings.niohttp.exceptions.http.RequestEntityTooLargeException;

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

	private SocketChannel channel;
	private int maxRequestSize;
	private byte[] requestHeaderData;
	private int requestHeaderMarker;
	private ArrayList<Session$Line> requestHeaderLines;
	private int lastHeaderByteLocation;
	private ByteBuffer readBuffer;

	public Session(SocketChannel channel) {
		this.channel = channel;
		this.maxRequestSize = IdealBlockSize.VALUE;		
		this.readBuffer = ByteBuffer.allocate(128);
		this.reset();
	}	
	
	private void reset() {
		this.requestHeaderData = new byte[maxRequestSize];
		this.requestHeaderMarker = 0;
		this.requestHeaderLines = new ArrayList<Session$Line>();
		this.lastHeaderByteLocation = 0;
	}

	private void analyze() throws HttpException {

		if (this.requestHeaderLines.size() == 0)
			return;

		RequestHeaderFactory headerFactory = new RequestHeaderFactory();

		for (Session$Line sessionLine : this.requestHeaderLines) {
			headerFactory.addLine(sessionLine.line);
		}
		
		if (headerFactory.isHeadUseable()) {
			RequestHeader header = headerFactory.build();
			System.out.println(header.toString());
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
							this.lastHeaderByteLocation, i - this.lastHeaderByteLocation - 1);
				}

				this.requestHeaderLines.add(new Session$Line(line,
						this.lastHeaderByteLocation, i + 1));

				this.lastHeaderByteLocation = (i + 1);
			}

		}

	}

	public Request readEvent() throws HttpException {

		try {
			this.readBuffer.clear();			
			int bytesRead = this.channel.read(this.readBuffer);
	
			if (bytesRead == -1) {
	
				System.err.println("Connection closed " + this.channel);
				this.channel.close();
	
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
		}
		catch (IOException e) {
			throw new InternalServerErrorException();
		}
				
		// return any request that's been generated so far.
		// if a request exists or has been deemed a failure, fcall reset first
		
		
		return null;

	}

}
