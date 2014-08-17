package com.codeandstrings.niohttp.data;

import java.io.*;
import java.util.*;

public class HeaderValues implements Externalizable {

	private ArrayList<NameValuePair> headers;

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.headers = (ArrayList<NameValuePair>)in.readObject();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(this.headers);
    }

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

    public HeaderValues(boolean initialize) {
        if (initialize) {
            this.headers = new ArrayList<NameValuePair>();
        }
    }

    public HeaderValues() {}

	public List<String> getValue(String name) {
		
		ArrayList<String> r = new ArrayList<String>();
		
		for (NameValuePair pair : this.headers) {
			if (pair.getName().equals(name)) {
				r.add(pair.getValue());
			}
		}
		
		return r;
		
	}

    public void removeHeader(String name) {

        Iterator<NameValuePair> itr = this.headers.iterator();

        while (itr.hasNext()) {
            NameValuePair nvp = itr.next();
            if (nvp.getName().equalsIgnoreCase(name)) {
                itr.remove();
            }
        }

    }
	
	public String getCaseInsensitiveHeaderName(String name) {
		
		for (NameValuePair pair : this.headers) {
			if (pair.getName().equalsIgnoreCase(name)) {
				return pair.getValue();
			}
		}
		
		return null;
		
	}
	
	public String getRequestContentType() {		
		return getCaseInsensitiveHeaderName("Content-Type");		
	}
	
	public int getRequestContentLength() {
		
		String target = getCaseInsensitiveHeaderName("Content-Length");
		
		if (target == null)
			return -1;
		
		try {
			Integer r = Integer.valueOf(target);
			return r.intValue();
		} catch (Exception e) {
			return -1;
		}
		
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
