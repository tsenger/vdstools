package de.tsenger.vdstools;

import java.io.IOException;

import org.bouncycastle.asn1.BERTags;
import org.bouncycastle.asn1.DEROctetString;

public class DerTlv {

	private byte tag;
	private byte[] value;

	public DerTlv(byte tag, byte[] value) {
		this.tag = tag;
		this.value = value;
	}

	/**
	 * wraps the given data (Value) in a DER TLV object with free choice of the tag
	 * Length will be calculated as defined in ASN.1 DER length encoding
	 * 
	 * @return value with added tag and length
	 * @throws IOException on encoding error.
	 */
	public byte[] getEncoded() throws IOException {
		DEROctetString dos = new DEROctetString(this.value);
		byte[] encodeBytes = dos.getEncoded();
		encodeBytes[0] = this.tag;
		return encodeBytes;
	}

	public static DerTlv fromByteArray(byte[] rawBytes) throws IOException {
		byte tag = rawBytes[0];
		rawBytes[0] = BERTags.OCTET_STRING;
		DEROctetString dos = (DEROctetString) DEROctetString.fromByteArray(rawBytes);
		return new DerTlv(tag, dos.getOctets());
	}

	public byte getTag() {
		return tag;
	}

	public byte[] getValue() {
		return value;
	}
}
