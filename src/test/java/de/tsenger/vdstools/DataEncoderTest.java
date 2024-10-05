package de.tsenger.vdstools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.naming.InvalidNameException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tinylog.Logger;

import de.tsenger.vdstools.seals.DigitalSeal;
import de.tsenger.vdstools.seals.Feature;
import de.tsenger.vdstools.seals.VdsHeader;
import de.tsenger.vdstools.seals.VdsMessage;
import de.tsenger.vdstools.seals.VdsSignature;
import de.tsenger.vdstools.seals.VdsType;

public class DataEncoderTest{
	
	String keyStorePassword = "jFd853v_+RL4";
	String keyStoreFile = "src/test/resources/sealgen_ds.bks";

	@BeforeClass
	public static void loadBC() {
		Security.addProvider(new BouncyCastleProvider());
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
	public void testEncodeDate_String() throws ParseException {
		byte[] encodedDate = DataEncoder.encodeDate("2024-09-27");
		System.out.println("encodedDate: " + Hex.toHexString(encodedDate));
		assertEquals("8d7ad8", Hex.toHexString(encodedDate));
	}

	@Test
	public void testGetSignerCertRef() throws InvalidNameException {
		X509Certificate cert = null;
		try {
			String certFilename = "src/test/resources/DETS32.crt";
			FileInputStream inStream = new FileInputStream(certFilename);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			cert = (X509Certificate) cf.generateCertificate(inStream);
		} catch (FileNotFoundException | CertificateException e) {
			fail(e.getMessage());
		}

		String signerCertRef[] = DataEncoder.getSignerCertRef(cert);
		assertEquals("DETS", signerCertRef[0]);
		assertEquals("32", signerCertRef[1]);
	}

	
	@Test
	public void testBuildVdsMessage() {
		String mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<" + "6525845096USA7008038M2201018<<<<<<06";
		String passportNumber = "UFO001979";
		HashMap<Feature, Object> featureMap = new LinkedHashMap<Feature, Object>(2);
		featureMap.put(Feature.MRZ, mrz);
		featureMap.put(Feature.PASSPORT_NUMBER, passportNumber);
		VdsMessage vdsMessage = DataEncoder.buildVdsMessage(VdsType.RESIDENCE_PERMIT, featureMap);

		System.out.println(Hex.toHexString(vdsMessage.getRawBytes()));
		assertEquals(
				"02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306",
				Hex.toHexString(vdsMessage.getRawBytes()));
	}
	
	@Test
	public void testCreateVdsSignature() {
		VdsHeader vdsHeader = buildHeader();
		
		String mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<" + "6525845096USA7008038M2201018<<<<<<06";
		String passportNumber = "UFO001979";
		HashMap<Feature, Object> featureMap = new LinkedHashMap<Feature, Object>(2);
		featureMap.put(Feature.MRZ, mrz);
		featureMap.put(Feature.PASSPORT_NUMBER, passportNumber);
		VdsMessage vdsMessage = DataEncoder.buildVdsMessage(VdsType.RESIDENCE_PERMIT, featureMap);
		
		Signer signer = new Signer(getKeystore(), keyStorePassword, "dets32");
		
		VdsSignature vdsSignature = DataEncoder.createVdsSignature(vdsHeader, vdsMessage, signer);
		byte[] rawSignatureBytes = vdsSignature.getRawSignatureBytes();
		System.out.println(Hex.toHexString(rawSignatureBytes));
		assertTrue(rawSignatureBytes.length*4==signer.getFieldSize());		
	}	
	
	@Test 
	public void testBuildDigitalSeal() throws IOException {		
		String mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<" + "6525845096USA7008038M2201018<<<<<<06";
		String passportNumber = "UFO001979";
		HashMap<Feature, Object> featureMap = new LinkedHashMap<Feature, Object>(2);
		featureMap.put(Feature.MRZ, mrz);
		featureMap.put(Feature.PASSPORT_NUMBER, passportNumber);
		
		X509Certificate cert = null;
		try {
			String certFilename = "src/test/resources/DETS32.crt";
			FileInputStream inStream = new FileInputStream(certFilename);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			cert = (X509Certificate) cf.generateCertificate(inStream);
		} catch (FileNotFoundException | CertificateException e) {
			fail(e.getMessage());
		}
		
		Signer signer = new Signer(getKeystore(), keyStorePassword, "dets32");
		
		DigitalSeal digitalSeal = DataEncoder.buildDigitalSeal(VdsType.RESIDENCE_PERMIT, featureMap, cert, signer);
		assertNotNull(digitalSeal);
		byte[] expectedHeaderMessage = Hex.decode("dc036abc6d32c8a72cb19961b89961b8fb0602305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306");
		byte[] headerMessage = Arrays.copyOfRange(digitalSeal.getEncodedBytes(), 0, 76);
		//System.out.println(Hex.toHexString(digitalSeal.getEncodedBytes()));
		assertTrue(Arrays.areEqual(expectedHeaderMessage, headerMessage));
	}
	
	@Test 
	public void testBuildDigitalSeal2() throws IOException {			
		String mrz = "MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<" + "6525845096USA7008038M2201018<<<<<<06";
		String azr = "ABC123456DEF";
		HashMap<Feature, Object> featureMap = new LinkedHashMap<Feature, Object>(2);
		featureMap.put(Feature.MRZ, mrz);
		featureMap.put(Feature.AZR, azr);
		
		Signer signer = new Signer(getKeystore(), keyStorePassword, "dets32");
		
		DigitalSeal digitalSeal = DataEncoder.buildDigitalSeal(buildHeader(), featureMap, signer);
		assertNotNull(digitalSeal);
		byte[] expectedHeaderMessage = Hex.decode("dc036abc6d32c8a72cb18d7ad88d7ad8fd020230a56213535bd4caecc87ca4ccaeb4133c133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b030859e9203833736d24");
		byte[] headerMessage = Arrays.copyOfRange(digitalSeal.getEncodedBytes(), 0, 78);
		//System.out.println(Hex.toHexString(digitalSeal.getEncodedBytes()));
		assertTrue(Arrays.areEqual(expectedHeaderMessage, headerMessage));
	}
	
	@Test
	public void testBuildHeader_2parameter() {
		X509Certificate cert = null;
		try {
			String certFilename = "src/test/resources/DETS32.crt";
			FileInputStream inStream = new FileInputStream(certFilename);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			cert = (X509Certificate) cf.generateCertificate(inStream);
		} catch (FileNotFoundException | CertificateException e) {
			fail(e.getMessage());
		}
		
		LocalDate ldNow = LocalDate.now();
		byte[] encodedDate = DataEncoder.encodeDate(ldNow);
		
		VdsHeader vdsHeader = DataEncoder.buildHeader(VdsType.RESIDENCE_PERMIT, cert );
		byte[] headerBytes = vdsHeader.getRawBytes();		
		byte[] expectedHeaderBytes = Arrays.concatenate(Hex.decode("dc036abc6d32c8a72cb1"), encodedDate, encodedDate, Hex.decode("fb06"));
		assertTrue(Arrays.areEqual(expectedHeaderBytes, headerBytes));		
	}
	
	@Test
	public void testBuildHeader_3parameter() {
		X509Certificate cert = null;
		try {
			String certFilename = "src/test/resources/DETS32.crt";
			FileInputStream inStream = new FileInputStream(certFilename);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			cert = (X509Certificate) cf.generateCertificate(inStream);
		} catch (FileNotFoundException | CertificateException e) {
			fail(e.getMessage());
		}
				
		LocalDate ldNow = LocalDate.now();
		byte[] encodedDate = DataEncoder.encodeDate(ldNow);
		
		VdsHeader vdsHeader = DataEncoder.buildHeader(VdsType.RESIDENCE_PERMIT, cert, "XYZ" );
		byte[] headerBytes = vdsHeader.getRawBytes();
		
		byte[] expectedHeaderBytes = Arrays.concatenate(Hex.decode("dc03ed586d32c8a72cb1"), encodedDate, encodedDate, Hex.decode("fb06"));
		assertTrue(Arrays.areEqual(expectedHeaderBytes, headerBytes));		
	}
	
	@Test
	public void testBuildHeader_4parameter() {
		X509Certificate cert = null;
		try {
			String certFilename = "src/test/resources/DETS32.crt";
			FileInputStream inStream = new FileInputStream(certFilename);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			cert = (X509Certificate) cf.generateCertificate(inStream);
		} catch (FileNotFoundException | CertificateException e) {
			fail(e.getMessage());
		}		

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
	public void testBuildHeader_4parameterV2() {
		X509Certificate cert = null;
		try {
			String certFilename = "src/test/resources/DETS32.crt";
			FileInputStream inStream = new FileInputStream(certFilename);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			cert = (X509Certificate) cf.generateCertificate(inStream);
		} catch (FileNotFoundException | CertificateException e) {
			fail(e.getMessage());
		}		

		LocalDate ldate = LocalDate.parse("2016-08-16");
		byte[] issuingDate = DataEncoder.encodeDate(ldate);
		
		LocalDate ldNow = LocalDate.now();
		byte[] signDate = DataEncoder.encodeDate(ldNow);
		
		VdsHeader vdsHeader = DataEncoder.buildHeader(VdsType.TEMP_PASSPORT, cert, "XYZ", (byte)0x02, ldate);
		byte[] headerBytes = vdsHeader.getRawBytes();
		System.out.println(Hex.toHexString(headerBytes));
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
	
	private KeyStore getKeystore() {
		KeyStore keystore;

		try {
			keystore = KeyStore.getInstance("BKS", "BC");
			FileInputStream fis = new FileInputStream(keyStoreFile);
			keystore.load(fis, keyStorePassword.toCharArray());
			fis.close();
			return keystore;
		} catch (KeyStoreException | NoSuchProviderException | NoSuchAlgorithmException | CertificateException
				| IOException e) {
			Logger.warn("Error while opening keystore '" + keyStoreFile + "': " + e.getMessage());
			return null;
		}
	}

}
