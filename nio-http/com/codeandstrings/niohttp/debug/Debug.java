package com.codeandstrings.niohttp.debug;

import com.codeandstrings.niohttp.Server;
import com.codeandstrings.niohttp.data.Parameters;

public class Debug {

	public static void main(String args[]) {

		Server server = new Server();
        boolean isLocal = false;

        if (System.getProperty("os.name") != null && System.getProperty("os.name").equalsIgnoreCase("mac os x")) {
            isLocal = true;
        }

        if (!isLocal) {
            server.setParameters(new Parameters(80));
        }

        try {
            server.addRequestHandler("/pictures/.*", DebugPictureHandler.class);
            server.addRequestHandler("/version", DebugVersionHandler.class);
            server.addRequestHandler(".*", DebugRootHandler.class);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        server.run();
	}

}
