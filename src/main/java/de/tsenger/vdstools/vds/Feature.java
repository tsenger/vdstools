package de.tsenger.vdstools.vds;

import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;

public class Feature {
	private final Object value;
	private final FeatureCoding coding;
	private final String name;

	public FeatureCoding getCoding() {
		return coding;
	}

	public String getName() {
		return name;
	}

	public Feature(String name, Object value, FeatureCoding coding) {
		this.name = name;
		this.value = value;
		this.coding = coding;
	}

	public String asString() {
		switch(coding) {
			case C40:
			case UTF8_STRING:
				return (String) value;
			case BYTE:
				return String.valueOf(asInteger());
			case BYTES:
			default:
				return Hex.toHexString((byte[]) value);
		}
	}

	public String asHexString() {
		return Hex.toHexString((byte[]) value);
	}

	public byte[] asByteArray() {
		return (byte[]) value;
	}

	public int asInteger() {
		return (byte)value;
	}
}
