package de.tsenger.vdstools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
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
import java.util.Locale;

import javax.naming.InvalidNameException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tinylog.Logger;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.seals.DigitalSeal;
import de.tsenger.vdstools.seals.Feature;
import de.tsenger.vdstools.seals.VdsHeader;
import de.tsenger.vdstools.seals.VdsMessage;
import de.tsenger.vdstools.seals.VdsSignature;
import de.tsenger.vdstools.seals.VdsType;
import junit.framework.TestCase;

public class DataEncoderTest{
	
	String keyStorePassword = "jFd853v_+RL4";
	String keyStoreFile = "src/test/resources/sealgen_ds.bks";

	@BeforeClass
	public static void loadBC() {
		System.out.println("BEFORE CLASS");
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
	public void testIsoCountryCode() {
		Locale locale = new Locale("","DE");
	    System.out.println("Country=" + locale.getISO3Country());
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
		System.out.println(Hex.toHexString(digitalSeal.getEncodedBytes()));
	}


	private VdsHeader buildHeader() {
		VdsHeader header = new VdsHeader();
		// RESIDENCE_PERMIT 0xfb06
		header.setDocumentType(VdsType.RESIDENCE_PERMIT);
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
