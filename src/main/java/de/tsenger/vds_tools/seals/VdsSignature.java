/*
 * Sealva VDS Validator scans and verifies visible digital seals in barcodes
 *     Copyright (C) 2023.  Tobias Senger <sealva@tsenger.de>
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.tsenger.vds_tools.seals;


import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.util.encoders.Hex;
import org.tinylog.Logger;

import java.io.IOException;
import java.math.BigInteger;

public class VdsSignature {
    private byte[] rawSignatureBytes;
    private byte[] signatureBytes = null;

    public VdsSignature(byte[] rawBytes) {
        this.rawSignatureBytes = rawBytes;
        parseSignature(rawBytes);
    }

    /**
     * Returns signature in format
     * ECDSASignature ::= SEQUENCE {
     * r   INTEGER,
     * s   INTEGER
     * }
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
