package de.tsenger.vdstools.idb;

import java.util.HashMap;

//@formatter:off
public enum IdbMessageType {
	VISA((byte)0x01), 
    EMERGENCY_TRAVEL_DOCUMENT((byte)0x02), 
    PROOF_OF_TESTING((byte)0x03),
    PROOF_OF_VACCINATION((byte)0x04), 
    PROOF_OF_RECOVERY((byte)0x05), 
    DIGITALTRAVEL_AUTHORIZATION((byte)0x06),
    MRZ_TD1((byte)0x07), 
    MRZ_TD3((byte)0x08), 
    CAN((byte)0x09),
	EF_CARDACCESS((byte)0x0A);

    private final byte reference;
    private static final HashMap<Byte, IdbMessageType> map = new HashMap<>();

    IdbMessageType(byte reference) {
        this.reference = reference;
    }

    static {
        for (IdbMessageType messageType : IdbMessageType.values()) {
            map.put(messageType.reference, messageType);
        }
    }

    public static IdbMessageType valueOf(byte messageTag) {
        return map.get(messageTag);
    }

    public byte getValue() {
        return reference;
    }
}
