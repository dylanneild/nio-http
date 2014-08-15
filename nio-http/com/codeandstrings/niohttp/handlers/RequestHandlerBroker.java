package com.codeandstrings.niohttp.handlers;

import com.codeandstrings.niohttp.exceptions.InvalidHandlerException;
import com.codeandstrings.niohttp.request.Request;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;

public class RequestHandlerBroker {

    private ArrayList<RequestHandlerMount> handlers;

    public RequestHandlerBroker() {
        this.handlers = new ArrayList<RequestHandlerMount>();
    }

    public void setSelectorReadHandler(Selector selector) throws ClosedChannelException {
        for (RequestHandlerMount mount : this.handlers) {
            mount.getHandler().getEngineSource().register(selector, SelectionKey.OP_READ);
        }
    }

    public RequestHandler getHandlerForRequest(Request request) {

        ArrayList<RequestHandlerMount> targets = new ArrayList<RequestHandlerMount>(handlers.size());

        for (RequestHandlerMount mount : this.handlers) {
            if (mount.matches(request)) {
                targets.add(mount);
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
//                    System.out.println("Made 0 next target.");
                    targets.get(0).setNext(true);
                } else {
//                    System.out.println("Made " + (i+1) +  " next target.");
                    targets.get(i+1).setNext(true);
                }

//                System.out.println("Returning target " + i);

                return t.getHandler();
            }

        }

        // there was no next target
//        System.out.println("Returning target 0, setting next to 1");
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

    public void addHandler(String path, Class handler) throws InvalidHandlerException {

        if (!isValidClass(handler))
            throw new InvalidHandlerException();

        try {

            RequestHandler testHandler = (RequestHandler)handler.newInstance();

            for (int i = 0; i < testHandler.getConcurrency(); i++) {
                RequestHandler newHandler = (RequestHandler)handler.newInstance();
                RequestHandlerMount mount = new RequestHandlerMount(path, newHandler);
                this.handlers.add(mount);
                newHandler.startThread();
            }


        } catch (InstantiationException e) {
            throw new InvalidHandlerException(e);
        } catch (IllegalAccessException e) {
            throw new InvalidHandlerException(e);
        }

    }

    public RequestHandler getHandlerForEngineSourceChannel(Pipe.SourceChannel channel) {

        if (this.handlers.size() == 0)
            return null;

        for (RequestHandlerMount mount : this.handlers) {
            if (mount.getHandler().getEngineSource() == channel) {
                return mount.getHandler();
            }
        }

        return null;

    }

    public RequestHandler getHandlerForEngineSinkChannel(Pipe.SinkChannel channel) {

        if (this.handlers.size() == 0)
            return null;

        for (RequestHandlerMount mount : this.handlers) {
            if (mount.getHandler().getEngineSink() == channel) {
                return mount.getHandler();
            }
        }

        return null;
    }

}
