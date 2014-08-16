package com.codeandstrings.niohttp.data;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class FileUtils {

    public static String getMimeType(String filePath) throws IOException {

        if (!filePath.startsWith("/")) {
            throw new IOException("Non-absolute path: " + filePath);
        }

        StringBuilder url = new StringBuilder();
        url.append("file://localhost");
        url.append(filePath);

        URL u = new URL(url.toString());
        URLConnection uc = u.openConnection();

        return uc.getContentType();

    }

}
