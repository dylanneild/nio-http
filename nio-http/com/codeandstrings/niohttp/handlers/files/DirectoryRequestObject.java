package com.codeandstrings.niohttp.handlers.files;

import com.codeandstrings.niohttp.data.DirectoryMember;
import com.codeandstrings.niohttp.data.mime.MimeTypes;
import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.Response;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Iterator;

public class DirectoryRequestObject {

    private MimeTypes mimeTypes;
    private Request request;
    private Response response;
    private DirectoryStream<Path> stream;
    private Iterator<Path> streamIterator;

    public DirectoryRequestObject(MimeTypes mimeTypes, Path path, Request request) throws IOException {
        this.mimeTypes = mimeTypes;
        this.request = request;
        this.stream = Files.newDirectoryStream(path);
        this.streamIterator = this.stream.iterator();
    }

    public void close() throws IOException {
        this.stream.close();
    }

    public Request getRequest() {
        return request;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public boolean hasNextMember() {
        return this.streamIterator.hasNext();
    }

    public DirectoryMember next() throws IOException, ParseException {
        Path path = this.streamIterator.next();
        String mimeType = this.mimeTypes.getMimeTypeForFilename(path.getFileName().toString());
        return new DirectoryMember(path, mimeType);
    }

}
