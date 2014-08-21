package com.codeandstrings.niohttp.wire;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.ArrayList;

public class PipeObjectWriter {

    private Pipe.SinkChannel channel;
    private ByteBuffer currentSizeBuffer;
    private ByteBuffer currentObjectBuffer;
    private ArrayList<Object> queue;

    public PipeObjectWriter(Pipe.SinkChannel channel) {
        this.channel = channel;
        this.queue = new ArrayList<Object>();
    }

    public void sendObject(Object r) {
        this.queue.add(r);
    }

    private boolean queueNextObject() throws IOException {

        if (queue.size() == 0)
            return false;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            oos.writeObject(queue.remove(0));
            oos.flush();

            this.currentSizeBuffer = ByteBuffer.allocate(Integer.SIZE / 8).putInt(baos.size());
            this.currentObjectBuffer = ByteBuffer.wrap(baos.toByteArray());

            this.currentSizeBuffer.flip();

            return true;
        }
        catch (IOException e) {
            throw e;
        }

    }

    private boolean hasCurrentObject() {
        if (currentSizeBuffer != null)
            return true;
        else if (currentObjectBuffer != null)
            return true;
        else
            return false;
    }

    public boolean executeObjectWriteEvent() throws IOException {

        if (!hasCurrentObject()) {
            if (!queueNextObject()) {
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

        if (this.currentObjectBuffer != null) {
            this.channel.write(this.currentObjectBuffer);
            if (this.currentObjectBuffer.hasRemaining()) {
                return true;
            } else {
                this.currentObjectBuffer = null;
            }
        }


        return queue.size() > 0;

    }

}
