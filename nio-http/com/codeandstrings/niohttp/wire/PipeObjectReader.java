package com.codeandstrings.niohttp.wire;

import com.codeandstrings.niohttp.request.Request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;

public class PipeObjectReader {

    private Pipe.SourceChannel channel;
    private ByteBuffer currentSizeBuffer;
    private ByteBuffer currentObjectBuffer;
    private boolean currentSizeDone;
    private boolean currentObjectDone;

    private void reset() {
        this.currentSizeBuffer = null;
        this.currentObjectBuffer = null;
        this.currentSizeDone = false;
        this.currentObjectDone = false;
    }

    public PipeObjectReader(Pipe.SourceChannel channel) {
        this.channel = channel;
        this.reset();
    }

    private boolean executeReadSize() throws IOException {

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

    private boolean executeReadObject(int size) throws IOException {

        // read the request buffer
        if (!this.currentObjectDone) {
            if (this.currentObjectBuffer == null) {

                this.currentObjectBuffer = ByteBuffer.allocate(size);
                this.channel.read(this.currentObjectBuffer);

                if (this.currentObjectBuffer.hasRemaining()) {
                    return false;
                } else {
                    this.currentObjectBuffer.flip();
                    this.currentObjectDone = true;
                    return true;
                }

            } else if (this.currentObjectBuffer.hasRemaining()) {

                this.channel.read(this.currentObjectBuffer);

                if (this.currentObjectBuffer.hasRemaining()) {
                    return false;
                } else {
                    this.currentObjectBuffer.flip();
                    this.currentObjectDone = true;
                    return true;
                }

            }
        }

        this.currentObjectBuffer.rewind();

        return true;

    }

    private Object getObject() throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(currentObjectBuffer.array());
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        }
        catch (IOException e) {
            throw e;
        }
        catch (ClassNotFoundException e) {
            throw e;
        }
    }

    public Object readObjectFromChannel() throws IOException, ClassNotFoundException {

        if (!executeReadSize()) {
            return null;
        }

        int requestBufferSize = this.currentSizeBuffer.getInt();

        if (!executeReadObject(requestBufferSize)) {
            return null;
        }

        Object r = getObject();

        this.reset();

        return r;

    }

}
