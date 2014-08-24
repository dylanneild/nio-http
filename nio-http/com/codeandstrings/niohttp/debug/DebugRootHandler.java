package com.codeandstrings.niohttp.debug;

import com.codeandstrings.niohttp.exceptions.HandlerInitException;
import com.codeandstrings.niohttp.handlers.impl.StringRequestHandler;
import com.codeandstrings.niohttp.request.Request;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DebugRootHandler extends StringRequestHandler {

    private static int hits = 0;

    public DebugRootHandler() throws HandlerInitException {
        super();
    }

    @Override
    public String handleRequest(Request request) {

        hits++;

        StringBuilder r = new StringBuilder();

        r.append("<html>\n");
        r.append("<head>\n");
        r.append("<title>Java NIO Debug</title>\n");
        r.append("</head>\n");
        r.append("<body>\n");

        r.append("<h1>NIO-HTTP Debug Page - " + this.hits + "</h1>\n");

        r.append("<p><b>Serving instance: </b>" + this.toString() + " <br>");
        r.append("<b>Your IP: </b>");
        r.append(request.getRemoteAddr());
        r.append("<br>");
        r.append("<b>Version Info: </b><a href=\"/version\">Click Here</a><br>");
        r.append("<b>Pictures: </b><a href=\"/pictures/\">Click Here</a><br>");
        r.append("</p>");

        r.append("<hr>\n");

        r.append("<p>\n");
        r.append("<h2>Header values received:</h2>\n");

        Iterator<String> headerItr = request.getHeaderNames()
                .iterator();

        while (headerItr.hasNext()) {
            String headerName = headerItr.next();
            List<String> values = request.getHeaders(headerName);

            for (String value : values) {
                r.append("<strong>" + headerName + "</strong> = "
                        + value + "<br>\n");
            }
        }

        r.append("</p>\n");

        r.append("<hr>\n");

        r.append("<p>\n");
        r.append("<h2>Parameter values received (GET and POST):</h2>\n");

        Set<String> valueCollection = request.getParameterNames();

        if (valueCollection.size() == 0) {
            r.append("None\n");
        } else {

            Iterator<String> valueItr = valueCollection.iterator();

            while (valueItr.hasNext()) {
                String valueName = valueItr.next();
                List<String> values = request.getParameters(valueName);

                for (String value : values) {
                    r.append("<strong>" + valueName + "</strong>: "
                            + value + "<br>\n");
                }
            }

        }

        r.append("</p>\n");

        r.append("<hr>\n");

        r.append("<h2>Form Test</h2>\n");

        String name = request.getParameter("name");
        String address = request.getParameter("address");
        String description = request.getParameter("description");

        if (name == null)
            name = "";

        if (address == null)
            address = "";

        if (description == null)
            description = "";

        r.append("<form method=\"post\" action=\"/?extraValue=received_From_Get\">\n");
        r.append("<p><strong>Name: </strong><input type=\"text\" name=\"name\" value=\"" + name + "\"></p>\n");
        r.append("<p><strong>Address: </strong><input type=\"text\" name=\"address\" value=\"" + address + "\"></p>\n");
        r.append("<p><strong>Description: </strong><br>\n");
        r.append("<textarea name=\"description\" cols=\"80\" rows=\"10\">" + description + "</textarea></p>\n");
        r.append("<p><input type=\"submit\" value=\"Submit\">\n");
        r.append("</form>\n");
        r.append("<hr>\n");

        r.append("</body>\n");
        r.append("</html>");

        return r.toString();

    }

    @Override
    public String getContentType() {
        return "text/html";
    }

}
