package com.codeandstrings.niohttp.handlers.impl;

import com.codeandstrings.niohttp.data.FileUtils;
import com.codeandstrings.niohttp.data.IdealBlockSize;
import com.codeandstrings.niohttp.enums.RequestMethod;
import com.codeandstrings.niohttp.exceptions.http.*;
import com.codeandstrings.niohttp.handlers.RequestHandler;
import com.codeandstrings.niohttp.request.Request;
import com.codeandstrings.niohttp.response.BufferContainer;
import com.codeandstrings.niohttp.response.ExceptionResponseFactory;
import com.codeandstrings.niohttp.response.Response;
import com.codeandstrings.niohttp.response.ResponseFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

class FileSystemRequestHandler$Task {

    private AsynchronousFileChannel fileChannel;
    private Future<Integer> future;
    private ByteBuffer readBuffer;
    private int position;
    private long fileSize;
    private String mimeType;
    private long requestId;
    private long sessionId;
    private long nextSequence;

    public FileSystemRequestHandler$Task(Path path, Request request) throws IOException {
        this.fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);
        this.fileSize = Files.size(path);
        this.position = 0;
        this.mimeType = FileUtils.getMimeType(path.toString());
        this.requestId = request.getRequestId();
        this.sessionId = request.getSessionId();
        this.nextSequence = 0;
    }

    public void close() {
        try {
            this.fileChannel.close();
        } catch (Exception e) {}
    }

    public long getRequestId() {
        return requestId;
    }

    public long getSessionId() {
        return sessionId;
    }

    public long getNextSequence() {
        this.nextSequence++;
        return this.nextSequence;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public boolean isBufferReady() throws ExecutionException, InterruptedException {

        if (!this.readBuffer.hasRemaining()) {
            return true;  // buffer is full
        } else if (this.future.get() == -1) {
            return true;  // buffer isn't full but last call was EOF
        } else if (this.fileSize - this.position - this.future.get() == 0) {
            return true;  // buffer isn't full but there is no more data
        } else {
            return false; // buffer isn't ready
        }

    }

    public ByteBuffer getBuffer() {
        return (ByteBuffer)this.readBuffer.flip();
    }

    public boolean isReadCompleted() throws ExecutionException, InterruptedException {
        return this.fileSize - this.position - this.future.get() == 0;
    }

    public void readNextBuffer() throws ExecutionException, InterruptedException {

        if (this.future != null) {
            this.position = this.position + this.future.get().intValue();
        }

        // TODO: This is a 64k buffer - at 8k, the system is almost 1/4 as fast.
        // TODO: This is a highly tunable value!
        this.readBuffer = ByteBuffer.allocate(IdealBlockSize.VALUE * 8);
        this.future = this.fileChannel.read(this.readBuffer, position);
    }

}

public abstract class FileSystemRequestHandler extends RequestHandler {

    private ArrayList<FileSystemRequestHandler$Task> tasks;
    private FileSystem fileSystem;

    public abstract String getFilePath();
    public abstract String getUriPrefix();

    public FileSystemRequestHandler() {
        this.tasks = new ArrayList<FileSystemRequestHandler$Task>();
        this.fileSystem = FileSystems.getDefault();
    }

    @Override
    public final int getConcurrency() {
        return 1;
    }

    private Path getRatifiedFilePath(String requestUri) throws BadRequestException, ForbiddenException, NotFoundException {

        if (!requestUri.startsWith(this.getUriPrefix()))
            throw new BadRequestException();

        String prefixRemoved = requestUri.substring(this.getUriPrefix().length());

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

    private void serviceDirectoryRequest(Path path, Request request, Selector selector) throws ClosedChannelException {
        System.out.println("Directory service not implemented.");
        this.sendException(new ForbiddenException(), request, selector);
    }

    private void serviceFileRequest(Path path, Request request, Selector selector) throws ClosedChannelException {

        // we handle directories differently
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            serviceDirectoryRequest(path, request, selector);
            return;
        }

        // allocate a task and start the reading process
        FileSystemRequestHandler$Task task = null;

        try {
            task = new FileSystemRequestHandler$Task(path, request);
            task.readNextBuffer();
        } catch (Exception e) {
            this.sendException(new InternalServerErrorException(e), request, selector);
        }

        boolean skipBody = request.getRequestMethod() == RequestMethod.HEAD;

        // we now have a task and it's readying data;
        Response r = ResponseFactory.createResponse(task.getMimeType(), task.getFileSize(), request);
        BufferContainer responseHeader = new BufferContainer(request.getSessionId(), request.getRequestId(), r.getByteBuffer(), 0, skipBody);
        this.sendBufferContainer(responseHeader);

        // if this is a HEAD request, don't bother sending back content
        // otherwise, schedule this new task for later
        if (skipBody) {
            task.close();
        } else {
            this.tasks.add(task);
        }

        this.scheduleWrites(selector);

    }

    @Override
    protected void listenForRequests() {

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
                            FileSystemRequestHandler$Task task = this.tasks.size() > 0 ? this.tasks.remove(0) : null;

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
