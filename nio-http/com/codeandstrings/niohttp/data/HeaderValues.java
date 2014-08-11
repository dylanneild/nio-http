package com.codeandstrings.niohttp.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HeaderValues {

	private ArrayList<NameValuePair> headers;

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
		this.headers = new ArrayList<NameValuePair>();
	}

	public List<String> getValue(String name) {
		
		ArrayList<String> r = new ArrayList<String>();
		
		for (NameValuePair pair : this.headers) {
			if (pair.getName().equals(name)) {
				r.add(pair.getValue());
			}
		}
		
		return r;
		
	}
	
	public Set<String> getNames() {
		
		HashSet<String> h = new HashSet<String>();
		
		for (NameValuePair pair : this.headers) {
			h.add(pair.getName());
		}
		
		return h;
		
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

		this.headers.add(new NameValuePair(trimmedName, trimmedValue));

	}

	public String generateResponse() {

		StringBuilder b = new StringBuilder();

		for (NameValuePair pair : headers) {
			b.append(pair.asHeaderString());
			b.append("\r\n");
		}

		return b.toString();

	}

}
