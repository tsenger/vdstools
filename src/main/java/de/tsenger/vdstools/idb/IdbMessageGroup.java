package de.tsenger.vdstools.idb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.DerTlv;

public class IdbMessageGroup {
	public final static byte TAG = 0x61;

	private List<IdbMessage> messagesList = new ArrayList<IdbMessage>();

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
		if (rawBytes[0] == TAG) {
			rawBytes = DerTlv.fromByteArray(rawBytes).getValue();
		}
		IdbMessageGroup messageGroup = new IdbMessageGroup();
		List<DerTlv> derTlvMessagesList = DataParser.parseDerTLvs(rawBytes);
		for (DerTlv derTlvMessage : derTlvMessagesList) {
			messageGroup.addMessage(IdbMessage.fromDerTlv(derTlvMessage));
		}
		return messageGroup;
	}

}
