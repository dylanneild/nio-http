package com.codeandstrings.niohttp.handlers.broker;

import com.codeandstrings.niohttp.exceptions.HandlerInitException;
import com.codeandstrings.niohttp.exceptions.InvalidHandlerException;
import com.codeandstrings.niohttp.handlers.base.RequestHandler;
import com.codeandstrings.niohttp.request.Request;

import java.nio.channels.*;
import java.util.ArrayList;

public class RequestHandlerBroker {

    private ArrayList<RequestHandlerMount> handlers;
    private Selector engineSelector;

    public RequestHandlerBroker(Selector selector) {
        this.engineSelector = selector;
        this.handlers = new ArrayList<RequestHandlerMount>();
    }

    public RequestHandler getHandlerForRequest(Request request) {

        ArrayList<RequestHandlerMount> targets = new ArrayList<RequestHandlerMount>(handlers.size());
        String directMatch = null;

        for (RequestHandlerMount mount : this.handlers) {
            if (mount.matches(request)) {

                if (directMatch == null) {
                    directMatch = mount.getMountPoint();
                    targets.add(mount);
                }
                else if (directMatch.equals(mount.getMountPoint())) {
                    targets.add(mount);
                }

            }
        }

        if (targets.size() == 0)
            return null;
        else if (targets.size() == 1)
            return targets.get(0).getHandler();

        for (int i = 0; i < targets.size(); i++) {

            RequestHandlerMount t = targets.get(i);

            if (t.isNext()) {
                t.setNext(false);

                if (i == (targets.size() - 1)) {
                    targets.get(0).setNext(true);
                } else {
                    targets.get(i+1).setNext(true);
                }

                return t.getHandler();
            }

        }

        // there was no next target
        targets.get(1).setNext(true);

        return targets.get(0).getHandler();

    }

    private static boolean isValidClass(Class handler) {
        if (handler == null)
            return false;
        else if (handler.getName().equalsIgnoreCase("java.lang.Object"))
            return false;
        else if (handler.getName().equalsIgnoreCase(RequestHandler.class.getName()))
            return true;
        else
            return isValidClass(handler.getSuperclass());
    }

    public void addHandler(String path, Class handler) throws InvalidHandlerException, HandlerInitException {

        if (!isValidClass(handler))
            throw new InvalidHandlerException();

        try {

            RequestHandler newHandler = (RequestHandler) handler.newInstance();
            newHandler.setEngineQueueSelector(this.engineSelector);

            RequestHandlerMount mount = new RequestHandlerMount(path, newHandler);

            newHandler.start();

            this.handlers.add(mount);

        } catch (InstantiationException | IllegalAccessException e) {
            throw new InvalidHandlerException(e);
        }

    }

    public RequestHandler getHandlerForEngineReceive(SelectableChannel channel) {

        if (this.handlers.size() == 0) {
            return null;
        }

        for (RequestHandlerMount mount : this.handlers) {
            RequestHandler handler = mount.getHandler();
            if (handler.getEngineQueue().isThisReadChannel(channel)) {
                return handler;
            }
        }

        return null;

    }

    public RequestHandler getHandlerForEngineSend(SelectableChannel channel) {

        if (this.handlers.size() == 0) {
            return null;
        }

        for (RequestHandlerMount mount : this.handlers) {
            RequestHandler handler = mount.getHandler();
            if (handler.getHandlerQueue().isThisWriteChannel(channel)) {
                return handler;
            }
        }

        return null;

    }


//    public RequestHandler getHandlerForEngineSinkChannel(Pipe.SinkChannel channel) {
//
//        if (this.handlers.size() == 0)
//            return null;
//
//        for (RequestHandlerMount mount : this.handlers) {
//            if (mount.getHandler().getEngineSink() == channel) {
//                return mount.getHandler();
//            }
//        }
//
//        return null;
//    }

}
