package com.codeandstrings.niohttp.debug;

import com.codeandstrings.niohttp.exceptions.HandlerInitException;
import com.codeandstrings.niohttp.handlers.impl.StringRequestHandler;
import com.codeandstrings.niohttp.request.Request;

import java.util.Enumeration;
import java.util.Properties;

public class DebugVersionHandler extends StringRequestHandler {

    public DebugVersionHandler() throws HandlerInitException {
        super();
    }

    @Override
    public String getContentType() {
        return "text/plain";
    }

    @Override
    public String handleRequest(Request request) {

        Properties p = System.getProperties();
        StringBuilder r = new StringBuilder();

        for (Enumeration e = p.propertyNames(); e.hasMoreElements(); ) {

            String s = (String)e.nextElement();
            String v = p.getProperty(s);

            r.append(s);
            r.append(" = ");
            r.append(v);
            r.append("\n");

        }

        return r.toString();



    }

}
