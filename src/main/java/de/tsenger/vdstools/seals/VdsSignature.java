package de.tsenger.vdstools.seals;

import java.io.IOException;
import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.util.encoders.Hex;
import org.tinylog.Logger;

import de.tsenger.vdstools.DataEncoder;

/**
 * @author Tobias Senger
 *
 */
public class VdsSignature {
    private byte[] rawSignatureBytes;
    private byte[] signatureBytes = null;

    public VdsSignature(byte[] rawSignatureBytes) {
        this.rawSignatureBytes = rawSignatureBytes;
        parseSignature(rawSignatureBytes);
    }

    /**
     * Returns signature in format ECDSASignature ::= SEQUENCE { r INTEGER, s
     * INTEGER }
     *
     * @return ASN1 DER encoded signature as byte array
     */
    public byte[] getSignatureBytes() {
        return signatureBytes;
    }

    /**
     * Returns signature in raw format: r||s
     *
     * @return r||s signature byte array
     */
    public byte[] getRawSignatureBytes() {
        return rawSignatureBytes;
    }
    

    public byte[] getRawBytes() throws IOException {
        return DataEncoder.buildTLVStructure((byte) 0xff, rawSignatureBytes);
    }

    private void parseSignature(byte[] rsBytes) {
        byte[] r = new byte[(rsBytes.length / 2)];
        byte[] s = new byte[(rsBytes.length / 2)];

        System.arraycopy(rsBytes, 0, r, 0, r.length);
        System.arraycopy(rsBytes, r.length, s, 0, s.length);

        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new ASN1Integer(new BigInteger(1, r)));
        v.add(new ASN1Integer(new BigInteger(1, s)));
        DERSequence derSeq = new DERSequence(v);

        try {
            signatureBytes = derSeq.getEncoded();
            Logger.debug("Signature sequence bytes: 0x" + Hex.toHexString(signatureBytes));
        } catch (IOException e) {
            Logger.error("Couldn't parse r and s to signatureBytes.");
        }

    }
}
