package de.tsenger.vdstools.idb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.DerTlv;

public class IdbMessageGroup {
	public final static byte TAG = 0x61;

	List<IdbMessage> messagesList;

	public IdbMessageGroup() {
	}

	public IdbMessageGroup(IdbMessage idbMessage) {
		addMessage(idbMessage);
	}

	public void addMessage(IdbMessage idbMessage) {
		messagesList.add(idbMessage);
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

		// TODO find "cleaner" solution, duplicated in DataParser
		ByteBuffer rawData = ByteBuffer.wrap(rawBytes);
		while (rawData.hasRemaining()) {
			byte tag = rawData.get();

			int le = rawData.get() & 0xff;
			if (le == 0x81) {
				le = rawData.get() & 0xff;
			} else if (le == 0x82) {
				le = ((rawData.get() & 0xff) * 0x100) + (rawData.get() & 0xff);
			} else if (le == 0x83) {
				le = ((rawData.get() & 0xff) * 0x1000) + ((rawData.get() & 0xff) * 0x100) + (rawData.get() & 0xff);
			} else if (le > 0x7F) {
				Logger.error(String.format("can't decode length: 0x%02X", le));
				throw new IllegalArgumentException(String.format("can't decode length: 0x%02X", le));
			}
			byte[] val = DataParser.getFromByteBuffer(rawData, le);
			messageGroup.addMessage(new IdbMessage(IdbMessageType.valueOf(tag), val));
		}
		return messageGroup;
	}

}
