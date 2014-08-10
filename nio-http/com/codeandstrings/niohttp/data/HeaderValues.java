package com.codeandstrings.niohttp.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class HeaderValues {

	private ArrayList<HeaderPair> headers;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((headers == null) ? 0 : headers.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HeaderValues other = (HeaderValues) obj;
		if (headers == null) {
			if (other.headers != null)
				return false;
		} else if (!headers.equals(other.headers))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "HeaderValues [headers=" + headers + "]";
	}

	public HeaderValues() {
		this.headers = new ArrayList<HeaderPair>();
	}

	public List<String> getValue(String name) {
		
		ArrayList<String> r = new ArrayList<String>();
		
		for (HeaderPair pair : this.headers) {
			if (pair.getName().equals(name)) {
				r.add(pair.getValue());
			}
		}
		
		return r;
		
	}
	
	public Iterator<String> getNames() {
		
		HashSet<String> r = new HashSet<String>();
		
		for (HeaderPair pair : this.headers) {
			r.add(pair.getName());
		}
		
		return r.iterator();
		
	}
	
	public void addHeader(String name, String value) {

		if (name == null)
			return;

		if (value == null)
			return;

		String trimmedName = name.trim();
		String trimmedValue = value.trim();

		if (trimmedName.length() == 0)
			return;

		if (trimmedValue.length() == 0)
			return;

		this.headers.add(new HeaderPair(trimmedName, trimmedValue));

	}

	public String generateResponse() {

		StringBuilder b = new StringBuilder();

		for (HeaderPair pair : headers) {
			b.append(pair.asResponseString());
			b.append("\r\n");
		}

		return b.toString();

	}

}
