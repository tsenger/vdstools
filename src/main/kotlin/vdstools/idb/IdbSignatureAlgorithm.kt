package de.tsenger.vdstools.idb;

import java.util.HashMap;

//@formatter:off
public enum IdbSignatureAlgorithm {
	SHA256_WITH_ECDSA((byte)0x01), 
	SHA384_WITH_ECDSA((byte)0x02), 
	SHA512_WITH_ECDSA((byte)0x03),
    ;

    private final byte reference;
    private static final HashMap<Byte, IdbSignatureAlgorithm> map = new HashMap<>();

    IdbSignatureAlgorithm(byte reference) {
        this.reference = reference;
    }

    static {
        for (IdbSignatureAlgorithm algorithm : IdbSignatureAlgorithm.values()) {
            map.put(algorithm.reference, algorithm);
        }
    }

    public static IdbSignatureAlgorithm valueOf(byte value) {
        return map.get(value);
    }

    public byte getValue() {
        return reference;
    }
}
