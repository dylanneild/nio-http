package com.codeandstrings.niohttp.data;

import java.io.BufferedOutputStream;
import java.io.OutputStream;

public class IdealBlockSize {
	
	private static class ExtendedBufferedOutputStream extends BufferedOutputStream {
		public ExtendedBufferedOutputStream() {
			super(System.out);
		}

		public ExtendedBufferedOutputStream(OutputStream out) {
			super(out);
		}

		public int bufferSize() {
			return buf.length;
		}
	}

	public static int VALUE = new IdealBlockSize.ExtendedBufferedOutputStream().bufferSize();
    public static int MAX_BUFFER = VALUE * 8;
	
}