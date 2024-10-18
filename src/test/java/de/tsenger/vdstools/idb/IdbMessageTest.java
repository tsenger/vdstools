package de.tsenger.vdstools.idb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

public class IdbMessageTest {

	//@formatter:off
	byte[] visa_content = Hex.decode(
			  "022cdd52134a74da1347c6fed95cb89f"
			+ "9fce133c133c133c133c203833734aaf"
			+ "47f0c32f1a1e20eb2625393afe310403" 
			+ "a00000050633be1fed20c6");
	
	byte[] idbMessageBytes = Hex.decode(
			  "013b022cdd52134a74da1347c6fed95c"
			+ "b89f9fce133c133c133c133c20383373"
			+ "4aaf47f0c32f1a1e20eb2625393afe31"
			+ "0403a00000050633be1fed20c6");
	//@formatter:on

	@Test
	public void testConstructor() throws IOException {
		IdbMessage message = new IdbMessage(IdbMessageType.VISA, visa_content);
		System.out.println(Hex.toHexString(message.getEncoded()));
		assertNotNull(message);
	}

	@Test
	public void testFromByteArray() throws IOException {
		IdbMessage message = IdbMessage.fromByteArray(idbMessageBytes);
		assertNotNull(message);
	}

	@Test
	public void testGetEncoded() throws IOException {
		IdbMessage message = new IdbMessage(IdbMessageType.VISA, visa_content);
		assertTrue(Arrays.equals(idbMessageBytes, message.getEncoded()));
	}

	@Test
	public void testGetMessageType() throws IOException {
		IdbMessage message = IdbMessage.fromByteArray(idbMessageBytes);
		assertEquals(IdbMessageType.VISA, message.getMessageType());
	}

	@Test
	public void testGetMessageContent() throws IOException {
		IdbMessage message = IdbMessage.fromByteArray(idbMessageBytes);
		assertTrue(Arrays.equals(visa_content, message.getMessageContent()));
	}

}
