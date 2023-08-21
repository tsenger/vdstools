package de.tsenger.vds_tools.seals;

import org.bouncycastle.util.encoders.Hex;

/**
 * Created by Tobias Senger on 12.01.2017.
 */

public class DocumentFeature {

    private final byte tag;
    private final int length;
    private final byte[] value;

    public DocumentFeature(byte tag, int len, byte[] val) {
        this.tag = tag;
        this.length = len;
        this.value = val.clone();
    }

    byte getTag() {
        return tag;
    }

    byte[] getValue() {
        return value;
    }

    @Override
    public String toString() {
        return " T: " + String.format("%02X ", tag) + " L: " + String.format("%02X ", length) + " V: "
                + Hex.toHexString(value);
    }
}
