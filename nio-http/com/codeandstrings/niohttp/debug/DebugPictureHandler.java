package com.codeandstrings.niohttp.debug;

import com.codeandstrings.niohttp.exceptions.HandlerInitException;
import com.codeandstrings.niohttp.handlers.impl.FileSystemRequestHandler;

public class DebugPictureHandler extends FileSystemRequestHandler {

    public DebugPictureHandler() throws HandlerInitException {
        super();
    }

    @Override
    public String getFilePath() {

        if (System.getProperty("os.name") != null && System.getProperty("os.name").equalsIgnoreCase("mac os x"))
            return "/Users/dylan/Pictures";
        else
            return "/var/www/photos";

    }

    @Override
    public String getUriPrefix() {
        return "/pictures";
    }

}
