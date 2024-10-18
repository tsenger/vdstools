package de.tsenger.vdstools.idb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

public class IdbHeaderTest {

	@Test
	public void testConstructor_minimal() throws IOException {
		IdbHeader header = new IdbHeader("D<<");
		assertEquals("6abc", Hex.toHexString(header.getEncoded()));
	}

	@Test
	public void testConstructor_full() throws IOException {
		IdbHeader header = new IdbHeader("D<<", IdbSignatureAlgorithm.SHA256_WITH_ECDSA, new byte[] { 1, 2, 3, 4, 5 },
				"2014-10-18");
		assertEquals("6abc010102030405009b5d7e", Hex.toHexString(header.getEncoded()));
	}

	@Test
	public void testGetCountryIdentifier() {
		IdbHeader header = IdbHeader.fromByteArray(Hex.decode("6abc010102030405009b5d7e"));
		assertEquals("D<<", header.getCountryIdentifier());
	}

	@Test
	public void testGetSignatureAlgorithm() {
		IdbHeader header = IdbHeader.fromByteArray(Hex.decode("6abc010102030405009b5d7e"));
		assertEquals(IdbSignatureAlgorithm.SHA256_WITH_ECDSA, header.getSignatureAlgorithm());
	}

	@Test
	public void testGetSignatureAlgorithm_null() {
		IdbHeader header = IdbHeader.fromByteArray(Hex.decode("6abc"));
		assertNull(header.getSignatureAlgorithm());
	}

	@Test
	public void testGetCertificateReference() {
		IdbHeader header = IdbHeader.fromByteArray(Hex.decode("6abc010102030405009b5d7e"));
		assertEquals("0102030405", Hex.toHexString(header.getCertificateReference()));
	}

	@Test
	public void testGetCertificateReference_null() {
		IdbHeader header = IdbHeader.fromByteArray(Hex.decode("6abc"));
		assertNull(header.getCertificateReference());
	}

	@Test
	public void testGetSignatureCreationDate() {
		IdbHeader header = IdbHeader.fromByteArray(Hex.decode("6abc010102030405009b5d7e"));
		assertEquals("2014-10-18", header.getSignatureCreationDate());
	}

	@Test
	public void testGetSignatureCreationDate_null() {
		IdbHeader header = IdbHeader.fromByteArray(Hex.decode("6abc"));
		assertNull(header.getSignatureCreationDate());
	}

	@Test
	public void testFromByteArray_minimal() {
		IdbHeader header = IdbHeader.fromByteArray(Hex.decode("eb11"));
		assertEquals("XKC", header.getCountryIdentifier());
		assertNull(header.getSignatureAlgorithm());
		assertNull(header.getCertificateReference());
		assertNull(header.getSignatureCreationDate());
	}

	@Test
	public void testFromByteArray_full() {
		IdbHeader header = IdbHeader.fromByteArray(Hex.decode("eb1101aabbccddee00bbddbf"));
		assertEquals("XKC", header.getCountryIdentifier());
		assertEquals(IdbSignatureAlgorithm.SHA256_WITH_ECDSA, header.getSignatureAlgorithm());
		assertEquals("aabbccddee", Hex.toHexString(header.getCertificateReference()));
		assertEquals("1999-12-31", header.getSignatureCreationDate());
	}

}
