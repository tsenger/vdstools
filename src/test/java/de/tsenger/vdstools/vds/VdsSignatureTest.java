package de.tsenger.vdstools.vds;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.KeyStore;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

public class VdsSignatureTest {

	static String keyStorePassword = "vdstools";
	static String keyStoreFile = "src/test/resources/vdstools_testcerts.bks";
	static KeyStore keystore;

	static final byte[] plainSignature = Hex.decode(
			"3c8b104fd4a8ad11157f87dadd05407f0cefa3ad0155c1179765933089896357e1b6fdbb3b2b003d6ee34875d6db833e05fffe9d99378eb01ae988c638c2eb27");

	@Test
	public void testGetPlainSignatureBytes() {
		VdsSignature signature = new VdsSignature(plainSignature);
		assertEquals(
				"3c8b104fd4a8ad11157f87dadd05407f0cefa3ad0155c1179765933089896357e1b6fdbb3b2b003d6ee34875d6db833e05fffe9d99378eb01ae988c638c2eb27",
				Hex.toHexString(signature.getPlainSignatureBytes()));
	}

	@Test
	public void testGetDerSignatureBytes() {
		VdsSignature signature = new VdsSignature(plainSignature);
		assertEquals(
				"304502203c8b104fd4a8ad11157f87dadd05407f0cefa3ad0155c1179765933089896357022100e1b6fdbb3b2b003d6ee34875d6db833e05fffe9d99378eb01ae988c638c2eb27",
				Hex.toHexString(signature.getDerSignatureBytes()));
	}

	@Test
	public void testFromByteArray() throws IOException {
		byte[] vdsSigBytes = new byte[] { (byte) 0xff, 6, 1, 2, 3, 4, 5, 6 };
		VdsSignature signature = VdsSignature.fromByteArray(vdsSigBytes);
		assertEquals("010203040506", Hex.toHexString(signature.getPlainSignatureBytes()));
		assertEquals("300a02030102030203040506", Hex.toHexString(signature.getDerSignatureBytes()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFromByteArray_IllegalArgumentException() throws IOException {
		VdsSignature.fromByteArray(plainSignature);
	}
}
