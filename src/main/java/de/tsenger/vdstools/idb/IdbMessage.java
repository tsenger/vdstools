package de.tsenger.vdstools.idb;

import java.io.IOException;

import de.tsenger.vdstools.DerTlv;

public class IdbMessage {

	IdbMessageType messageType;
	byte[] messageContent;

	public IdbMessage(IdbMessageType messageType, byte[] messageContent) {
		this.messageType = messageType;
		this.messageContent = messageContent;
	}

	public static IdbMessage fromByteArray(byte[] rawMessageBytes) throws IOException {
		DerTlv tlvMessage = DerTlv.fromByteArray(rawMessageBytes);
		IdbMessageType messageType = IdbMessageType.valueOf(tlvMessage.getTag());
		byte[] messageContent = tlvMessage.getValue();
		return new IdbMessage(messageType, messageContent);
	}

	public byte[] getEncoded() throws IOException {
		return new DerTlv(messageType.getValue(), messageContent).getEncoded();
	}

}
