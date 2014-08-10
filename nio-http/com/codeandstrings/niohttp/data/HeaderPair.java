package com.codeandstrings.niohttp.data;

public class HeaderPair {

	private String name;
	private String value;

	public String asResponseString() {

		StringBuilder r = new StringBuilder();

		r.append(name);
		r.append(": ");
		r.append(value);

		return r.toString();

	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	public HeaderPair(String name, String value) {
		super();
		this.name = name;
		this.value = value;
	}

	@Override
	public String toString() {
		return "HeaderPair [name=" + name + ", value=" + value + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		HeaderPair other = (HeaderPair) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

}
