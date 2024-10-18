package de.tsenger.vdstools.idb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

public class IdbMessageGroupTest {

	@Test
	public void testConstructorEmpty() {
		IdbMessageGroup messageGroup = new IdbMessageGroup();
		assertNotNull(messageGroup);
	}

	@Test
	public void testConstructorIdbMessage() {
		IdbMessage message = new IdbMessage(IdbMessageType.CAN, Hex.decode("a0a1a2a3a4a5a6a7a8a9aa"));
		IdbMessageGroup messageGroup = new IdbMessageGroup(message);
		assertNotNull(messageGroup);
		assertEquals(1, messageGroup.getMessagesList().size());
	}

	@Test
	public void testAddMessage() {
		IdbMessage message = new IdbMessage(IdbMessageType.MRZ_TD1, Hex.decode("b0b1b2b3b4b5b6b7b8b9babbbcbdbebf"));
		IdbMessageGroup messageGroup = new IdbMessageGroup();
		messageGroup.addMessage(message);
		assertEquals(1, messageGroup.getMessagesList().size());
	}

	@Test
	public void testGetMessagesList() {
		IdbMessage message = new IdbMessage(IdbMessageType.CAN, Hex.decode("a0a1a2a3a4a5a6a7a8a9aa"));
		IdbMessage message2 = new IdbMessage(IdbMessageType.MRZ_TD1, Hex.decode("b0b1b2b3b4b5b6b7b8b9babbbcbdbebf"));
		IdbMessageGroup messageGroup = new IdbMessageGroup(message);
		messageGroup.addMessage(message2);
		assertEquals(2, messageGroup.getMessagesList().size());
		List<IdbMessage> messageList = messageGroup.getMessagesList();
		assertEquals(IdbMessageType.CAN, messageList.get(0).getMessageType());
		assertEquals(IdbMessageType.MRZ_TD1, messageList.get(1).getMessageType());
	}

	@Test
	public void testGetEncoded() throws IOException {
		IdbMessage message1 = new IdbMessage(IdbMessageType.CAN, Hex.decode("a0a1a2a3a4a5a6a7a8a9aa"));
		IdbMessage message2 = new IdbMessage(IdbMessageType.MRZ_TD1, Hex.decode("b0b1b2b3b4b5b6b7b8b9babbbcbdbebf"));
		IdbMessageGroup messageGroup = new IdbMessageGroup();
		messageGroup.addMessage(message1);
		messageGroup.addMessage(message2);
		assertEquals("611f090ba0a1a2a3a4a5a6a7a8a9aa0710b0b1b2b3b4b5b6b7b8b9babbbcbdbebf",
				Hex.toHexString(messageGroup.getEncoded()));
	}

	@Test
	public void testFromByteArray() throws IOException {
		IdbMessageGroup messageGroup = IdbMessageGroup
				.fromByteArray(Hex.decode("611f090ba0a1a2a3a4a5a6a7a8a9aa0710b0b1b2b3b4b5b6b7b8b9babbbcbdbebf"));
		List<IdbMessage> messageList = messageGroup.getMessagesList();
		assertEquals(IdbMessageType.CAN, messageList.get(0).getMessageType());
		assertEquals(IdbMessageType.MRZ_TD1, messageList.get(1).getMessageType());
	}

}
