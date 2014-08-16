package com.codeandstrings.niohttp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import com.codeandstrings.niohttp.data.IdealBlockSize;
import com.codeandstrings.niohttp.data.Parameters;
import com.codeandstrings.niohttp.exceptions.http.HttpException;
import com.codeandstrings.niohttp.exceptions.http.RequestEntityTooLargeException;
import com.codeandstrings.niohttp.exceptions.tcp.CloseConnectionException;
import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.request.RequestBodyFactory;
import com.codeandstrings.niohttp.request.RequestHeaderFactory;
import com.codeandstrings.niohttp.response.BufferContainer;

class Session$Line {

    public final String line;
    public final int start;
    public final int nextStart;

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
      Session ID Management
     */
    private static long lastSessionId = 0;
    private long sessionId;
    private long nextRequestId;

    /*
     * Our channel and selector
     */
    private SocketChannel channel;
    private Selector selector;
    private Parameters parameters;

    /*
     * Request acceptance data
     */
    private int maxRequestSize;
    private byte[] requestHeaderData;
    private int requestHeaderMarker;
    private ArrayList<Session$Line> requestHeaderLines;
    private int lastHeaderByteLocation;
    private ByteBuffer readBuffer;
    private RequestHeaderFactory headerFactory;
    private boolean bodyReadBegun;
    private RequestBodyFactory bodyFactory;

    /*
     * Response Management
     */
    private ArrayList<Request> requestQueue;
    private ArrayList<BufferContainer> outputQueue;

    /**
     * Constructor for a new HTTP session.
     *
     * @param channel  The TCP this session is operating against
     * @param selector The NIO selector this channel interacts with.
     */
    public Session(SocketChannel channel, Selector selector, Parameters parameters) {

        this.sessionId = Session.lastSessionId;
        Session.lastSessionId++;

        this.nextRequestId = 0;

        this.channel = channel;
        this.selector = selector;
        this.maxRequestSize = IdealBlockSize.VALUE;
        this.readBuffer = ByteBuffer.allocate(128);
        this.outputQueue = new ArrayList<BufferContainer>();
        this.requestQueue = new ArrayList<Request>();
        this.parameters = parameters;

        this.reset();
    }

    public void reset() {
        this.requestHeaderData = new byte[maxRequestSize];
        this.requestHeaderMarker = 0;
        this.requestHeaderLines = new ArrayList<Session$Line>();
        this.lastHeaderByteLocation = 0;
        this.headerFactory = new RequestHeaderFactory();
        this.bodyReadBegun = false;
        this.bodyFactory = new RequestBodyFactory();
        this.readBuffer.clear();
    }

    public long getSessionId() {
        return sessionId;
    }

    public long getNextRequestId() {
        long r = this.nextRequestId;
        this.nextRequestId++;
        return r;
    }

    public void queueBuffer(BufferContainer container) throws IOException {
        this.outputQueue.add(container);
        this.setSelectionRequest(true);
    }

    public SocketChannel getChannel() {
        return this.channel;
    }

    private Request generateAndHandleRequest() throws IOException {

        InetSocketAddress remote = (InetSocketAddress) this.channel
                .getRemoteAddress();

        Request r = Request.generateRequest(this.sessionId, this.getNextRequestId(),
                remote.getHostString(), remote.getPort(), headerFactory.build(),
                bodyFactory.build());

        return r;

    }

    private void copyExistingBytesToBody(int startPosition) {
        this.bodyFactory.addBytes(this.requestHeaderData, startPosition, this.requestHeaderMarker - startPosition);
    }

    private Request storeAndReturnRequest(Request request) {
        this.requestQueue.add(request);
        return request;
    }

    public void removeRequest(Request request) {
        this.requestQueue.remove(request);
    }

    private Request analyzeForHeader() throws HttpException, IOException {

        // likely won't ever happen anyways, but just in case
        // don't do this - we're already in body mode
        if (this.bodyReadBegun) {
            return null;
        }

        // there is nothing to analyze
        if (this.requestHeaderLines.size() == 0)
            return null;

        // reset our factory
        this.headerFactory.reset();

        // walk the received header lines.
        for (Session$Line sessionLine : this.requestHeaderLines) {

            headerFactory.addLine(sessionLine.line);

            if (headerFactory.shouldBuildRequestHeader()) {

                int requestBodySize = headerFactory.shouldExpectBody();

                if (requestBodySize > this.parameters.getMaximumPostSize()) {

                    throw new RequestEntityTooLargeException(requestBodySize);

                } else if (requestBodySize != -1) {

                    // we have a request body; attempt to grab it from
                    // existing request information; otherwise we'll just defer
                    // off to the next read event to try to finish the request.

                    this.bodyFactory.resize(requestBodySize);
                    this.bodyReadBegun = true;
                    this.copyExistingBytesToBody(sessionLine.nextStart);

                    if (this.bodyFactory.isFull()) {
                        return this.storeAndReturnRequest(this.generateAndHandleRequest());
                    }

                }
                else {
                    // there is no request body; go
                    return this.storeAndReturnRequest(this.generateAndHandleRequest());
                }

            }

        }

        // if we're here, we don't have enough of a request yet
        return null;

    }

    private void extractLines() {

        for (int i = this.lastHeaderByteLocation; i < this.requestHeaderMarker; i++) {

            if (i == 0) {
                continue;
            }

            if ((this.requestHeaderData[i] == 10) && (this.requestHeaderData[i - 1] == 13)) {

                String line = null;

                if ((i - this.lastHeaderByteLocation - 1) == 0) {
                    line = new String();
                } else {
                    line = new String(this.requestHeaderData, this.lastHeaderByteLocation, i - this.lastHeaderByteLocation - 1);
                }

                this.requestHeaderLines.add(new Session$Line(line, this.lastHeaderByteLocation, i + 1));
                this.lastHeaderByteLocation = (i + 1);
            }

        }

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

    public void socketWriteEvent() throws IOException, CloseConnectionException {

        if (this.outputQueue.size() == 0) {
            this.setSelectionRequest(false);
            return;
        }

        Request nextRequest = null;
        BufferContainer nextContainer = null;

        if (this.requestQueue.size() > 0) {
            nextRequest = this.requestQueue.get(0);
        }

        if (nextRequest == null) {
            // the output queue has unassigned buffers
            // these are errors and can just go in order.
            nextContainer = this.outputQueue.get(0);
        } else {
            for (BufferContainer bufferContainer : this.outputQueue) {
                if (bufferContainer.getRequestId() == nextRequest.getRequestId()) {
                    nextContainer = bufferContainer;
                    break;
                }
            }
        }

        if (nextContainer == null) {
            // next time a packet gets written it will reset this
            this.setSelectionRequest(false);
            return;
        }

        ByteBuffer bufferToWrite = nextContainer.getBuffer();
        this.channel.write(bufferToWrite);

        if (bufferToWrite.hasRemaining()) {
            bufferToWrite.compact();
        } else {

            this.outputQueue.remove(nextContainer);

            if (nextContainer.isLastBufferForRequest()) {

                // we're done with this response

                if (nextRequest == null || !nextRequest.isKeepAlive()) {

                    // shut it down - remove the request and fire a CloseConnectionException
                    if (nextRequest != null) {
                        this.requestQueue.remove(0);
                    }

                    throw new CloseConnectionException();

                }
                else {
                    // eventually we want to be able to support chunked
                    // for now, we simply send the response
                    if (nextRequest != null) {
                        this.requestQueue.remove(0);
                    }
                }

            }

            if (this.outputQueue.size() == 0) {
                this.setSelectionRequest(false);
            }
        }

    }

    public Request socketReadEvent() throws IOException, CloseConnectionException, HttpException {

        try {
            if (!this.channel.isConnected() || !this.channel.isOpen()) {
                return null;
            }

            int bytesRead = this.channel.read(this.readBuffer);

            if (bytesRead == -1) {
                throw new CloseConnectionException();
            } else {

                byte[] bytes = new byte[bytesRead];

                this.readBuffer.flip();
                this.readBuffer.get(bytes);

                if (this.bodyReadBegun) {

                    this.bodyFactory.addBytes(bytes);

                    if (this.bodyFactory.isFull()) {
                        return this.generateAndHandleRequest();
                    } else {
                        return null;
                    }

                } else {

                    for (int i = 0; i < bytesRead; i++) {

                        if (this.requestHeaderMarker >= (this.maxRequestSize - 1)) {
                            // we won't receive header blocks that are much bigger than
                            // the maximum requst size, which is generally 8192 bytes.
                            // Once the header has been setup the request body can
                            // reach the maximum post in size without issue.
                            //
                            throw new RequestEntityTooLargeException();
                        }

                        this.requestHeaderData[this.requestHeaderMarker] = bytes[i];
                        this.requestHeaderMarker++;

                    }

                    // header has been ingested
                    this.extractLines();

                    // return the request if one is there
                    return this.analyzeForHeader();

                }
            }
        } catch (IOException e) {
            throw new CloseConnectionException(e);
        }

    }


}
