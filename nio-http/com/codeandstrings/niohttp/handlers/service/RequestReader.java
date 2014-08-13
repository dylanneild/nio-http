package com.codeandstrings.niohttp.handlers.service;

import com.codeandstrings.niohttp.request.Request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;

public class RequestReader {

    private Pipe.SourceChannel channel;
    private ByteBuffer currentSizeBuffer;
    private ByteBuffer currentRequestBuffer;

    public RequestReader(Pipe.SourceChannel channel) {
        this.channel = channel;
        this.currentSizeBuffer = null;
        this.currentRequestBuffer = null;
    }

    private boolean executeRequestReadRequestSize() throws IOException {

        // read the size buffer
        if (this.currentSizeBuffer == null) {

            this.currentSizeBuffer = ByteBuffer.allocate(Integer.SIZE / 8);
            this.channel.read(this.currentSizeBuffer);

            if (this.currentSizeBuffer.hasRemaining()) {
                return false;
            } else {
                this.currentSizeBuffer.flip();
                return true;
            }

        } else if (this.currentSizeBuffer.hasRemaining()) {

            this.channel.read(this.currentSizeBuffer);

            if (this.currentSizeBuffer.hasRemaining()) {
                return false;
            } else {
                this.currentSizeBuffer.flip();
                return true;
            }

        }

        this.currentSizeBuffer.rewind();

        return true;

    }

    private boolean executeRequestReadRequest(int size) throws IOException {

        // read the request buffer
        if (this.currentRequestBuffer == null) {

            this.currentRequestBuffer = ByteBuffer.allocate(size);
            this.channel.read(this.currentRequestBuffer);

            if (this.currentRequestBuffer.hasRemaining()) {
                return false;
            } else {
                this.currentRequestBuffer.flip();
                return true;
            }

        } else if (this.currentRequestBuffer.hasRemaining()) {

            this.channel.read(this.currentRequestBuffer);

            if (this.currentRequestBuffer.hasRemaining()) {
                return false;
            } else {
                this.currentRequestBuffer.flip();
                return true;
            }

        }

        this.currentRequestBuffer.rewind();

        return true;

    }

    /**
     * <p>Read a Request object form the handler channel.</p>
     *
     * <p>This will either return a Request object or null. If null is returned it means that the full
     * request object could not be read from the channel. This method should then be called again on the
     * next OP_READ selector event in order to attempt to read the remainder of the request.</p>
     *
     * @return A Request object, or null
     * @throws IOException On an IO Error
     * @throws ClassNotFoundException If there is a class cast issue
     */
    public Request readRequestFromChannel() throws IOException, ClassNotFoundException {

        if (!executeRequestReadRequestSize()) {
            return null;
        }

        int requestBufferSize = this.currentSizeBuffer.getInt();

        if (!executeRequestReadRequest(requestBufferSize)) {
            return null;
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(currentRequestBuffer.array());
        ObjectInputStream ois = new ObjectInputStream(bais);

        Request r = (Request)ois.readObject();

        ois.close();

        this.currentSizeBuffer = null;
        this.currentRequestBuffer = null;

        return r;

    }

}
