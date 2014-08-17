package com.codeandstrings.niohttp.debug;

import com.codeandstrings.niohttp.handlers.impl.FileSystemRequestHandler;

public class DebugPictureHandler extends FileSystemRequestHandler {

    @Override
    public String getFilePath() {
        return "/Users/dylan/Pictures";
    }

    @Override
    public String getUriPrefix() {
        return "/pictures";
    }

}
