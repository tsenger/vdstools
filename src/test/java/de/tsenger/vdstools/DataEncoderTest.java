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

import javax.naming.InvalidNameException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsenger.vdstools.seals.DigitalSeal;
import de.tsenger.vdstools.seals.Feature;
import de.tsenger.vdstools.seals.VdsHeader;
import de.tsenger.vdstools.seals.VdsMessage;
import de.tsenger.vdstools.seals.VdsSignature;
import de.tsenger.vdstools.seals.VdsType;

public class DataEncoderTest{
	
	static String keyStorePassword = "vdstools";
	static String keyStoreFile = "src/test/resources/vdstools_testcerts.bks";
	static KeyStore keystore;	

	@BeforeClass
	public static void loadKeyStore() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException, NoSuchProviderException {
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
	public void testEncodeDate1_Mask0() {
		byte[] encodedDate = DataEncoder.encodeMaskedDate("1900-00-01", (byte) 0);
		System.out.println("encodedDate: " + Hex.toHexString(encodedDate));
		assertEquals("00002e7c", Hex.toHexString(encodedDate));
	}
	
	@Test
	public void testEncodeDate2_Mask0() {
		byte[] encodedDate = DataEncoder.encodeMaskedDate("2100-12-31", (byte) 0);
		System.out.println("encodedDate: " + Hex.toHexString(encodedDate));
		assertEquals("00bbde24", Hex.toHexString(encodedDate));
	}
	
	@Test
	public void testEncodeDate3_Mask0() {
		byte[] encodedDate = DataEncoder.encodeMaskedDate("0001-00-00", (byte) 0);
		System.out.println("encodedDate: " + Hex.toHexString(encodedDate));
		assertEquals("00000001", Hex.toHexString(encodedDate));
	}
	
	@Test
	public void testEncodeDate1_Mask1() {
		byte[] encodedDate = DataEncoder.encodeMaskedDate("1900-00-01", (byte) 0b11000011);
		System.out.println("encodedDate: " + Hex.toHexString(encodedDate));
		assertEquals("c3002e7c", Hex.toHexString(encodedDate));
	}
	
	@Test
	public void testEncodeDate2_Mask1() {
		byte[] encodedDate = DataEncoder.encodeMaskedDate("2100-12-31", (byte) 0b00000011);
		System.out.println("encodedDate: " + Hex.toHexString(encodedDate));
		assertEquals("03bbde24", Hex.toHexString(encodedDate));
	}
	
	@Test
	public void testEncodeDate3_Mask1() {
		byte[] encodedDate = DataEncoder.encodeMaskedDate("0001-00-00", (byte)  0xFC);
		System.out.println("encodedDate: " + Hex.toHexString(encodedDate));
		assertEquals("fc000001", Hex.toHexString(encodedDate));
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
				Hex.toHexString(vdsMessage.getRawBytes()));
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
		byte[] rawSignatureBytes = vdsSignature.getRawSignatureBytes();
		
		assertTrue(rawSignatureBytes.length*4==signer.getFieldSize());		
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
		byte[] expectedHeaderMessage = Arrays.concatenate(Hex.decode("dc036abc6d32c8a72cb1"), encodedDate, encodedDate, Hex.decode("fb0602305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306"));
		byte[] headerMessage = Arrays.copyOfRange(digitalSeal.getEncodedBytes(), 0, 76);
		//System.out.println(Hex.toHexString(digitalSeal.getEncodedBytes()));
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
		byte[] expectedHeaderMessage = Hex.decode("dc036abc6d32c8a72cb18d7ad88d7ad8fd020230a56213535bd4caecc87ca4ccaeb4133c133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b030859e9203833736d24");
		byte[] headerMessage = Arrays.copyOfRange(digitalSeal.getEncodedBytes(), 0, 78);
		assertTrue(Arrays.areEqual(expectedHeaderMessage, headerMessage));
	}
	
	@Test
	public void testBuildHeader_2parameter() throws KeyStoreException {
		X509Certificate cert = (X509Certificate) keystore.getCertificate("dets32");		
		LocalDate ldNow = LocalDate.now();
		byte[] encodedDate = DataEncoder.encodeDate(ldNow);
		
		VdsHeader vdsHeader = DataEncoder.buildHeader(VdsType.RESIDENCE_PERMIT, cert );
		byte[] headerBytes = vdsHeader.getRawBytes();		
		byte[] expectedHeaderBytes = Arrays.concatenate(Hex.decode("dc036abc6d32c8a72cb1"), encodedDate, encodedDate, Hex.decode("fb06"));
		assertTrue(Arrays.areEqual(expectedHeaderBytes, headerBytes));		
	}
	
	@Test
	public void testBuildHeader_3parameter() throws KeyStoreException {
		X509Certificate cert = (X509Certificate) keystore.getCertificate("dets32");
				
		LocalDate ldNow = LocalDate.now();
		byte[] encodedDate = DataEncoder.encodeDate(ldNow);
		
		VdsHeader vdsHeader = DataEncoder.buildHeader(VdsType.RESIDENCE_PERMIT, cert, "XYZ" );
		byte[] headerBytes = vdsHeader.getRawBytes();
		
		byte[] expectedHeaderBytes = Arrays.concatenate(Hex.decode("dc03ed586d32c8a72cb1"), encodedDate, encodedDate, Hex.decode("fb06"));
		assertTrue(Arrays.areEqual(expectedHeaderBytes, headerBytes));		
	}
	
	@Test
	public void testBuildHeader_4parameter() throws KeyStoreException {
		X509Certificate cert = (X509Certificate) keystore.getCertificate("dets32");	

		LocalDate ldate = LocalDate.parse("2016-08-16");
		byte[] issuingDate = DataEncoder.encodeDate(ldate);
		
		LocalDate ldNow = LocalDate.now();
		byte[] signDate = DataEncoder.encodeDate(ldNow);
		
		VdsHeader vdsHeader = DataEncoder.buildHeader(VdsType.RESIDENCE_PERMIT, cert, "XYZ", (byte)0x03, ldate);
		byte[] headerBytes = vdsHeader.getRawBytes();
		
		byte[] expectedHeaderBytes = Arrays.concatenate(Hex.decode("dc03ed586d32c8a72cb1"), issuingDate, signDate, Hex.decode("fb06"));
		assertTrue(Arrays.areEqual(expectedHeaderBytes, headerBytes));		
	}
	
	@Test
	public void testBuildHeader_4parameterV2() throws KeyStoreException {
		X509Certificate cert = (X509Certificate) keystore.getCertificate("dets32");		

		LocalDate ldate = LocalDate.parse("2016-08-16");
		byte[] issuingDate = DataEncoder.encodeDate(ldate);
		
		LocalDate ldNow = LocalDate.now();
		byte[] signDate = DataEncoder.encodeDate(ldNow);
		
		VdsHeader vdsHeader = DataEncoder.buildHeader(VdsType.TEMP_PASSPORT, cert, "XYZ", (byte)0x02, ldate);
		byte[] headerBytes = vdsHeader.getRawBytes();
		byte[] expectedHeaderBytes = Arrays.concatenate(Hex.decode("dc02ed586d32c8a51a1f"), issuingDate, signDate, Hex.decode("f60d"));
		assertTrue(Arrays.areEqual(expectedHeaderBytes, headerBytes));		
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
