package de.tsenger.vdstools.idb;

import java.io.IOException;
import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.util.encoders.Hex;
import org.tinylog.Logger;

import de.tsenger.vdstools.DerTlv;

public class IdbSignature {

	public final static byte TAG = 0x7F;

	private byte[] rawSignatureBytes;
	private byte[] derSignatureBytes = null;

	public IdbSignature(byte[] rawSignatureBytes) throws IOException {
		if (rawSignatureBytes[0] == TAG) {
			rawSignatureBytes = DerTlv.fromByteArray(rawSignatureBytes).getValue();
		}
		this.rawSignatureBytes = rawSignatureBytes;
		this.derSignatureBytes = buildDerSignature(rawSignatureBytes);
	}

	/**
	 * Returns signature in format ECDSASignature ::= SEQUENCE { r INTEGER, s
	 * INTEGER }
	 *
	 * @return ASN1 DER encoded signature as byte array
	 */
	public byte[] getDerSignatureBytes() {
		return derSignatureBytes;
	}

	/**
	 * Returns signature in raw format: r||s
	 *
	 * @return r||s signature byte array
	 */
	public byte[] getRawSignatureBytes() {
		return rawSignatureBytes;
	}

	public byte[] getEncoded() throws IOException {
		DerTlv derSignature = new DerTlv(TAG, rawSignatureBytes);
		return derSignature.getEncoded();
	}

	/**
	 * Builds signature in format ECDSASignature ::= SEQUENCE { r INTEGER, s INTEGER
	 * } from raw signature bytes
	 *
	 * @return ASN1 DER encoded signature as byte array
	 */
	private byte[] buildDerSignature(byte[] rsBytes) {
		byte[] r = new byte[(rsBytes.length / 2)];
		byte[] s = new byte[(rsBytes.length / 2)];

		System.arraycopy(rsBytes, 0, r, 0, r.length);
		System.arraycopy(rsBytes, r.length, s, 0, s.length);

		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new ASN1Integer(new BigInteger(1, r)));
		v.add(new ASN1Integer(new BigInteger(1, s)));
		DERSequence derSeq = new DERSequence(v);

		byte[] derSignatureBytes = null;
		try {
			derSignatureBytes = derSeq.getEncoded();
			Logger.debug("Signature sequence bytes: 0x" + Hex.toHexString(derSignatureBytes));
		} catch (IOException e) {
			Logger.error("Couldn't parse r and s to DER-encoded signature.");
		}

		return derSignatureBytes;

	}

}
