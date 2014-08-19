package com.codeandstrings.niohttp.handlers.impl;

import com.codeandstrings.niohttp.data.DirectoryMember;
import com.codeandstrings.niohttp.handlers.files.BaseFileSystemRequestHandler;
import com.codeandstrings.niohttp.request.Request;

import java.net.URLEncoder;

public abstract class FileSystemRequestHandler extends BaseFileSystemRequestHandler {

    @Override
    public String getDirectoryHeader(Request request) {

        StringBuilder r = new StringBuilder();
        String path = request.getRequestURI().getPath();

        r.append("<html>");
        r.append("<head>");
        r.append("<title>");
        r.append(path);
        r.append("</title>");
        r.append("</head>");
        r.append("<body>");
        r.append("<p>");

        return r.toString();

    }

    @Override
    public String getDirectoryListing(Request request, DirectoryMember directoryMember) {

        StringBuilder r = new StringBuilder();
        String path = request.getRequestURI().getPath();

        if (directoryMember.isHidden()) {
            return null;
        }

        r.append("<div>");
        r.append("<a href=\"");
        r.append(path);

        if (!path.endsWith("/")) {
            r.append("/");
        }

        try {
            r.append(URLEncoder.encode(directoryMember.getName(), "UTF-8"));
        }
        catch (Exception e) {
            r.append(directoryMember.getName());
        }

        r.append("\">");
        r.append(directoryMember.getName());
        r.append("</a>");
        r.append("</div>");

        return r.toString();
    }

    @Override
    public String getDirectoryFooter() {

        StringBuilder r = new StringBuilder();

        r.append("</p><p><pre>Directory index served by NIO-HTTP v1.0</pre></p>");

        r.append("</body>");
        r.append("</html>");

        return r.toString();

    }

}
