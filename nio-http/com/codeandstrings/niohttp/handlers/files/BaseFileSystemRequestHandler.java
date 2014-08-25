package com.codeandstrings.niohttp.handlers.files;

import com.codeandstrings.niohttp.data.DateUtils;
import com.codeandstrings.niohttp.data.DirectoryMember;
import com.codeandstrings.niohttp.data.mime.MimeTypes;
import com.codeandstrings.niohttp.enums.RequestMethod;
import com.codeandstrings.niohttp.exceptions.HandlerInitException;
import com.codeandstrings.niohttp.exceptions.http.*;
import com.codeandstrings.niohttp.handlers.base.RequestHandler;
import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

public abstract class BaseFileSystemRequestHandler extends RequestHandler {

    private Queue<Object> tasks;
    private FileSystem fileSystem;
    private MimeTypes mimeTypes;

    public abstract String getFilePath();
    public abstract String getUriPrefix();
    public abstract boolean isDirectoryListingsGenerated();
    public abstract String getDirectoryHeader(Request request);
    public abstract String getDirectoryListing(Request request, DirectoryMember directoryMember);
    public abstract String getDirectoryFooter(Request request);
    public abstract List<String> getDirectoryIndexes();

    public BaseFileSystemRequestHandler() throws HandlerInitException {
        super();
        this.tasks = new LinkedList<>();
        this.fileSystem = FileSystems.getDefault();
        this.mimeTypes = MimeTypes.getInstance();
    }

    private Path validatePath(Path path) throws NotFoundException, ForbiddenException {
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

        // is this a directory?
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            List<String> indexes = this.getDirectoryIndexes();

            if (indexes != null) {
                for (String index : indexes) {
                    try {
                        return validatePath(path.resolve(index));
                    }
                    catch (NotFoundException | ForbiddenException e) {}
                }
            }
        }

        return this.validatePath(path);

    }

    @Override
    protected String getHandlerDescription() {
        return "File System Handler";
    }

    private void sendException(HttpException e, Request request) throws IOException, InterruptedException {
        this.getEngineQueue().sendObject(ResponseFactory.createResponse(e, request));
    }

    private void serviceDirectoryRequest(Path path, Request request) throws Exception {

        if (request.getRequestMethod() == RequestMethod.HEAD) {
            Response r = ResponseFactory.createResponse("text/html", 0, request);
            r.setBodyIncluded(false);
            this.engineQueue.sendObject(r);
        }
        else {

            Response r = ResponseFactory.createStreamingResponse("text/html", request);
            byte headerBytes[] = this.getDirectoryHeader(request).getBytes(Charset.forName("UTF-8"));
            ResponseContent directoryHeader = new ResponseContent(request.getSessionId(), request.getRequestId(), headerBytes, false);

            this.engineQueue.sendObject(r);
            this.engineQueue.sendObject(directoryHeader);

            DirectoryRequestObject task = new DirectoryRequestObject(this.mimeTypes, path, request);

            if (task.hasNextMember()) {
                tasks.add(task);
            } else {

                // no files in the directory anyways...
                byte footerBytes[] = this.getDirectoryFooter(request).getBytes(Charset.forName("UTF-8"));
                ResponseContent directoryFooter = new ResponseContent(request.getSessionId(), request.getRequestId(), footerBytes, true);
                this.engineQueue.sendObject(directoryFooter);

            }

        }

    }

    private static void addFileInformationToRequest(FileRequestObject task, Response r) throws IOException {

        r.addHeader("Last-Modified", DateUtils.getRfc822DateStringGMT(task.getLastModified()));

        if (task.getEtag() != null) {
            r.addHeader("ETag", task.getEtag());
        }

    }

    private final void serviceFileRequest(Path path, Request request) throws Exception {

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

            if (skipBody) {
                r.setBodyIncluded(false);
            }


        }

        this.getEngineQueue().sendObject(r);

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
                this.sendException(new InternalServerErrorException(e), request);
            }
        }
    }

    private final void serviceRequest(Path path, Request request) throws Exception {

        // we handle directories differently
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            if (!isDirectoryListingsGenerated()) {
                this.sendException(new ForbiddenException(), request);
            } else {
                serviceDirectoryRequest(path, request);
            }
        } else {
            serviceFileRequest(path, request);
        }

    }

    private void executeFileReadProcedure(FileRequestObject task) throws ExecutionException, InterruptedException, IOException {

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

            this.getEngineQueue().sendObject(responseContent);

            if (finalBuffer) {
                task.close();
            }
        }

    }

    private void executeDirectoryReadProcedure(DirectoryRequestObject task) throws IOException, ParseException, InterruptedException {

        DirectoryMember directoryMember = task.next();

        if (directoryMember == null) {
            this.tasks.add(task);
            return;
        }

        Request request = task.getRequest();
        String listingContents = this.getDirectoryListing(request, directoryMember);

        // is this file hidden?
        if (listingContents != null) {

            // this is not a hidden file
            byte listingContentsBytes[] = listingContents.getBytes(Charset.forName("UTF-8"));
            ResponseContent r = new ResponseContent(request.getSessionId(), request.getRequestId(), listingContentsBytes, false);
            this.getEngineQueue().sendObject(r);

        }

        if (task.hasNextMember()) {
            this.tasks.add(task);
        } else {

            // close the task
            task.close();

            String directoryFooter = this.getDirectoryFooter(request);
            byte directoryFooterBytes[] = directoryFooter.getBytes(Charset.forName("UTF-8"));
            ResponseContent r = new ResponseContent(request.getSessionId(), request.getRequestId(), directoryFooterBytes, true);
            this.getEngineQueue().sendObject(r);

        }


    }

    private boolean executeReadProcedure() throws ExecutionException, InterruptedException, IOException, ParseException {

        // there are no further write events to execute;
        // let's see if there are more file read events to refill the buffers
        Object task = this.tasks.poll();

        if (task == null) {
            return false;
        }

        if (task instanceof DirectoryRequestObject) {
            this.executeDirectoryReadProcedure((DirectoryRequestObject) task);
        } else {
            this.executeFileReadProcedure((FileRequestObject)task);
        }

        return tasks.size() > 0;

    }

    @Override
    protected final void listenForRequests() {

        try {

            while (true) {

                int keyCount = this.selector.select();

                if (keyCount == 0) {
                    continue;
                }

                Iterator<SelectionKey> ki = selector.selectedKeys().iterator();

                while (ki.hasNext()) {

                    SelectionKey key = ki.next();

                    if (key.isReadable()) {

                        if (this.handlerQueue.shouldReadObject()) {

                            Request request = (Request) this.handlerQueue.getNextObject();

                            if (request != null) {
                                try {
                                    this.engineQueue.setContinueWriteNotifications(true);
                                    this.serviceRequest(getRatifiedFilePath(request.getRequestURI().getPath()), request);
                                } catch (HttpException e) {
                                    this.sendException(e, request);
                                }
                            }

                        }

                    } else {

                        this.engineQueue.sendObject(null);

                        if (!this.executeReadProcedure()) {
                            this.engineQueue.setContinueWriteNotifications(false);
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