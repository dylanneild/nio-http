package com.codeandstrings.niohttp.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class DirectoryMembers {

    private long size;
    private String name;
    private boolean hidden;
    private String mimeType;
    private boolean directory;
    private boolean file;
    private Date date;

    public long getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    public boolean isHidden() {
        return hidden;
    }

    public String getMimeType() {
        return mimeType;
    }

    public boolean isDirectory() {
        return directory;
    }

    public boolean isFile() {
        return file;
    }

    public Date getDate() {
        return date;
    }

    public DirectoryMembers(Path file, String mimeType) throws IOException, ParseException {

        this.size = Files.size(file);
        this.name = file.getFileName().toString();
        this.hidden = Files.isHidden(file);
        this.mimeType = mimeType;
        this.directory = Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS);
        this.file = Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS);
        this.date = new Date((((FileTime)Files.getAttribute(file, "lastModifiedTime", LinkOption.NOFOLLOW_LINKS)).toMillis()));

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DirectoryMembers that = (DirectoryMembers) o;

        if (directory != that.directory) return false;
        if (file != that.file) return false;
        if (hidden != that.hidden) return false;
        if (size != that.size) return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (mimeType != null ? !mimeType.equals(that.mimeType) : that.mimeType != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (size ^ (size >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (hidden ? 1 : 0);
        result = 31 * result + (mimeType != null ? mimeType.hashCode() : 0);
        result = 31 * result + (directory ? 1 : 0);
        result = 31 * result + (file ? 1 : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DirectoryMembers{" +
                "size=" + size +
                ", name='" + name + '\'' +
                ", hidden=" + hidden +
                ", mimeType='" + mimeType + '\'' +
                ", directory=" + directory +
                ", file=" + file +
                ", date=" + date +
                '}';
    }
}
