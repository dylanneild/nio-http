package com.codeandstrings.niohttp.handlers.base;

import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.ResponseContent;
import com.codeandstrings.niohttp.response.ResponseMessage;
import com.codeandstrings.niohttp.wire.*;

import java.io.*;
import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;

public abstract class RequestHandler implements Runnable {

    private Pipe aPipe;
    private Pipe bPipe;

    private Pipe.SourceChannel engineSource;
    private Pipe.SourceChannel handlerSource;
    private Pipe.SinkChannel engineSink;
    private Pipe.SinkChannel handlerSink;

    private PipeObjectReader requestReader;
    private PipeObjectWriter requestWriter;
    private PipeObjectReader responseContentReader;
    private PipeObjectWriter responseContentWriter;

    private Thread handlerThread;

    public RequestHandler()  {

        try {
            this.aPipe = Pipe.open();
            this.bPipe = Pipe.open();

            this.engineSource = aPipe.source();
            this.handlerSink = aPipe.sink();

            this.handlerSource = bPipe.source();
            this.engineSink = bPipe.sink();

            this.handlerSource.configureBlocking(false);
            this.engineSource.configureBlocking(false);
            this.handlerSink.configureBlocking(false);
            this.engineSink.configureBlocking(false);

            this.requestReader = new PipeObjectReader(this.handlerSource);
            this.requestWriter = new PipeObjectWriter(this.engineSink);
            this.responseContentReader = new PipeObjectReader(this.engineSource);
            this.responseContentWriter = new PipeObjectWriter(this.handlerSink);

        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void startThread() {
        this.handlerThread = new Thread(this);
        this.handlerThread.setName("NIO-HTTP Handler Thread: " + this.getHandlerDescription());
        this.handlerThread.start();
    }

    public Request executeRequestReadEvent() throws IOException, ClassNotFoundException {
        return (Request) this.requestReader.readObjectFromChannel();
    }

    public boolean executeRequestWriteEvent() throws IOException {
        return this.requestWriter.executeObjectWriteEvent();
    }

    public void sendRequest(Request r) {
        this.requestWriter.sendObject(r);
    }

    public void sendResponse(ResponseMessage b) {
        this.responseContentWriter.sendObject(b);
    }

    public boolean executeBufferWriteEvent() throws IOException {
        return this.responseContentWriter.executeObjectWriteEvent();
    }

    public ResponseMessage executeBufferReadEvent() throws IOException, ClassNotFoundException {
        return (ResponseMessage) this.responseContentReader.readObjectFromChannel();
    }

    public Pipe.SourceChannel getEngineSource() {
        return engineSource;
    }

    public Pipe.SinkChannel getEngineSink() {
        return engineSink;
    }

    @Override
    public void run() {
        this.listenForRequests();
    }

    protected abstract void listenForRequests();

    protected abstract String getHandlerDescription();

    protected SelectableChannel getHandlerReadChannel() {
        return this.handlerSource;
    }

    protected SelectableChannel getHandlerWriteChannel() {
        return this.handlerSink;
    }

    public int getConcurrency() {
        return 1;
    }

}