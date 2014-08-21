package com.codeandstrings.niohttp.wire;

import com.codeandstrings.niohttp.response.ResponseContent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.ArrayList;

public class ResponseContentWriter {

    private Pipe.SinkChannel channel;
    private ArrayList<ResponseContent> queue;
    private ByteBuffer currentSizeBuffer;
    private ByteBuffer currentDataBuffer;

    public ResponseContentWriter(Pipe.SinkChannel channel) {
        this.channel = channel;
        this.queue = new ArrayList<ResponseContent>();
    }

    public void sendBufferContainer(ResponseContent r) {
        this.queue.add(r);
    }

    private boolean queueNextBuffer() throws IOException {

        if (queue.size() == 0)
            return false;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            oos.writeObject(queue.remove(0));
            oos.flush();

            this.currentSizeBuffer = ByteBuffer.allocate(Integer.SIZE / 8).putInt(baos.size());
            this.currentDataBuffer = ByteBuffer.wrap(baos.toByteArray());

            this.currentSizeBuffer.flip();

            return true;
        }
        catch (IOException e) {
            throw e;
        }

    }

    private boolean hasCurrentBuffer() {
        if (currentSizeBuffer != null)
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
