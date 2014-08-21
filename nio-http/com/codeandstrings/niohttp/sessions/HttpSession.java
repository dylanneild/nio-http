package com.codeandstrings.niohttp.sessions;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;

import com.codeandstrings.niohttp.data.Parameters;
import com.codeandstrings.niohttp.exceptions.http.HttpException;
import com.codeandstrings.niohttp.exceptions.http.RequestEntityTooLargeException;
import com.codeandstrings.niohttp.exceptions.tcp.CloseConnectionException;
import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.request.RequestBodyFactory;
import com.codeandstrings.niohttp.request.RequestHeaderFactory;
import com.codeandstrings.niohttp.response.ResponseContent;

class HttpSession$Line {

    public final String line;
    public final int start;
    public final int nextStart;

    public HttpSession$Line(String line, int start, int nextStart) {
        this.line = line;
        this.start = start;
        this.nextStart = nextStart;
    }

    @Override
    public String toString() {
        return "HttpSession$Line [line=" + line + ", start=" + start
                + ", nextStart=" + nextStart + "]";
    }

}

public class HttpSession extends Session {

    private byte[] requestHeaderData;
    private int requestHeaderMarker;
    private ArrayList<HttpSession$Line> requestHeaderLines;
    private int lastHeaderByteLocation;
    private ByteBuffer readBuffer;
    private RequestHeaderFactory headerFactory;
    private boolean bodyReadBegun;
    private RequestBodyFactory bodyFactory;

    /* Write Queue */
    private ByteBuffer writeBuffer;
    private boolean writeBufferLastPacket;
    private Request writeRequest;

    /*
     * Response Management
     */
    private ArrayList<Request> requestQueue;
    private ArrayList<ResponseContent> outputQueue;

    /**
     * Constructor for a new HTTP session.
     *
     * @param channel  The TCP this session is operating against
     * @param selector The NIO selector this channel interacts with.
     */
    public HttpSession(SocketChannel channel, Selector selector, Parameters parameters) {

        super(channel, selector, parameters);

        this.readBuffer = ByteBuffer.allocate(128);
        this.outputQueue = new ArrayList<ResponseContent>();
        this.requestQueue = new ArrayList<Request>();

        this.reset();
    }

    public void reset() {
        this.requestHeaderData = new byte[maxRequestSize];
        this.requestHeaderMarker = 0;
        this.requestHeaderLines = new ArrayList<HttpSession$Line>();
        this.lastHeaderByteLocation = 0;
        this.headerFactory = new RequestHeaderFactory();
        this.bodyReadBegun = false;
        this.bodyFactory = new RequestBodyFactory();
        this.readBuffer.clear();
    }

    public void queueBuffer(ResponseContent container) throws IOException {
        this.outputQueue.add(container);
        this.setSelectionRequest(true);
    }

    public SocketChannel getChannel() {
        return this.channel;
    }

    private Request generateAndHandleRequest() throws IOException {

        InetSocketAddress remote = (InetSocketAddress) this.channel
                .getRemoteAddress();

        Request r = Request.generateRequest(this.getSessionId(), this.getNextRequestId(),
                remote.getHostString(), remote.getPort(), headerFactory.build(),
                bodyFactory.build(), this.parameters);

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
        for (HttpSession$Line sessionLine : this.requestHeaderLines) {

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

                this.requestHeaderLines.add(new HttpSession$Line(line, this.lastHeaderByteLocation, i + 1));
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

    private final void clearWriteOperations() {
        this.writeRequest = null;
        this.writeBuffer = null;
        this.writeBufferLastPacket = false;
    }

    private final boolean hasWriteEventQueued() {
        if (this.writeBuffer != null) {
            if (this.writeBuffer.hasRemaining()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private final void socketResponseConcluded() throws CloseConnectionException {
        boolean shouldClose = true;

        if (this.writeBufferLastPacket) {
            if (this.writeRequest != null && this.writeRequest.isKeepAlive()) {
                shouldClose = false;
            }
        }

        this.clearWriteOperations();

        if (shouldClose) {
            throw new CloseConnectionException();
        }
    }

    private final void socketWriteEventExecute() throws IOException, CloseConnectionException {

        if (!this.hasWriteEventQueued()) {
            this.socketResponseConcluded();
        }
        else {
            this.channel.write(this.writeBuffer);

            if (!this.writeBuffer.hasRemaining()) {
                this.clearWriteOperations();
            }
        }

    }

    private final void covertResponseContentToQueue(ResponseContent responseContent) {
        this.writeBuffer = ByteBuffer.wrap(responseContent.getBuffer());
        this.writeBufferLastPacket = responseContent.isLastBufferForRequest();
    }

    private final void queueNextWriteEvent() throws ClosedChannelException {

        if (this.outputQueue.size() == 0) {
            this.setSelectionRequest(false);
            return;
        }

        if (this.requestQueue.size() > 0) {
            this.writeRequest = this.requestQueue.remove(0);
        }

        if (this.writeRequest == null) {
            this.covertResponseContentToQueue(this.outputQueue.remove(0));
            return;
        } else {
            Iterator<ResponseContent> iterator = this.outputQueue.iterator();

            while (iterator.hasNext()) {
                ResponseContent responseContent = iterator.next();

                if (responseContent.getRequestId() == this.writeRequest.getRequestId()) {
                    this.covertResponseContentToQueue(responseContent);
                    iterator.remove();
                    return;
                }
            }
        }

        if (!this.hasWriteEventQueued()) {
            this.setSelectionRequest(false);
        }

    }

    public void socketWriteEvent() throws IOException, CloseConnectionException {
        if (this.hasWriteEventQueued()) {
            this.socketWriteEventExecute();
        } else {
            queueNextWriteEvent();
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
