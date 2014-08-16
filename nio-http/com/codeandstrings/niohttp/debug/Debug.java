package com.codeandstrings.niohttp.debug;

import com.codeandstrings.niohttp.Server;

public class Debug {

	public static void main(String args[]) {

		Server server = new Server();

        try {

            server.addRequestHandler("/version", DebugVersionHandler.class);
            server.addRequestHandler(".*", DebugRootHandler.class);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        server.run();
	}

}
