package de.tsenger.vdstools.vds;

import de.tsenger.vdstools.DerTlv;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.util.encoders.Hex;
import org.tinylog.Logger;

import java.io.IOException;
import java.math.BigInteger;

public class VdsSignature {
	public static final byte TAG = (byte) 0xff;
	private final byte[] plainSignatureBytes;

	/**
	 * @param plainSignatureBytes signature bytes in plain format: r||s
	 */
	public VdsSignature(byte[] plainSignatureBytes) {
		this.plainSignatureBytes = plainSignatureBytes;
	}

	/**
	 * Returns signature in format ECDSASignature ::= SEQUENCE { r INTEGER, s
	 * INTEGER }
	 *
	 * @return ASN1 DER encoded signature as byte array
	 */
	public byte[] getDerSignatureBytes() {
		byte[] r = new byte[(plainSignatureBytes.length / 2)];
		byte[] s = new byte[(plainSignatureBytes.length / 2)];

		System.arraycopy(plainSignatureBytes, 0, r, 0, r.length);
		System.arraycopy(plainSignatureBytes, r.length, s, 0, s.length);

		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new ASN1Integer(new BigInteger(1, r)));
		v.add(new ASN1Integer(new BigInteger(1, s)));
		DERSequence derSeq = new DERSequence(v);

		byte[] derSignatureBytes = null;
		try {
			derSignatureBytes = derSeq.getEncoded();
			Logger.debug("Signature sequence bytes: 0x" + Hex.toHexString(derSignatureBytes));
		} catch (IOException e) {
			Logger.error("Couldn't parse r and s to DER Sequence Signature Bytes.");
		}
		return derSignatureBytes;
	}

	/**
	 * Returns signature bytes in plain format: r||s
	 *
	 * @return r||s signature byte array
	 */
	public byte[] getPlainSignatureBytes() {
		return plainSignatureBytes;
	}

	public byte[] getEncoded() throws IOException {
		DerTlv derSignature = new DerTlv(TAG, plainSignatureBytes);
		return derSignature.getEncoded();
	}

	public static VdsSignature fromByteArray(byte[] rawBytes) throws IOException {
		if (rawBytes[0] != TAG) {
			throw new IllegalArgumentException(
					String.format("VdsSignature shall have tag %2X, but tag %2X was found instead.", TAG, rawBytes[0]));
		}
		DerTlv derTlv = DerTlv.fromByteArray(rawBytes);
		return new VdsSignature(derTlv.getValue());
	}

}
