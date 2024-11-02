package de.tsenger.vdstools.vds;

import java.nio.charset.StandardCharsets;

public class Feature {
	private final Object value;

	public Feature(Object value) {
		this.value = value;
	}

	public boolean isEmpty() {
		return value == null;
	}

	public String asString() {
		if (value instanceof String) {
			return (String) value;
		} else if (value instanceof byte[]) {
			// Konvertiere byte[] in UTF-8-String
			return new String((byte[]) value, StandardCharsets.UTF_8);
		}
		return null;
	}

	public byte[] asByteArray() {
		return (byte[]) value;
	}

	public int asInteger() {
		return ((byte[]) value)[0];
	}
}
