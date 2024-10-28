package de.tsenger.vdstools.vds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.Signer;

public class DigitalSealTest {

	//@formatter:off

	static String keyStorePassword = "vdstools";
	static String keyStoreFile = "src/test/resources/vdstools_testcerts.bks";
	static KeyStore keystore;

	@BeforeClass
	public static void loadKeyStore() throws NoSuchAlgorithmException, CertificateException, IOException,
			KeyStoreException, NoSuchProviderException {
		Security.addProvider(new BouncyCastleProvider());
		keystore = KeyStore.getInstance("BKS", "BC");
		FileInputStream fis = new FileInputStream(keyStoreFile);
		keystore.load(fis, keyStorePassword.toCharArray());
		fis.close();
	}

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
	
	@Test
	public void testBuildDigitalSeal() throws IOException, KeyStoreException {
		String mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<" + "6525845096USA7008038M2201018<<<<<<06";
		String passportNumber = "UFO001979";
		VdsMessage vdsMessage = new VdsMessage.Builder("RESIDENCE_PERMIT")
				.addDocumentFeature("MRZ", mrz)
				.addDocumentFeature("PASSPORT_NUMBER", passportNumber)
				.build();

		X509Certificate cert = (X509Certificate) keystore.getCertificate("dets32");
		Signer signer = new Signer(keystore, keyStorePassword, "dets32");

		LocalDate ldNow = LocalDate.now();
		byte[] encodedDate = DataEncoder.encodeDate(ldNow);

		VdsHeader vdsHeader = new VdsHeader.Builder(vdsMessage.getVdsType())
				.setSignerCertRef(cert, true)
				.build();
		DigitalSeal digitalSeal = new DigitalSeal.Builder()
				.setHeader(vdsHeader)
				.setMessage(vdsMessage)
				.setSigner(signer)
				.build();
		assertNotNull(digitalSeal);
		byte[] expectedHeaderMessage = Arrays.concatenate(Hex.decode("dc036abc6d32c8a72cb1"), encodedDate, encodedDate,
				Hex.decode(
						"fb0602305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306"));
		byte[] headerMessage = Arrays.copyOfRange(digitalSeal.getEncoded(), 0, 76);
		// System.out.println(Hex.toHexString(digitalSeal.getEncodedBytes()));
		assertTrue(Arrays.areEqual(expectedHeaderMessage, headerMessage));
	}

	@Test
	public void testBuildDigitalSeal2() throws IOException {
		String mrz = "MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<" + "6525845096USA7008038M2201018<<<<<<06";
		String azr = "ABC123456DEF";
		VdsMessage vdsMessage = new VdsMessage.Builder("ARRIVAL_ATTESTATION")
				.addDocumentFeature("MRZ", mrz)
				.addDocumentFeature("AZR", azr)
				.build();

		Signer signer = new Signer(keystore, keyStorePassword, "dets32");

		VdsHeader header = new VdsHeader.Builder("ARRIVAL_ATTESTATION")
				.setIssuingCountry("D<<")
				.setSignerIdentifier("DETS")
				.setCertificateReference("32")
				.setIssuingDate(LocalDate.parse("2024-09-27"))
				.setSigDate(LocalDate.parse("2024-09-27"))
				.build();
		
		DigitalSeal digitalSeal = new DigitalSeal.Builder()
				.setHeader(header)
				.setMessage(vdsMessage)
				.setSigner(signer)
				.build();
		assertNotNull(digitalSeal);
		byte[] expectedHeaderMessage = Hex.decode(
				"dc036abc6d32c8a72cb18d7ad88d7ad8fd020230a56213535bd4caecc87ca4ccaeb4133c133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b030859e9203833736d24");
		byte[] headerMessage = Arrays.copyOfRange(digitalSeal.getEncoded(), 0, 78);
		assertTrue(Arrays.areEqual(expectedHeaderMessage, headerMessage));
	}

}