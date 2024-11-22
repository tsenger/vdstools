package de.tsenger.vdstools.vds;

import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;

public class Feature {
	private final Object value;
	private final String coding;
	private final String name;

	public String getCoding() {
		return coding;
	}

	public String getName() {
		return name;
	}

	public Feature(String name, Object value, String coding) {
		this.name = name;
		this.value = value;
		this.coding = coding;
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

	public String asHexString() {
		return Hex.toHexString((byte[]) value);
	}

	public byte[] asByteArray() {
		return (byte[]) value;
	}

	public int asInteger() {
		return ((byte[]) value)[0];
	}
}
