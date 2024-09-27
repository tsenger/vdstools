package de.tsenger.vdstools.seals;

import static org.junit.Assert.*;

import java.time.LocalDate;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

public class VdsHeaderTest {

	@Test
	public void testGetDocumentRef() {
		VdsHeader header = new VdsHeader();
		header.docFeatureRef = (byte) 0x3f;
		header.docTypeCat = (byte) 0x9b;
		assertEquals(header.getDocumentRef(), 0x3f9b);
	}

	@Test
	public void testSetDocumentType() {
		VdsHeader header = new VdsHeader();
		// ARRIVAL_ATTESTATION 0xfd02
		header.setDocumentType(VdsType.ARRIVAL_ATTESTATION);
		assertEquals(header.docFeatureRef, (byte) 0xfd);
		assertEquals(header.docTypeCat, (byte) 0x02);
	}

	@Test
	public void testGetRawBytes_V3() {
		VdsHeader header = new VdsHeader();
		// RESIDENCE_PERMIT 0xfb06
		header.setDocumentType(VdsType.RESIDENCE_PERMIT);
		header.signerIdentifier = "DETS";
		header.certificateReference = "32";
		header.issuingDate = LocalDate.parse("2024-09-27");
		header.sigDate = LocalDate.parse("2024-09-27");
		header.issuingCountry = "D<<";
		header.rawVersion = 0x03;
		byte [] headerBytes = header.getRawBytes();
		System.out.println("Header bytes:\n"+Hex.toHexString(headerBytes));
		assertEquals("dc036abc6d32c8a72cb18d7ad88d7ad8fb06", Hex.toHexString(headerBytes));
	}
	
	@Test
	public void testGetRawBytes_V2() {
		VdsHeader header = new VdsHeader();
		// RESIDENCE_PERMIT 0xfb06
		header.setDocumentType(VdsType.ARRIVAL_ATTESTATION);
		header.signerIdentifier = "DETS";
		header.certificateReference = "32";
		header.issuingDate = LocalDate.parse("2024-09-27");
		header.sigDate = LocalDate.parse("2024-09-27");
		header.issuingCountry = "D<<";
		header.rawVersion = 0x02;
		byte [] headerBytes = header.getRawBytes();
		System.out.println("Header bytes:\n"+Hex.toHexString(headerBytes));
		assertEquals("dc026abc6d32c8a51a1f8d7ad88d7ad8fd02", Hex.toHexString(headerBytes));
	}

}
