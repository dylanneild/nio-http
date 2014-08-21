package com.codeandstrings.niohttp.wire;

import com.codeandstrings.niohttp.response.ResponseContent;
import com.codeandstrings.niohttp.response.ResponseContentHeader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;

public class ResponseContentReader {

    private Pipe.SourceChannel channel;

    private ByteBuffer currentSizeBuffer;
    private ByteBuffer currentBuffer;

    private boolean currentSizeDone;
    private boolean currentBufferDone;

    private void reset() {
        this.currentSizeBuffer = null;
        this.currentBuffer = null;
        this.currentSizeDone = false;
        this.currentBufferDone = false;
    }
    
    public ResponseContentReader(Pipe.SourceChannel channel) {
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

    private ResponseContent getContainer() throws IOException, ClassNotFoundException {

        try (ByteArrayInputStream bais = new ByteArrayInputStream(currentBuffer.array());
             ObjectInputStream ois = new ObjectInputStream(bais)) {

            return (ResponseContent)ois.readObject();

        }  catch (IOException e) {
            throw e;
        } catch (ClassNotFoundException e) {
            throw e;
        }

    }
    
    public ResponseContent readBufferFromChannel() throws IOException, ClassNotFoundException {

        if (!executeBufferReadSize()) {
            return null;
        }

        int requestBufferSize = this.currentSizeBuffer.getInt();

        if (!executeBufferRead(requestBufferSize)) {
            return null;
        }

        ResponseContent r = this.getContainer();

        this.reset();

        return r;

    }

}
