package com.codeandstrings.niohttp.handlers;

import com.codeandstrings.niohttp.response.BufferContainer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.ArrayList;

public class BufferWriter {

    private Pipe.SinkChannel channel;
    private ArrayList<BufferContainer> queue;
    private ByteBuffer currentSizeBuffer;
    private ByteBuffer currentHeaderBuffer;
    private ByteBuffer currentDataBuffer;

    public BufferWriter (Pipe.SinkChannel channel) {
        this.channel = channel;
        this.queue = new ArrayList<BufferContainer>();
    }

    public void sendBufferContainer(BufferContainer r) {
        this.queue.add(r);
    }

    private boolean queueNextBuffer() throws IOException {

        if (queue.size() == 0)
            return false;

        BufferContainer container = queue.remove(0);

        this.currentHeaderBuffer = container.getHeaderAsByteBuffer();
        this.currentSizeBuffer = ByteBuffer.allocate(Integer.SIZE / 8).putInt(currentHeaderBuffer.capacity());
        this.currentDataBuffer = container.getBuffer();

        this.currentHeaderBuffer.flip();
        this.currentSizeBuffer.flip();

        return true;

    }

    private boolean hasCurrentBuffer() {
        if (currentSizeBuffer != null)
            return true;
        else if (currentHeaderBuffer != null)
            return true;
        else if (currentDataBuffer != null)
            return true;
        else
            return false;
    }

    public boolean executeBufferWriteEvent() throws IOException {

        if (!hasCurrentBuffer()) {
            if (!queueNextBuffer()) {
                return false;
            }
        }

        if (this.currentSizeBuffer != null) {
            this.channel.write(this.currentSizeBuffer);
            if (this.currentSizeBuffer.hasRemaining()) {
                return true;
            } else {
                this.currentSizeBuffer = null;
            }
        }

        if (this.currentHeaderBuffer != null) {
            this.channel.write(this.currentHeaderBuffer);
            if (this.currentHeaderBuffer.hasRemaining()) {
                return true;
            } else {
                this.currentHeaderBuffer = null;
            }
        }

        if (this.currentDataBuffer != null) {
            this.channel.write(this.currentDataBuffer);
            if (this.currentDataBuffer.hasRemaining()) {
                return true;
            } else {
                this.currentDataBuffer = null;
            }
        }

        return queue.size() > 0;

    }

}
