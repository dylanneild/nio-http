package com.codeandstrings.niohttp.handlers.service;

import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.BufferContainer;

import java.io.*;
import java.nio.channels.Pipe;

public abstract class RequestHandler implements Runnable {

    private Pipe aPipe;
    private Pipe bPipe;

    private Pipe.SourceChannel engineSource;
    protected Pipe.SourceChannel handlerSource;
    private Pipe.SinkChannel engineSink;
    protected Pipe.SinkChannel handlerSink;

    private RequestReader requestReader;
    private RequestWriter requestWriter;
    private BufferReader bufferReader;
    private BufferWriter bufferWriter;

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

            this.requestReader = new RequestReader(this.handlerSource);
            this.requestWriter = new RequestWriter(this.engineSink);
            this.bufferReader = new BufferReader(this.engineSource);
            this.bufferWriter = new BufferWriter(this.handlerSink);

        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public Request executeRequestReadEvent() throws IOException, ClassNotFoundException {
        return this.requestReader.readRequestFromChannel();
    }

    public boolean executeRequestWriteEvent() throws IOException {
        return this.requestWriter.executeRequestWriteEvent();
    }

    public void sendRequest(Request r) {
        this.requestWriter.sendRequest(r);
    }

    public void sendBufferContainer(BufferContainer b) {
        this.bufferWriter.sendBufferContainer(b);
    }

    public boolean executeBufferWriteEvent() throws IOException {
        return this.bufferWriter.executeBufferWriteEvent();
    }

    public BufferContainer executeBufferReadEvent() throws IOException, ClassNotFoundException {
        return this.bufferReader.readBufferFromChannel();
    }

    public Pipe.SourceChannel getEngineSource() {
        return engineSource;
    }

    public Pipe.SinkChannel getEngineSink() {
        return engineSink;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("NIO-HTTP Handler Thread: " + this.getHandlerDescription());
        this.listenForRequests();
    }

    protected abstract void listenForRequests();

    protected abstract String getHandlerDescription();

}
