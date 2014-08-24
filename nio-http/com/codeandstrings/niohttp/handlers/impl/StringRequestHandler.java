package com.codeandstrings.niohttp.handlers.impl;

import com.codeandstrings.niohttp.exceptions.HandlerInitException;
import com.codeandstrings.niohttp.handlers.base.RequestHandler;
import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.StringResponseFactory;

import java.nio.channels.SelectionKey;
import java.util.Iterator;

public abstract class StringRequestHandler extends RequestHandler {

    public StringRequestHandler() throws HandlerInitException {
        super();
    }

    @Override
    protected final void listenForRequests() {

        try {

            while (true) {
                int keyCount = this.selector.select();

                if (keyCount == 0)
                    continue;

                Iterator<SelectionKey> ki = this.selector.selectedKeys().iterator();

                while (ki.hasNext()) {

                    SelectionKey key = ki.next();

                    if (key.isReadable()) {

                        if (this.handlerQueue.shouldReadObject()) {
                            Request request = (Request) this.handlerQueue.getNextObject();

                            if (request != null) {

                                String responseText = this.handleRequest(request);
                                StringResponseFactory factory = new StringResponseFactory(request, this.getContentType(), responseText);

                                this.engineQueue.sendObject(factory.getHeader());
                                this.engineQueue.sendObject(factory.getBody());

                            }
                        }

                    }
                    else if (key.isWritable()) {
                        this.engineQueue.sendObject(null);
                    }

                    ki.remove();

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public String getHandlerDescription() {
        return "String Request";
    }

    public abstract String getContentType();
    public abstract String handleRequest(Request request);

}
