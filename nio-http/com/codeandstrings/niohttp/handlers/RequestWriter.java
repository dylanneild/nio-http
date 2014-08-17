package com.codeandstrings.niohttp.handlers;

import com.codeandstrings.niohttp.request.Request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.ArrayList;

public class RequestWriter {

    private Pipe.SinkChannel channel;
    private ByteBuffer currentSizeBuffer;
    private ByteBuffer currentRequestBuffer;
    private ArrayList<Request> queue;

    public RequestWriter(Pipe.SinkChannel channel) {
        this.channel = channel;
        this.queue = new ArrayList<Request>();
    }

    public void sendRequest(Request r) {
        this.queue.add(r);
    }

    private boolean queueNextRequest() throws IOException {

        if (queue.size() == 0)
            return false;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(queue.remove(0));

        oos.flush();
        oos.close();

        this.currentSizeBuffer = ByteBuffer.allocate(Integer.SIZE / 8).putInt(baos.size());
        this.currentRequestBuffer = ByteBuffer.allocate(baos.size()).put(baos.toByteArray());

        this.currentSizeBuffer.flip();
        this.currentRequestBuffer.flip();

        return true;

    }

    private boolean hasCurrentRequest() {
        if (currentSizeBuffer != null)
            return true;
        else if (currentRequestBuffer != null)
            return true;
        else
            return false;
    }

    public boolean executeRequestWriteEvent() throws IOException {

        if (!hasCurrentRequest()) {
            if (!queueNextRequest()) {
                return false;
            }
        }

        if (this.currentSizeBuffer != null) {
            this.channel.write(this.currentSizeBuffer);
            if (this.currentSizeBuffer.hasRemaining()) {

                this.currentSizeBuffer.compact();
                this.currentSizeBuffer.flip();

                return true;

            } else {
                this.currentSizeBuffer = null;
            }
        }

        if (this.currentRequestBuffer != null) {
            this.channel.write(this.currentRequestBuffer);
            if (this.currentRequestBuffer.hasRemaining()) {

                this.currentRequestBuffer.compact();
                this.currentRequestBuffer.flip();

                return true;

            } else {
                this.currentRequestBuffer = null;
            }
        }


        return queue.size() > 0;

    }

}
