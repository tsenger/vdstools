package de.tsenger.vdstools.idb;

import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.DerTlv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IdbMessageGroup {
	public final static byte TAG = 0x61;

	private final List<IdbMessage> messagesList = new ArrayList<>();

	public IdbMessageGroup() {
	}

	public IdbMessageGroup(IdbMessage idbMessage) {
		addMessage(idbMessage);
	}

	public void addMessage(IdbMessage idbMessage) {
		messagesList.add(idbMessage);
	}

	public List<IdbMessage> getMessagesList() {
		return messagesList;
	}

	public byte[] getEncoded() throws IOException {
		ByteArrayOutputStream messages = new ByteArrayOutputStream();
		for (IdbMessage message : messagesList) {
			messages.write(message.getEncoded());
		}
		return new DerTlv(TAG, messages.toByteArray()).getEncoded();
	}

	public static IdbMessageGroup fromByteArray(byte[] rawBytes) throws IOException {
		if (rawBytes[0] != TAG) {
			throw new IllegalArgumentException(String
					.format("IdbMessageGroup shall have tag %2X, but tag %2X was found instead.", TAG, rawBytes[0]));

		}
		rawBytes = DerTlv.fromByteArray(rawBytes).getValue();
		IdbMessageGroup messageGroup = new IdbMessageGroup();
		List<DerTlv> derTlvMessagesList = DataParser.parseDerTLvs(rawBytes);
		for (DerTlv derTlvMessage : derTlvMessagesList) {
			messageGroup.addMessage(IdbMessage.fromDerTlv(derTlvMessage));
		}
		return messageGroup;
	}

}
