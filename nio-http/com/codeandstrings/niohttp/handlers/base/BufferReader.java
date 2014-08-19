package com.codeandstrings.niohttp.handlers.base;

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

    private boolean currentSizeDone;
    private boolean currentHeaderDone;
    private boolean currentBufferDone;

    private void reset() {
        this.currentSizeBuffer = null;
        this.currentHeaderBuffer = null;
        this.currentBuffer = null;
        this.currentSizeDone = false;
        this.currentHeaderDone = false;
        this.currentBufferDone = false;
    }
    
    public BufferReader(Pipe.SourceChannel channel) {
        this.channel = channel;
        this.reset();
    }

    private boolean executeBufferReadSize() throws IOException {

        // read the size buffer
        if (!this.currentSizeDone) {
            if (this.currentSizeBuffer == null) {

                this.currentSizeBuffer = ByteBuffer.allocate(Integer.SIZE / 8);
                this.channel.read(this.currentSizeBuffer);

                if (this.currentSizeBuffer.hasRemaining()) {
                    return false;
                } else {
                    this.currentSizeBuffer.flip();
                    this.currentSizeDone = true;
                    return true;
                }

            } else if (this.currentSizeBuffer.hasRemaining()) {

                this.channel.read(this.currentSizeBuffer);

                if (this.currentSizeBuffer.hasRemaining()) {
                    return false;
                } else {
                    this.currentSizeBuffer.flip();
                    this.currentSizeDone = true;
                    return true;
                }

            }
        }

        this.currentSizeBuffer.rewind();

        return true;

    }

    private boolean executeBufferReadHeader(int size) throws IOException {

        // read the request buffer
        if (!this.currentHeaderDone) {
            if (this.currentHeaderBuffer == null) {

                this.currentHeaderBuffer = ByteBuffer.allocate(size);
                this.channel.read(this.currentHeaderBuffer);

                if (this.currentHeaderBuffer.hasRemaining()) {
                    return false;
                } else {
                    this.currentHeaderBuffer.flip();
                    this.currentHeaderDone = true;
                    return true;
                }

            } else if (this.currentHeaderBuffer.hasRemaining()) {

                this.channel.read(this.currentHeaderBuffer);

                if (this.currentHeaderBuffer.hasRemaining()) {
                    return false;
                } else {
                    this.currentHeaderBuffer.flip();
                    this.currentHeaderDone = true;
                    return true;
                }

            }
        }

        this.currentHeaderBuffer.rewind();

        return true;

    }

    private boolean executeBufferRead(int size) throws IOException {

        // read the actual buffer
        if (!this.currentBufferDone) {
            if (this.currentBuffer == null) {

                this.currentBuffer = ByteBuffer.allocate(size);
                this.channel.read(this.currentBuffer);

                if (this.currentBuffer.hasRemaining()) {
                    return false;
                } else {
                    this.currentBuffer.flip();
                    this.currentBufferDone = true;
                    return true;
                }

            } else if (this.currentBuffer.hasRemaining()) {

                this.channel.read(this.currentBuffer);

                if (this.currentBuffer.hasRemaining()) {
                    return false;
                } else {
                    this.currentBuffer.flip();
                    this.currentBufferDone = true;
                    return true;
                }

            }
        }

        this.currentBuffer.rewind();

        return true;

    }

    private BufferContainerHeader getContainerHeader() throws IOException, ClassNotFoundException {
 
    	ByteArrayInputStream bais = new ByteArrayInputStream(currentHeaderBuffer.array());
        ObjectInputStream ois = new ObjectInputStream(bais);

        BufferContainerHeader header = (BufferContainerHeader)ois.readObject();

        ois.close();
        
        return header;
          
    }
    
    public BufferContainer readBufferFromChannel() throws IOException, ClassNotFoundException {

        if (!executeBufferReadSize()) {
            return null;
        }

        int requestBufferSize = this.currentSizeBuffer.getInt();

        if (!executeBufferReadHeader(requestBufferSize)) {
            return null;
        }

        BufferContainerHeader header = getContainerHeader();

        if (!executeBufferRead(header.getBufferSize())) {
            return null;
        }

        BufferContainer r = new BufferContainer(header, this.currentBuffer);

        this.reset();

        return r;

    }

}
