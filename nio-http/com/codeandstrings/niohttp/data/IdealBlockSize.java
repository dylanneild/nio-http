package com.codeandstrings.niohttp.data;

import java.io.BufferedOutputStream;
import java.io.OutputStream;

public class IdealBlockSize {
	
	private static class MyBufferedOS extends BufferedOutputStream {
		public MyBufferedOS() {
			super(System.out);
		}

		public MyBufferedOS(OutputStream out) {
			super(out);
		}

		public int bufferSize() {
			return buf.length;
		}
	}

	public static int VALUE = new IdealBlockSize.MyBufferedOS().bufferSize();
	
}