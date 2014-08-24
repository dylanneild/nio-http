package com.codeandstrings.niohttp.sessions;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
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
import com.codeandstrings.niohttp.response.Response;
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
        return "HttpSession$Line{" +
                "line='" + line + '\'' +
                ", start=" + start +
                ", nextStart=" + nextStart +
                '}';
    }
}

public class HttpSession extends Session {

    /* Object Variables */
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
    private Response writeResponse;

    /**
     * Constructor for a new HTTP session.
     *
     * @param channel  The TCP this session is operating against
     * @param selector The NIO selector this channel interacts with.
     */
    public HttpSession(SocketChannel channel, Selector selector, Parameters parameters) {
        super(channel, selector, parameters);
        this.readBuffer = ByteBuffer.allocateDirect(1460);
        this.resetHeaderReads();
    }

    @Override
    public void resetHeaderReads() {
        this.requestHeaderData = new byte[maxRequestSize];
        this.requestHeaderMarker = 0;
        this.requestHeaderLines = new ArrayList<HttpSession$Line>();
        this.lastHeaderByteLocation = 0;
        this.headerFactory = new RequestHeaderFactory();
        this.bodyReadBegun = false;
        this.bodyFactory = new RequestBodyFactory();
        this.readBuffer.clear();
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

        if (request != null) {
            this.requestQueue.add(request);
            this.requestMap.put(request.getRequestId(), request);
        }

        return request;
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

        // resetHeaderReads our factory
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

    private final boolean socketWriteConditionalClose() {

        boolean shouldClose = true;

        if (this.writeBufferLastPacket) {
            if (this.writeRequest != null && this.writeRequest.isKeepAlive()) {
                shouldClose = false;
            }
        } else {
            shouldClose = false;
        }

        return shouldClose;

    }

    private final void socketWritePartialClear() {
        this.writeBuffer = null;
        this.writeBufferLastPacket = false;
    }

    private final void socketWriteFullClear() throws ClosedChannelException {

        this.socketWritePartialClear();

        if (this.writeResponse != null) {
            if (this.writeResponse.getRequestId() >= 0) {
                this.responseMap.remove(this.writeResponse.getRequestId());
            }
        }

        if (this.writeRequest != null) {
            this.requestMap.remove(this.writeRequest.getRequestId());
        }

        this.writeRequest = null;
        this.writeResponse = null;

        if (this.responseQueue.size() == 0) {
            this.setSelectionRequest(false);
        }

    }

    private final void socketWriteExecute() throws IOException, CloseConnectionException {

        // write the present buffer
        this.channel.write(this.writeBuffer);

        if (!this.writeBuffer.hasRemaining()) {

            if (this.socketWriteConditionalClose()) {
                this.socketWriteFullClear();
                throw new CloseConnectionException();
            } else {
                if (this.writeBufferLastPacket) {
                    this.socketWriteFullClear();
                } else {
                    this.socketWritePartialClear();
                }
            }

        }

    }

    private final boolean socketWriteReady() {
        if (this.writeBuffer == null) {
            return false;
        }
        else if (!this.writeBuffer.hasRemaining()) {
            return false;
        }
        else {
            return true;
        }
    }

    private final void socketWriteResponseToBuffer(Response r) {


        byte bytes[] = r.getByteRepresentation();

        if (bytes != null) {
            this.writeBuffer = ByteBuffer.wrap(bytes);

            if (!r.isBodyIncluded()) {
                this.writeBufferLastPacket = true;
            }
        }

    }

    private final boolean socketWriteQueueBadRequest() {
        if (this.requestQueue.size() == 0 && this.responseQueue.size() > 0) {
            this.writeResponse = this.responseQueue.poll();
            this.socketWriteResponseToBuffer(this.writeResponse);
            return true;
        } else {
            return false;
        }
    }

    private final void socketWriteQueueInitial() throws ClosedChannelException {

        this.writeRequest = this.requestQueue.peek();

        // if there is no present request, we're done
        if (this.writeRequest == null) {
            this.setSelectionRequest(false);
            return;
        }

        // get the response object - this will always be here because
        // you can't get this trigger without having a response object
        for (Iterator<Response> itr = this.responseQueue.iterator(); itr.hasNext(); ) {

            Response r = itr.next();

            if (r.getRequestId() == this.writeRequest.getRequestId()) {
                this.writeResponse = r;
                itr.remove();
                break;
            }

        }

        if (this.writeResponse == null) {
            this.setSelectionRequest(false);
            return;
        }

        this.writeRequest = this.requestQueue.poll();
        this.socketWriteResponseToBuffer(this.writeResponse);

    }

    private final void socketWriteConvertResponseContent(ResponseContent responseContent) {
        this.writeBufferLastPacket = responseContent.isLastBufferForRequest();
        this.writeBuffer = ByteBuffer.wrap(responseContent.getBuffer());
    }

    private final void socketWriteQueueAdditional() throws ClosedChannelException {

        Iterator<ResponseContent> iterator = this.contentQueue.iterator();

        while (iterator.hasNext()) {
            ResponseContent responseContent = iterator.next();
            if (responseContent.getRequestId() == this.writeRequest.getRequestId()) {
                this.socketWriteConvertResponseContent(responseContent);
                iterator.remove();
                break;
            }
        }

    }

    private final void socketWriteQueueNext() throws ClosedChannelException {
        if (this.writeRequest == null) {
            if (!this.socketWriteQueueBadRequest()) {
                this.socketWriteQueueInitial();
            }
        } else {
            this.socketWriteQueueAdditional();
        }
    }

    @Override
    public void socketWriteEvent() throws IOException, CloseConnectionException {
        if (this.socketWriteReady()) {
            this.socketWriteExecute();
        } else {
            this.socketWriteQueueNext();
        }
    }

    @Override
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
                        return this.storeAndReturnRequest(this.generateAndHandleRequest());
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
