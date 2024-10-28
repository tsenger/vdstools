package de.tsenger.vdstools.vds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

public class DigitalSealTest {

	@Test
	public void testParseSocialInsurranceCard() throws IOException {
		DigitalSeal seal = DigitalSeal.fromByteArray(VdsRawBytes.socialInsurance);
		assertEquals("65170839J003", seal.getFeature("SOCIAL_INSURANCE_NUMBER"));
		assertEquals("Perschweiß", seal.getFeature("SURNAME"));
		assertEquals("Oscar", seal.getFeature("FIRST_NAME"));
		assertEquals("Jâcobénidicturius", seal.getFeature("BIRTH_NAME"));
	}

	@Test
	public void testParseArrivalAttestationV02() throws IOException {
		DigitalSeal seal = DigitalSeal.fromByteArray(VdsRawBytes.arrivalAttestationV02);
		assertEquals("MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06",
				seal.getFeature("MRZ"));
		assertEquals("ABC123456DEF", seal.getFeature("AZR"));
		assertNull(seal.getFeature("FIRST_NAME"));
	}

	@Test
	public void testParseResidentPermit() throws IOException {
		DigitalSeal seal = DigitalSeal.fromByteArray(VdsRawBytes.residentPermit);
		assertEquals("ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06",
				seal.getFeature("MRZ"));
		assertEquals("UFO001979", seal.getFeature("PASSPORT_NUMBER"));
	}

	@Test
	public void testParseSupplementSheet() throws IOException {
		DigitalSeal seal = DigitalSeal.fromByteArray(VdsRawBytes.supplementSheet);
		assertEquals("ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06",
				seal.getFeature("MRZ"));
		assertEquals("PA0000005", seal.getFeature("SHEET_NUMBER"));
	}

	@Test
	public void testEmergencyTravelDoc() throws IOException {
		DigitalSeal seal = DigitalSeal.fromByteArray(VdsRawBytes.emergenyTravelDoc);
		assertEquals("I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06",
				seal.getFeature("MRZ"));
	}

	@Test
	public void testParseAddressStickerId() throws IOException {
		DigitalSeal seal = DigitalSeal.fromByteArray(VdsRawBytes.addressStickerId);
		assertEquals("T2000AK47", seal.getFeature("DOCUMENT_NUMBER"));
		assertEquals("05314000", seal.getFeature("AGS"));
		assertEquals("53175HEINEMANNSTR11", seal.getFeature("ADDRESS"));
	}

	@Test
	public void testParseAddressStickerPassport() throws IOException {
		DigitalSeal seal = DigitalSeal.fromByteArray(VdsRawBytes.addressStickerPassport);
		assertEquals("PA5500K11", seal.getFeature("DOCUMENT_NUMBER"));
		assertEquals("03359010", seal.getFeature("AGS"));
		assertEquals("21614", seal.getFeature("POSTAL_CODE"));
	}

	@Test
	public void testParseVisa() throws IOException {
		DigitalSeal seal = DigitalSeal.fromByteArray(VdsRawBytes.visa_224bitSig);
		assertEquals("VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<1234567XY7GBR5203116M2005250", seal.getFeature("MRZ_MRVB"));
		assertEquals("47110815P", seal.getFeature("PASSPORT_NUMBER"));
		assertEquals("a00000", Hex.toHexString((byte[]) seal.getFeature("DURATION_OF_STAY")));
	}

	@Test
	public void testParseFictionCert() throws IOException {
		DigitalSeal seal = DigitalSeal.fromByteArray(VdsRawBytes.fictionCert);
		assertEquals("NFD<<MUSTERMANN<<CLEOPATRE<<<<<<<<<<L000000007TUR8308126F2701312T2611011",
				seal.getFeature("MRZ"));
		assertEquals("X98723021", seal.getFeature("PASSPORT_NUMBER"));
		assertEquals("160113000085", seal.getFeature("AZR"));
	}

	@Test
	public void testParseTempPerso() throws IOException {
		DigitalSeal seal = DigitalSeal.fromByteArray(VdsRawBytes.tempPerso);
		assertEquals("ITD<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<D000000001D<<8308126<2701312<<<<<<<0",
				seal.getFeature("MRZ"));
	}

	@Test
	public void testParseTempPassport() throws IOException {
		DigitalSeal seal = DigitalSeal.fromByteArray(VdsRawBytes.tempPassport);
		assertEquals("PPD<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<<<<<<<<<A000000000D<<8308126<2710316<<<<<<<<<<<<<<<8",
				seal.getFeature("MRZ"));
	}

	@Test
	public void testGetEncodedBytes_rp() throws IOException {
		DigitalSeal seal = DigitalSeal.fromByteArray(VdsRawBytes.residentPermit);
		assertTrue(Arrays.areEqual(VdsRawBytes.residentPermit, seal.getEncoded()));
	}

	@Test
	public void testGetEncodedBytes_aa() throws IOException {
		DigitalSeal seal = DigitalSeal.fromByteArray(VdsRawBytes.arrivalAttestation);
		System.out.println(Hex.toHexString(VdsRawBytes.arrivalAttestation));
		System.out.println(Hex.toHexString(seal.getEncoded()));
		assertTrue(Arrays.areEqual(VdsRawBytes.arrivalAttestation, seal.getEncoded()));
	}

	@Test
	public void testGetEncodedBytes_aav2() throws IOException {
		DigitalSeal seal = DigitalSeal.fromByteArray(VdsRawBytes.arrivalAttestationV02);
		System.out.println(Hex.toHexString(VdsRawBytes.arrivalAttestationV02));
		System.out.println(Hex.toHexString(seal.getEncoded()));
		assertTrue(Arrays.areEqual(VdsRawBytes.arrivalAttestationV02, seal.getEncoded()));
	}

	@Test
	public void testGetEncodedBytes_fc() throws IOException {
		DigitalSeal seal = DigitalSeal.fromByteArray(VdsRawBytes.fictionCert);
		assertTrue(Arrays.areEqual(VdsRawBytes.fictionCert, seal.getEncoded()));
	}

	@Test
	public void testgetRawString1() throws IOException {
		DigitalSeal seal = DigitalSeal.fromByteArray(VdsRawBytes.arrivalAttestationV02);
		String rawString = seal.getRawString();
		DigitalSeal seal2 = DigitalSeal.fromRawString(rawString);
		assertEquals(rawString, seal2.getRawString());
		assertEquals(Hex.toHexString(VdsRawBytes.arrivalAttestationV02), Hex.toHexString(seal2.getEncoded()));
	}

	@Test
	public void testgetRawString2() throws IOException {
		DigitalSeal seal = DigitalSeal.fromByteArray(VdsRawBytes.tempPerso);
		String rawString = seal.getRawString();
		DigitalSeal seal2 = DigitalSeal.fromRawString(rawString);
		assertEquals(rawString, seal2.getRawString());
		assertEquals(Hex.toHexString(VdsRawBytes.tempPerso), Hex.toHexString(seal2.getEncoded()));
	}

}
