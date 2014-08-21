package com.codeandstrings.niohttp.handlers.impl;

import com.codeandstrings.niohttp.handlers.base.RequestHandler;
import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.ResponseContent;
import com.codeandstrings.niohttp.response.Response;
import com.codeandstrings.niohttp.response.ResponseFactory;
import com.codeandstrings.niohttp.response.StringResponseFactory;

import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public abstract class StringRequestHandler extends RequestHandler {

    @Override
    protected final void listenForRequests() {

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

                            String responseText = this.handleRequest(request);
                            StringResponseFactory factory = new StringResponseFactory(request, this.getContentType(), responseText);

                            this.sendBufferContainer(factory.getHeader());
                            this.sendBufferContainer(factory.getBody());

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
