package com.codeandstrings.niohttp.data.mime;

import java.io.*;
import java.util.HashMap;

public class MimeTypes {

    public static MimeTypes getInstance() {

        MimeTypes r = new MimeTypes();
        r.map = new HashMap<String, String>();

        InputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;

        try {
            fis = MimeTypes.class.getResourceAsStream("mime.types");
            isr = new InputStreamReader(fis);
            br = new BufferedReader(isr);

            while (true) {

                String line = br.readLine();

                if (line == null)
                    return r;

                line = line.trim();

                if (!line.startsWith("#")) {
                    r.digest(line);
                }

            }

        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {}
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (Exception e) {}
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {}
            }
        }

    }

    private HashMap<String,String> map;

    private void digest(String line) {

        String[] strings = line.split("[ \t]");

        if (strings == null)
            return;

        if (strings.length < 2)
            return;

        String mimeType = strings[0].trim().toLowerCase();

        if (mimeType.length() == 0)
            return;

        for (int i = 1; i < strings.length; i++) {

            String extension = strings[i];

            if (extension == null)
                continue;

            extension = extension.trim();

            if (extension.length() == 0)
                continue;

            this.map.put(extension.toLowerCase(), mimeType);

        }

    }

    public String getMimeTypeForExtension(String extension) {

        if (extension==null)
            return null;

        return this.map.get(extension.toLowerCase());

    }

    public String getMimeTypeForFilename(String filename) {

        if (filename == null)
            return null;

        int extensionIndex = filename.lastIndexOf('.');

        if (extensionIndex == -1)
            return null;

        return this.getMimeTypeForExtension(filename.substring(extensionIndex + 1));

    }

}
