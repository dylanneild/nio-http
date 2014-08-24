package com.codeandstrings.niohttp.wire;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.LinkedBlockingQueue;

public class ChannelQueue {

    private Selector readSelector;
    private Selector writeSelector;
    private Pipe pipe;
    private Pipe.SinkChannel sinkChannel;
    private Pipe.SourceChannel sourceChannel;
    private LinkedBlockingQueue queue;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;
    private int notificationQueue;
    private boolean continueWriteNotifications;

    public void setContinueWriteNotifications(boolean continueWriteNotifications) {
        this.continueWriteNotifications = continueWriteNotifications;
    }

    public ChannelQueue(Selector readSelector) throws IOException {

        this.pipe = Pipe.open();
        this.sinkChannel = this.pipe.sink();
        this.sourceChannel = this.pipe.source();

        this.sinkChannel.configureBlocking(false);
        this.sourceChannel.configureBlocking(false);

        this.readSelector = readSelector;
        this.queue = new LinkedBlockingQueue();
        this.notificationQueue = 0;
        this.readBuffer = ByteBuffer.allocate(1);

        this.writeBuffer = ByteBuffer.allocate(1);
        this.writeBuffer.put((byte)0x1);
        this.writeBuffer.flip();

        this.sourceChannel.register(this.readSelector, SelectionKey.OP_READ);

    }

    public void setWriteSelector(Selector writeSelector) {
        this.writeSelector = writeSelector;
    }

    public Object getNextObject() {
        return this.queue.poll();
    }

    public boolean isThisReadChannel(SelectableChannel channel) {
        return this.sourceChannel == channel;
    }

    public boolean isThisWriteChannel(SelectableChannel channel) {
        return this.sinkChannel == channel;
    }

    public boolean shouldReadObject() throws IOException {

        this.readBuffer.clear();

        if (this.sourceChannel.read(this.readBuffer) == 1) {
            readBuffer.flip();
            return readBuffer.get() == 0x1;
        } else {
            return false;
        }

    }

    public void sendObject(Object object) throws IOException, InterruptedException {

        // object can be null - we do this because this
        // services as a way to send notifications if the initial notifications
        // fail due to write buffer excess.
        //
        if (object != null) {
            this.queue.put(object);
        }

        this.notificationQueue++;
        int deduction = 0;

        for (int i = 0; i < this.notificationQueue; i++) {
            this.writeBuffer.rewind();
            if (this.sinkChannel.write(this.writeBuffer) == 1) {
                deduction++;
            } else {
                break;
            }
        }

        this.notificationQueue -= deduction;
        int register = this.notificationQueue == 0 ? 0 : SelectionKey.OP_WRITE;

        if (continueWriteNotifications) {
            register = SelectionKey.OP_WRITE;
        }

        this.sinkChannel.register(this.writeSelector, register);

    }

}
