package com.codeandstrings.niohttp.handlers.impl;

import com.codeandstrings.niohttp.data.DirectoryMember;
import com.codeandstrings.niohttp.handlers.files.BaseFileSystemRequestHandler;
import com.codeandstrings.niohttp.request.Request;

import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.Date;

public abstract class FileSystemRequestHandler extends BaseFileSystemRequestHandler {

    @Override
    public boolean isDirectoryListingsGenerated() {
        return true;
    }

    @Override
    public String getDirectoryHeader(Request request) {

        StringBuilder r = new StringBuilder();
        String path = request.getRequestURI().getPath();

        r.append("<html>");
        r.append("<head>");
        r.append("<title>Index of ");
        r.append(path);
        r.append("</title>");
        r.append("</head>");
        r.append("<body>");
        r.append("<h1>Index of ");
        r.append(path);
        r.append("</h1>");
        r.append("<hr><p>");
        r.append("<table cellspacing=\"3\">");

        return r.toString();

    }

    protected static String getFileSizeString(long fileSize) {

        double d = Long.valueOf(fileSize).doubleValue();

        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setMaximumFractionDigits(0);

        if (fileSize < 1024) {
            return numberFormat.format(fileSize) + " B";
        } else if (fileSize < 1024 * 1024) {
            return numberFormat.format(d / 1024.0) + " KB";
        } else if (fileSize < 1024 * 1024 * 1024) {
            numberFormat.setMaximumFractionDigits(2);
            return numberFormat.format(d / (1024.0 * 1024.0)) + " MB";
        } else {
            numberFormat.setMaximumFractionDigits(2);
            return numberFormat.format(d / (1024.0 * 1024.0 * 1024.0)) + " GB";
        }

    }

    @Override
    public String getDirectoryListing(Request request, DirectoryMember directoryMember) {

        if (directoryMember.isHidden()) {
            return null;
        }

        StringBuilder r = new StringBuilder();
        String path = request.getRequestURI().getPath();

        r.append("<tr>");

        if (directoryMember.isDirectory()) {
            r.append("<td align=\"right\"><b>Folder</b></td>");
        } else {
            r.append("<td align=\"right\"><b>");
            r.append(getFileSizeString(directoryMember.getSize()));
            r.append("</b></td>");
        }

        r.append("<td>&nbsp;</td><td>");
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
        r.append("</td></tr>");

        return r.toString();
    }

    @Override
    public String getDirectoryFooter() {

        StringBuilder r = new StringBuilder();

        r.append("</table>");
        r.append("</p><hr><p><pre>Directory index served at " + (new Date()).toString() + "</pre></p>");

        r.append("</body>");
        r.append("</html>");

        return r.toString();

    }

}
