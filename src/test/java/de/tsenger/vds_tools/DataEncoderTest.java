package de.tsenger.vds_tools;

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.naming.InvalidNameException;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.seals.Feature;
import de.tsenger.vdstools.seals.VdsHeader;
import de.tsenger.vdstools.seals.VdsMessage;
import de.tsenger.vdstools.seals.VdsType;
import junit.framework.TestCase;

public class DataEncoderTest extends TestCase {

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
	public void testGetSignerCertRef_V3() throws InvalidNameException {
		X509Certificate cert = null;
		try {
			String certFilename = "src/test/resources/DETS32.crt";
			FileInputStream inStream = new FileInputStream(certFilename);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			cert = (X509Certificate) cf.generateCertificate(inStream);
		} catch (FileNotFoundException | CertificateException e) {
			fail(e.getMessage());
		}

		String signerCertRef = DataEncoder.getSignerCertRef(cert, (byte) 0x03);
		assertEquals("DETS0232", signerCertRef);
	}

	@Test
	public void testGetSignerCertRef_V2() throws InvalidNameException {
		X509Certificate cert = null;
		try {
			String certFilename = "src/test/resources/DETS32.crt";
			FileInputStream inStream = new FileInputStream(certFilename);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			cert = (X509Certificate) cf.generateCertificate(inStream);
		} catch (FileNotFoundException | CertificateException e) {
			fail(e.getMessage());
		}

		String signerCertRef = DataEncoder.getSignerCertRef(cert, (byte) 0x02);
		assertEquals("DETS00032", signerCertRef);
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

}
