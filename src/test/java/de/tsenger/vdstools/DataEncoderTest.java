package de.tsenger.vdstools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.naming.InvalidNameException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsenger.vdstools.vds.Feature;
import de.tsenger.vdstools.vds.VdsHeader;
import de.tsenger.vdstools.vds.VdsMessage;
import de.tsenger.vdstools.vds.VdsSignature;
import de.tsenger.vdstools.vds.VdsType;
import de.tsenger.vdstools.vds.seals.DigitalSeal;

public class DataEncoderTest {

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
	public void testEncodeDate_Now() {
		LocalDate ldNow = LocalDate.now();
		System.out.println("LocalDate.now(): " + ldNow);
		byte[] encodedDate = DataEncoder.encodeDate(ldNow);
		System.out.println("encodedDate: " + Hex.toHexString(encodedDate));
		assertEquals(ldNow, DataParser.decodeDate(encodedDate));
	}

	@Test
	public void testEncodeDateString() throws ParseException {
		byte[] encodedDate = DataEncoder.encodeDate("1979-10-09");
		System.out.println("encodedDate: " + Hex.toHexString(encodedDate));
		assertEquals("99fdcb", Hex.toHexString(encodedDate));
	}

	@Test
	public void testEncodeMaskedDate1() {
		byte[] encodedDate = DataEncoder.encodeMaskedDate("19xx-xx-01");
		assertEquals("c3002e7c", Hex.toHexString(encodedDate));
	}

	@Test
	public void testEncodeMaskedDate2() {
		byte[] encodedDate = DataEncoder.encodeMaskedDate("201x-04-XX");
		assertEquals("313d10da", Hex.toHexString(encodedDate));
	}

	@Test
	public void testEncodeMaskedDate3() {
		byte[] encodedDate = DataEncoder.encodeMaskedDate("1900-xx-xx");
		assertEquals("f000076c", Hex.toHexString(encodedDate));
	}

	@Test
	public void testEncodeMaskedDate4() {
		byte[] encodedDate = DataEncoder.encodeMaskedDate("1999-12-31");
		assertEquals("00bbddbf", Hex.toHexString(encodedDate));
	}

	@Test
	public void testEncodeMaskedDate5() {
		byte[] encodedDate = DataEncoder.encodeMaskedDate("xxxx-xx-xx");
		assertEquals("ff000000", Hex.toHexString(encodedDate));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEncodeMaskedDate6_invalidFormat() {
		byte[] encodedDate = DataEncoder.encodeMaskedDate("19-03-2010");
		assertEquals("ff000000", Hex.toHexString(encodedDate));
	}

	@Test
	public void testEncodeDateTime1() {
		LocalDateTime dateTime = LocalDateTime.parse("1957-03-25T08:15:22");
		byte[] dateTimeBytes = DataEncoder.encodeDateTime(dateTime);
		assertEquals("02f527bf25b2", Hex.toHexString(dateTimeBytes));
	}

	@Test
	public void testEncodeDateTime2() {
		LocalDateTime dateTime = LocalDateTime.parse("2030-12-01T00:00:00");
		byte[] dateTimeBytes = DataEncoder.encodeDateTime(dateTime);
		assertEquals("0aecc4c7fb80", Hex.toHexString(dateTimeBytes));
	}

	@Test
	public void testEncodeDateTime3() {
		LocalDateTime dateTime = LocalDateTime.parse("0001-01-01T00:00:00");
		byte[] dateTimeBytes = DataEncoder.encodeDateTime(dateTime);
		assertEquals("00eb28c03640", Hex.toHexString(dateTimeBytes));
	}

	@Test
	public void testEncodeDateTime4() {
		LocalDateTime dateTime = LocalDateTime.parse("9999-12-31T23:59:59");
		byte[] dateTimeBytes = DataEncoder.encodeDateTime(dateTime);
		assertEquals("0b34792d9777", Hex.toHexString(dateTimeBytes));
	}

	@Test
	public void testRegex() {
		String dateString = "1979-10-09";
		String formattedDate = dateString.replaceAll("(.{4})-(.{2})-(.{2})", "$2$3$1");
		assertEquals("10091979", formattedDate);
	}

	@Test
	public void testEncodeDate_String() throws ParseException {
		byte[] encodedDate = DataEncoder.encodeDate("2024-09-27");
		System.out.println("encodedDate: " + Hex.toHexString(encodedDate));

		assertEquals("8d7ad8", Hex.toHexString(encodedDate));
	}

	@Test
	public void testGetSignerCertRef() throws InvalidNameException, KeyStoreException {
		X509Certificate cert = (X509Certificate) keystore.getCertificate("dets32");
		String signerCertRef[] = DataEncoder.getSignerCertRef(cert);

		assertEquals("DETS", signerCertRef[0]);
		assertEquals("32", signerCertRef[1]);
	}

	@Test
	public void testBuildVdsMessage() {
		String mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<" + "6525845096USA7008038M2201018<<<<<<06";
		String passportNumber = "UFO001979";
		VdsMessage vdsMessage = new VdsMessage(VdsType.RESIDENCE_PERMIT);
		vdsMessage.addDocumentFeature(Feature.MRZ, mrz);
		vdsMessage.addDocumentFeature(Feature.PASSPORT_NUMBER, passportNumber);

		assertEquals(
				"02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306",
				Hex.toHexString(vdsMessage.getEncoded()));
	}

	@Test
	public void testCreateVdsSignature() {
		VdsHeader vdsHeader = buildHeader();
		String mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<" + "6525845096USA7008038M2201018<<<<<<06";
		String passportNumber = "UFO001979";
		VdsMessage vdsMessage = new VdsMessage(VdsType.RESIDENCE_PERMIT);
		vdsMessage.addDocumentFeature(Feature.MRZ, mrz);
		vdsMessage.addDocumentFeature(Feature.PASSPORT_NUMBER, passportNumber);

		Signer signer = new Signer(keystore, keyStorePassword, "dets32");
		VdsSignature vdsSignature = DataEncoder.createVdsSignature(vdsHeader, vdsMessage, signer);
		byte[] plainSignatureBytes = vdsSignature.getPlainSignatureBytes();

		assertTrue(plainSignatureBytes.length * 4 == signer.getFieldSize());
	}

	@Test
	public void testBuildDigitalSeal() throws IOException, KeyStoreException {
		String mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<" + "6525845096USA7008038M2201018<<<<<<06";
		String passportNumber = "UFO001979";
		VdsMessage vdsMessage = new VdsMessage(VdsType.RESIDENCE_PERMIT);
		vdsMessage.addDocumentFeature(Feature.MRZ, mrz);
		vdsMessage.addDocumentFeature(Feature.PASSPORT_NUMBER, passportNumber);

		X509Certificate cert = (X509Certificate) keystore.getCertificate("dets32");
		Signer signer = new Signer(keystore, keyStorePassword, "dets32");

		LocalDate ldNow = LocalDate.now();
		byte[] encodedDate = DataEncoder.encodeDate(ldNow);

		DigitalSeal digitalSeal = DataEncoder.buildDigitalSeal(vdsMessage, cert, signer);
		assertNotNull(digitalSeal);
		byte[] expectedHeaderMessage = Arrays.concatenate(Hex.decode("dc036abc6d32c8a72cb1"), encodedDate, encodedDate,
				Hex.decode(
						"fb0602305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306"));
		byte[] headerMessage = Arrays.copyOfRange(digitalSeal.getEncodedBytes(), 0, 76);
		// System.out.println(Hex.toHexString(digitalSeal.getEncodedBytes()));
		assertTrue(Arrays.areEqual(expectedHeaderMessage, headerMessage));
	}

	@Test
	public void testBuildDigitalSeal2() throws IOException {
		String mrz = "MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<" + "6525845096USA7008038M2201018<<<<<<06";
		String azr = "ABC123456DEF";
		VdsMessage vdsMessage = new VdsMessage(VdsType.ARRIVAL_ATTESTATION);
		vdsMessage.addDocumentFeature(Feature.MRZ, mrz);
		vdsMessage.addDocumentFeature(Feature.AZR, azr);

		Signer signer = new Signer(keystore, keyStorePassword, "dets32");

		DigitalSeal digitalSeal = DataEncoder.buildDigitalSeal(buildHeader(), vdsMessage, signer);
		assertNotNull(digitalSeal);
		byte[] expectedHeaderMessage = Hex.decode(
				"dc036abc6d32c8a72cb18d7ad88d7ad8fd020230a56213535bd4caecc87ca4ccaeb4133c133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b030859e9203833736d24");
		byte[] headerMessage = Arrays.copyOfRange(digitalSeal.getEncodedBytes(), 0, 78);
		assertTrue(Arrays.areEqual(expectedHeaderMessage, headerMessage));
	}

	@Test
	public void testBuildHeader_2parameter() throws KeyStoreException {
		X509Certificate cert = (X509Certificate) keystore.getCertificate("dets32");
		LocalDate ldNow = LocalDate.now();
		byte[] encodedDate = DataEncoder.encodeDate(ldNow);

		VdsHeader vdsHeader = DataEncoder.buildHeader(VdsType.RESIDENCE_PERMIT, cert);
		byte[] headerBytes = vdsHeader.getEncoded();
		byte[] expectedHeaderBytes = Arrays.concatenate(Hex.decode("dc036abc6d32c8a72cb1"), encodedDate, encodedDate,
				Hex.decode("fb06"));
		assertTrue(Arrays.areEqual(expectedHeaderBytes, headerBytes));
	}

	@Test
	public void testBuildHeader_3parameter() throws KeyStoreException {
		X509Certificate cert = (X509Certificate) keystore.getCertificate("dets32");

		LocalDate ldNow = LocalDate.now();
		byte[] encodedDate = DataEncoder.encodeDate(ldNow);

		VdsHeader vdsHeader = DataEncoder.buildHeader(VdsType.RESIDENCE_PERMIT, cert, "XYZ");
		byte[] headerBytes = vdsHeader.getEncoded();

		byte[] expectedHeaderBytes = Arrays.concatenate(Hex.decode("dc03ed586d32c8a72cb1"), encodedDate, encodedDate,
				Hex.decode("fb06"));
		assertTrue(Arrays.areEqual(expectedHeaderBytes, headerBytes));
	}

	@Test
	public void testBuildHeader_4parameter() throws KeyStoreException {
		X509Certificate cert = (X509Certificate) keystore.getCertificate("dets32");

		LocalDate ldate = LocalDate.parse("2016-08-16");
		byte[] issuingDate = DataEncoder.encodeDate(ldate);

		LocalDate ldNow = LocalDate.now();
		byte[] signDate = DataEncoder.encodeDate(ldNow);

		VdsHeader vdsHeader = DataEncoder.buildHeader(VdsType.RESIDENCE_PERMIT, cert, "XYZ", (byte) 0x03, ldate);
		byte[] headerBytes = vdsHeader.getEncoded();

		byte[] expectedHeaderBytes = Arrays.concatenate(Hex.decode("dc03ed586d32c8a72cb1"), issuingDate, signDate,
				Hex.decode("fb06"));
		assertTrue(Arrays.areEqual(expectedHeaderBytes, headerBytes));
	}

	@Test
	public void testBuildHeader_4parameterV2() throws KeyStoreException {
		X509Certificate cert = (X509Certificate) keystore.getCertificate("dets32");

		LocalDate ldate = LocalDate.parse("2016-08-16");
		byte[] issuingDate = DataEncoder.encodeDate(ldate);

		LocalDate ldNow = LocalDate.now();
		byte[] signDate = DataEncoder.encodeDate(ldNow);

		VdsHeader vdsHeader = DataEncoder.buildHeader(VdsType.TEMP_PASSPORT, cert, "XYZ", (byte) 0x02, ldate);
		byte[] headerBytes = vdsHeader.getEncoded();
		byte[] expectedHeaderBytes = Arrays.concatenate(Hex.decode("dc02ed586d32c8a51a1f"), issuingDate, signDate,
				Hex.decode("f60d"));
		assertTrue(Arrays.areEqual(expectedHeaderBytes, headerBytes));
	}

	@Test
	public void testZip() throws IOException {
		byte[] bytesToCompress = Hex.decode(
				"61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7");
		byte[] compressedBytes = DataEncoder.zip(bytesToCompress);
		System.out.println("Compressed: " + Hex.toHexString(compressedBytes));
		assertEquals(
				"78da014e00b1ff61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c743d4280b",
				Hex.toHexString(compressedBytes));
	}

	private VdsHeader buildHeader() {
		VdsHeader header = new VdsHeader();
		// RESIDENCE_PERMIT 0xfb06
		header.setDocumentType(VdsType.ARRIVAL_ATTESTATION);
		header.signerIdentifier = "DETS";
		header.certificateReference = "32";
		header.issuingDate = LocalDate.parse("2024-09-27");
		header.sigDate = LocalDate.parse("2024-09-27");
		header.issuingCountry = "D<<";
		header.rawVersion = 0x03;
		return header;
	}

}
