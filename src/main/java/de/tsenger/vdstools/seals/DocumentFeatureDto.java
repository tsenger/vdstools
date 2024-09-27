package de.tsenger.vdstools.seals;

import org.bouncycastle.util.encoders.Hex;

/**
 * @author Tobias Senger
 *
 */
public class DocumentFeatureDto {

    private final byte tag;
    private final int length;
    private final byte[] value;

    public DocumentFeatureDto(byte tag, int len, byte[] val) {
        this.tag = tag;
        this.length = len;
        this.value = val.clone();
    }

    byte getTag() {
        return tag;
    }
    
    int getLen() {
    	return length;
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
