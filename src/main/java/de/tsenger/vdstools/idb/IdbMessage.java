package de.tsenger.vdstools.idb;

import java.io.IOException;

import de.tsenger.vdstools.DerTlv;

public class IdbMessage {

	private byte messageType;
	private byte[] messageContent;

	public IdbMessage(IdbMessageType messageType, byte[] messageContent) {
		this.messageType = messageType.getValue();
		this.messageContent = messageContent;
	}

	public IdbMessage(byte messageType, byte[] messageContent) {
		this.messageType = messageType;
		this.messageContent = messageContent;
	}

	public static IdbMessage fromDerTlv(DerTlv derTlv) {
		IdbMessageType messageType = IdbMessageType.valueOf(derTlv.getTag());
		byte[] messageContent = derTlv.getValue();
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
