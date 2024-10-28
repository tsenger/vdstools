package de.tsenger.vdstools.vds;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

public class VdsHeaderTest {

	@Test
	public void testGetDocumentRef() {
		VdsHeader header = new VdsHeader("ALIENS_LAW");
		assertEquals(header.getDocumentRef(), 0x01fe);
	}

	@Test
	public void testSetDocumentType() {
		VdsHeader header = new VdsHeader("ARRIVAL_ATTESTATION");
		// ARRIVAL_ATTESTATION 0xfd02
		assertEquals(header.docFeatureRef, (byte) 0xfd);
		assertEquals(header.docTypeCat, (byte) 0x02);
	}

	@Test
	public void testGetRawBytes_V3() {
		VdsHeader header = new VdsHeader("RESIDENCE_PERMIT");
		// RESIDENCE_PERMIT 0xfb06
		header.signerIdentifier = "DETS";
		header.certificateReference = "32";
		header.issuingDate = LocalDate.parse("2024-09-27");
		header.sigDate = LocalDate.parse("2024-09-27");
		header.issuingCountry = "D<<";
		header.rawVersion = 0x03;
		byte[] headerBytes = header.getEncoded();
		System.out.println("Header bytes:\n" + Hex.toHexString(headerBytes));
		assertEquals("dc036abc6d32c8a72cb18d7ad88d7ad8fb06", Hex.toHexString(headerBytes));
	}

	@Test
	public void testGetRawBytes_V2() {
		VdsHeader header = new VdsHeader("ARRIVAL_ATTESTATION");
		// RESIDENCE_PERMIT 0xfb06
		header.signerIdentifier = "DETS";
		header.certificateReference = "32";
		header.issuingDate = LocalDate.parse("2024-09-27");
		header.sigDate = LocalDate.parse("2024-09-27");
		header.issuingCountry = "D<<";
		header.rawVersion = 0x02;
		byte[] headerBytes = header.getEncoded();
		System.out.println("Header bytes:\n" + Hex.toHexString(headerBytes));
		assertEquals("dc026abc6d32c8a51a1f8d7ad88d7ad8fd02", Hex.toHexString(headerBytes));
	}

}
