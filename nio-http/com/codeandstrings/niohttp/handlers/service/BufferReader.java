package com.codeandstrings.niohttp.handlers.service;

import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.BufferContainer;
import com.codeandstrings.niohttp.response.BufferContainerHeader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;

public class BufferReader {

    private Pipe.SourceChannel channel;
    private ByteBuffer currentSizeBuffer;
    private ByteBuffer currentHeaderBuffer;
    private ByteBuffer currentBuffer;

    public BufferReader(Pipe.SourceChannel channel) {
        this.channel = channel;
        this.currentSizeBuffer = null;
        this.currentHeaderBuffer = null;
        this.currentBuffer = null;
    }

    private boolean executeBufferReadSize() throws IOException {

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

    private boolean executeBufferReadHeader(int size) throws IOException {

        // read the request buffer
        if (this.currentHeaderBuffer == null) {

            this.currentHeaderBuffer = ByteBuffer.allocate(size);
            this.channel.read(this.currentHeaderBuffer);

            if (this.currentHeaderBuffer.hasRemaining()) {
                return false;
            } else {
                this.currentHeaderBuffer.flip();
                return true;
            }

        } else if (this.currentHeaderBuffer.hasRemaining()) {

            this.channel.read(this.currentHeaderBuffer);

            if (this.currentHeaderBuffer.hasRemaining()) {
                return false;
            } else {
                this.currentHeaderBuffer.flip();
                return true;
            }

        }

        this.currentHeaderBuffer.rewind();

        return true;

    }

    private boolean executeBufferRead(int size) throws IOException {

        // read the request buffer
        if (this.currentBuffer == null) {

            this.currentBuffer = ByteBuffer.allocate(size);
            this.channel.read(this.currentBuffer);

            if (this.currentBuffer.hasRemaining()) {
                return false;
            } else {
                this.currentBuffer.flip();
                return true;
            }

        } else if (this.currentBuffer.hasRemaining()) {

            this.channel.read(this.currentBuffer);

            if (this.currentBuffer.hasRemaining()) {
                return false;
            } else {
                this.currentBuffer.flip();
                return true;
            }

        }

        this.currentBuffer.rewind();

        return true;

    }

    public BufferContainer readBufferFromChannel() throws IOException, ClassNotFoundException {

        if (!executeBufferReadSize()) {
            return null;
        }

        int requestBufferSize = this.currentSizeBuffer.getInt();

        if (!executeBufferReadHeader(requestBufferSize)) {
            return null;
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(currentHeaderBuffer.array());
        ObjectInputStream ois = new ObjectInputStream(bais);

        BufferContainerHeader header = (BufferContainerHeader)ois.readObject();

        ois.close();

        if (!executeBufferRead(header.getBufferSize())) {
            return null;
        }

        BufferContainer r = new BufferContainer(header, this.currentBuffer);

        this.currentSizeBuffer = null;
        this.currentHeaderBuffer = null;
        this.currentBuffer = null;

        return r;

    }

}
