package de.tsenger.vdstools.vds;

import org.bouncycastle.util.encoders.Hex;

public class Feature {
	private final Object value;
	private final FeatureCoding coding;
	private final String name;

	public FeatureCoding coding() {
		return coding;
	}

	public String name() {
		return name;
	}

	public Feature(String name, Object value, FeatureCoding coding) {
		this.name = name;
		this.value = value;
		this.coding = coding;
	}

	public String valueStr() {
		switch(coding) {
			case C40:
			case UTF8_STRING:
				return (String) value;
			case BYTE:
				return String.valueOf(valueInt());
			case BYTES:
			default:
				return Hex.toHexString((byte[]) value);
		}
	}

	public byte[] valueBytes() {
		return (byte[]) value;
	}

	public int valueInt() {
		return (byte)value;
	}
}
