package de.tsenger.vdstools.idb;

import de.tsenger.vdstools.asn1.DerTlv;

import java.io.IOException;

public class IdbMessage {

    private final byte messageType;
    private final byte[] messageContent;

    public IdbMessage(IdbMessageType messageType, byte[] messageContent) {
        this.messageType = messageType.getValue();
        this.messageContent = messageContent;
    }

    public IdbMessage(byte messageType, byte[] messageContent) {
        this.messageType = messageType;
        this.messageContent = messageContent;
    }

    public static IdbMessage fromDerTlv(DerTlv derTlv) {
        IdbMessageType messageType = IdbMessageType.valueOf(derTlv.tag);
        byte[] messageContent = derTlv.value;
        return new IdbMessage(messageType, messageContent);
    }

    public static IdbMessage fromByteArray(byte[] rawMessageBytes) throws IOException {
        DerTlv tlvMessage = DerTlv.fromByteArray(rawMessageBytes);
        return IdbMessage.fromDerTlv(tlvMessage);
    }

    public byte[] getEncoded() throws IOException {
        return new DerTlv(messageType, messageContent).getEncoded();
    }

    public IdbMessageType getMessageType() {
        return IdbMessageType.valueOf(messageType);
    }

    public byte[] getMessageContent() {
        return messageContent;
    }

}
