package com.codeandstrings.niohttp.handlers.files;

import com.codeandstrings.niohttp.data.DateUtils;
import com.codeandstrings.niohttp.data.DirectoryMember;
import com.codeandstrings.niohttp.data.mime.MimeTypes;
import com.codeandstrings.niohttp.enums.RequestMethod;
import com.codeandstrings.niohttp.exceptions.http.*;
import com.codeandstrings.niohttp.handlers.base.RequestHandler;
import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.BufferContainer;
import com.codeandstrings.niohttp.response.ExceptionResponseFactory;
import com.codeandstrings.niohttp.response.Response;
import com.codeandstrings.niohttp.response.ResponseFactory;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public abstract class BaseFileSystemRequestHandler extends RequestHandler {

    private ArrayList<FileRequestObject> tasks;
    private FileSystem fileSystem;
    private MimeTypes mimeTypes;

    public abstract String getFilePath();
    public abstract String getUriPrefix();
    public abstract String getDirectoryHeader(Request request);
    public abstract String getDirectoryListing(Request request, DirectoryMember directoryMember);
    public abstract String getDirectoryFooter();

    public BaseFileSystemRequestHandler() {
        this.tasks = new ArrayList<FileRequestObject>();
        this.fileSystem = FileSystems.getDefault();
        this.mimeTypes = MimeTypes.getInstance();
    }

    @Override
    public final int getConcurrency() {
        return 1;
    }

    private Path getRatifiedFilePath(String requestUri) throws BadRequestException, ForbiddenException, NotFoundException {

        String decoded = null;

        try {
            decoded = URLDecoder.decode(requestUri, "UTF-8");
        } catch (Exception e) {
            throw new BadRequestException();
        }

        if (!decoded.startsWith(this.getUriPrefix()))
            throw new BadRequestException();

        String prefixRemoved = decoded.substring(this.getUriPrefix().length());

        // TODO: The notion of which files should be considered index files
        // TODO: Needs to be implemented.

        if (prefixRemoved.length() == 0)
            prefixRemoved = "index.html";

        if (prefixRemoved.indexOf("..") != -1)
            throw new ForbiddenException();

        Path path = this.fileSystem.getPath(this.getFilePath(), prefixRemoved);

        // is this even a file?
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new NotFoundException();
        }

        // is this readable?
        if (!Files.isReadable(path)) {
            throw new ForbiddenException();
        }

        return path;

    }

    @Override
    protected String getHandlerDescription() {
        return "File System Handler";
    }

    private void scheduleWrites(Selector selector) throws ClosedChannelException {
        this.getHandlerWriteChannel().register(selector, SelectionKey.OP_WRITE);
    }

    private void sendException(HttpException e, Request request, Selector selector) throws ClosedChannelException {

        ExceptionResponseFactory responseFactory = new ExceptionResponseFactory(e);
        Response response = responseFactory.create(null);
        BufferContainer container = new BufferContainer(request.getSessionId(), request.getRequestId(), response.getByteBuffer(), 0, true);

        this.sendBufferContainer(container);
        this.scheduleWrites(selector);

    }

    private void serviceDirectoryRequest(Path path, Request request, Selector selector) throws IOException {

        StringBuilder html = new StringBuilder();

        html.append(this.getDirectoryHeader(request));

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
            for (Path filePath : directoryStream) {

                String directoryEntry = this.getDirectoryListing(request,
                        new DirectoryMember(filePath,
                                this.mimeTypes.getMimeTypeForFilename(filePath.getFileName().toString())));

                if (directoryEntry != null)
                    html.append(directoryEntry);

            }
        } catch (Exception e) {
            this.sendException(new InternalServerErrorException(e), request, selector);
        }

        html.append(this.getDirectoryFooter());

        Response response = ResponseFactory.createResponse(html.toString(), "text/html", request);
        BufferContainer responseHeader = new BufferContainer(request.getSessionId(), request.getRequestId(), response.getByteBuffer(), 0, true);
        this.sendBufferContainer(responseHeader);
        this.scheduleWrites(selector);

    }

    private static void addFileInformationToRequest(FileRequestObject task, Response r) throws IOException {

        r.addHeader("Last-Modified", DateUtils.getRfc822DateStringGMT(task.getLastModified()));

        if (task.getEtag() != null) {
            r.addHeader("ETag", task.getEtag());
        }

    }

    private static boolean shouldSendNotModified(Request r, FileRequestObject task) {

        String modifiedSince = r.getHeaderCaseInsensitive("If-Modified-Since");
        String etagMatch = r.getHeaderCaseInsensitive("If-None-Match");

        if (etagMatch != null && task.getEtag() != null && etagMatch.equals(task.getEtag())) {
            return true;
        }

        if (modifiedSince != null) {

            Date dateObject = DateUtils.parseRfc822DateString(modifiedSince);

            if (dateObject == null) {
                return false;
            }

            if (task.getLastModified().getTime() > dateObject.getTime()) {
                return false;
            } else {
                return true;
            }

        }

        return false;

    }

    private final void serviceFileRequest(Path path, Request request, Selector selector) throws IOException {

        // we handle directories differently
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            serviceDirectoryRequest(path, request, selector);
            return;
        }

        // allocate a task and start the reading process
        FileRequestObject task = new FileRequestObject(path, this.mimeTypes.getMimeTypeForFilename(path.toString()), request);
        boolean skipBody = request.getRequestMethod() == RequestMethod.HEAD;
        boolean notModified = shouldSendNotModified(request, task);

        Response r = null;

        if (notModified) {
            // this task is not modified
            r = ResponseFactory.createResponseNotModified(request, task.getLastModified(), task.getEtag());
        } else {
            // we now have a task and it's readying data;
            r = ResponseFactory.createResponse(task.getMimeType(), task.getFileSize(), request);
            BaseFileSystemRequestHandler.addFileInformationToRequest(task, r);
        }

        BufferContainer responseHeader = new BufferContainer(request.getSessionId(), request.getRequestId(), r.getByteBuffer(), 0, (skipBody || notModified));
        this.sendBufferContainer(responseHeader);

        // if this is a HEAD request, don't bother sending back content
        // otherwise, schedule this new task for later
        if (skipBody || notModified) {
            task.close();
        } else {
            try {
                task.readNextBuffer();
                this.tasks.add(task);
            } catch (Exception e) {
                task.close();
                this.sendException(new InternalServerErrorException(e), request, selector);
            }
        }

        // schedule writes, no matter what (we're either sending a response or an exception)
        this.scheduleWrites(selector);

    }

    @Override
    protected final void listenForRequests() {

        try {

            Selector selector = Selector.open();

            this.getHandlerReadChannel().register(selector, SelectionKey.OP_READ);

            while (true) {

                int keyCount = selector.select();

                if (keyCount == 0)
                    continue;

                Iterator<SelectionKey> ki = selector.selectedKeys().iterator();

                while (ki.hasNext()) {

                    SelectionKey key = ki.next();
                    SelectableChannel channel = key.channel();

                    if (channel instanceof Pipe.SourceChannel) {

                        Request request = (Request)this.executeRequestReadEvent();

                        if (request != null) {
                            try {
                                this.serviceFileRequest(getRatifiedFilePath(request.getRequestURI().getPath()), request, selector);
                            } catch (HttpException e) {
                                this.sendException(e, request, selector);
                            }
                        }

                    } else {

                        if (!this.executeBufferWriteEvent()) {

                            // there are no further write events to execute;
                            // let's see if there are more file read events to refill the buffers
                            FileRequestObject task = this.tasks.size() > 0 ? this.tasks.remove(0) : null;

                            if (task == null) {
                                channel.register(selector, 0);
                                ki.remove();
                                continue;
                            }

                            // the current buffer is ready.
                            if (!task.isBufferReady()) {
                                // return the task to the queue
                                this.tasks.add(task);
                            } else {

                                ByteBuffer nextBuffer = task.getBuffer();
                                boolean finalBuffer = false;

                                if (task.isReadCompleted()) {
                                    // our buffer is ready and we're done reading.
                                    // we're not going to add anything back because
                                    // there is no reason to.
                                    finalBuffer = true;
                                } else {
                                    // our buffer is ready and we're not done reading.
                                    // we've captured our buffer though, so get the next one
                                    // re-add the task to the task array, which will move it to
                                    // the bottom of the queue
                                    task.readNextBuffer();
                                    this.tasks.add(task);
                                }

                                // we have a buffer to send and we know it
                                BufferContainer bufferContainer = new BufferContainer(task.getSessionId(),
                                        task.getRequestId(), nextBuffer, task.getNextSequence(), finalBuffer);

                                this.sendBufferContainer(bufferContainer);

                                if (finalBuffer) {
                                    task.close();
                                }


                            }

                        }

                    }

                    ki.remove();

                }

            }

        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
