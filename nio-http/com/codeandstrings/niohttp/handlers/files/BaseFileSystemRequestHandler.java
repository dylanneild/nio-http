package com.codeandstrings.niohttp.handlers.files;

import com.codeandstrings.niohttp.data.DateUtils;
import com.codeandstrings.niohttp.data.DirectoryMember;
import com.codeandstrings.niohttp.data.mime.MimeTypes;
import com.codeandstrings.niohttp.enums.RequestMethod;
import com.codeandstrings.niohttp.exceptions.http.*;
import com.codeandstrings.niohttp.handlers.base.RequestHandler;
import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

public abstract class BaseFileSystemRequestHandler extends RequestHandler {

    private Queue<FileRequestObject> tasks;
    private FileSystem fileSystem;
    private MimeTypes mimeTypes;

    public abstract String getFilePath();
    public abstract String getUriPrefix();
    public abstract boolean isDirectoryListingsGenerated();
    public abstract String getDirectoryHeader(Request request);
    public abstract String getDirectoryListing(Request request, DirectoryMember directoryMember);
    public abstract String getDirectoryFooter(Request request);

    public BaseFileSystemRequestHandler() {
        this.tasks = new LinkedList<FileRequestObject>();
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

        // TODO: This is where we'd ratify to see if there was an index file...

        return path;

    }

    @Override
    protected String getHandlerDescription() {
        return "File System Handler";
    }

    private void scheduleWrites(Selector selector) throws ClosedChannelException {
        this.getHandlerWriteChannel().register(selector, SelectionKey.OP_WRITE);
    }

    private void descheduleWrites(Selector selector) throws ClosedChannelException {
        this.getHandlerWriteChannel().register(selector, 0);
    }

    private void sendException(HttpException e, Request request, Selector selector) throws ClosedChannelException {

        ExceptionResponseFactory responseFactory = new ExceptionResponseFactory(e);
        Response response = responseFactory.create(request.getSessionId(), request.getServerParameters());
        ResponseContent container = new ResponseContent(request.getSessionId(), request.getRequestId(), response.getByteBuffer(), true);

        this.sendResponse(container);
        this.scheduleWrites(selector);

    }

    private void serviceDirectoryRequest(Path path, Request request, Selector selector) throws Exception {

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

        html.append(this.getDirectoryFooter(request));

        StringResponseFactory factory = new StringResponseFactory(request, "text/html", html.toString());

        this.sendResponse(factory.getHeader());
        this.sendResponse(factory.getBody());

        this.scheduleWrites(selector);

    }

    private static void addFileInformationToRequest(FileRequestObject task, Response r) throws IOException {

        r.addHeader("Last-Modified", DateUtils.getRfc822DateStringGMT(task.getLastModified()));

        if (task.getEtag() != null) {
            r.addHeader("ETag", task.getEtag());
        }

    }

    private final void serviceFileRequest(Path path, Request request, Selector selector) throws Exception {

        // we handle directories differently
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {

            if (!isDirectoryListingsGenerated()) {
                this.sendException(new ForbiddenException(), request, selector);
            } else {
                serviceDirectoryRequest(path, request, selector);
            }

            return;
        }

        // allocate a task and start the reading process
        FileRequestObject task = new FileRequestObject(path, this.mimeTypes.getMimeTypeForFilename(path.toString()), request);
        boolean skipBody = request.getRequestMethod() == RequestMethod.HEAD;
        boolean notModified = task.isNotModified(request);

        Response r = null;

        if (notModified) {
            // this task is not modified
            r = ResponseFactory.createResponseNotModified(request, task.getLastModified(), task.getEtag());
        } else {
            // we now have a task and it's readying data;
            r = ResponseFactory.createResponse(task.getMimeType(), task.getFileSize(), request);
            BaseFileSystemRequestHandler.addFileInformationToRequest(task, r);
        }

        ResponseContent responseHeader = new ResponseContent(request.getSessionId(), request.getRequestId(),
                r.getByteBuffer(), (skipBody || notModified));

        this.sendResponse(responseHeader);

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

    private boolean executeFileReadProcedure(Selector selector) throws ExecutionException, InterruptedException, ClosedChannelException {

        // there are no further write events to execute;
        // let's see if there are more file read events to refill the buffers
        FileRequestObject task = this.tasks.poll();

        if (task == null) {
            return false;
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

            // copy the captured buffer into a byte array
            byte content[] = new byte[nextBuffer.remaining()];
            nextBuffer.get(content);

            // we have a buffer to send and we know it
            ResponseContent responseContent = new ResponseContent(task.getSessionId(),
                    task.getRequestId(), content, finalBuffer);

            this.sendResponse(responseContent);

            if (finalBuffer) {
                task.close();
            }
        }

        return true;

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

                            boolean file = this.executeFileReadProcedure(selector);
                            boolean directory = false;

                            if (!file && !directory) {
                                this.descheduleWrites(selector);
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
