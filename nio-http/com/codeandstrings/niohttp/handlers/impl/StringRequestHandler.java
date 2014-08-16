package com.codeandstrings.niohttp.handlers.impl;

import com.codeandstrings.niohttp.handlers.RequestHandler;
import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.BufferContainer;
import com.codeandstrings.niohttp.response.Response;
import com.codeandstrings.niohttp.response.ResponseFactory;

import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public abstract class StringRequestHandler extends RequestHandler {

    @Override
    protected void listenForRequests() {

        try {

            Selector selector = Selector.open();

            this.getHandlerReadChannel().register(selector, SelectionKey.OP_READ);

            while (true) {

                int keyCount = selector.select();

                if (keyCount == 0)
                    continue;

                Iterator<SelectionKey> ki = selector.selectedKeys().iterator();

                while (ki.hasNext()) {

                    SelectionKey key = ki.next();
                    SelectableChannel channel = key.channel();

                    if (channel instanceof Pipe.SourceChannel) {

                        Request request = (Request)this.executeRequestReadEvent();

                        if (request != null) {

                            Response response = ResponseFactory.createResponse(this.handleRequest(request),
                                    this.getContentType(), request);

                            BufferContainer container = new BufferContainer(request.getSessionId(),
                                    request.getRequestId(), response.getByteBuffer(), 0, true);

                            this.sendBufferContainer(container);
                            this.getHandlerWriteChannel().register(selector, SelectionKey.OP_WRITE);
                        }

                    } else {

                        if (!this.executeBufferWriteEvent()) {
                            channel.register(selector, 0);
                        }

                    }

                    ki.remove();

                }

            }

        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }

    @Override
    public String getHandlerDescription() {
        return "String Request";
    }

    public abstract String getContentType();
    public abstract String handleRequest(Request request);

}
