package de.tsenger.vdstools.vds;

import static org.junit.Assert.assertEquals;
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

public class VdsHeaderTest {
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
	public void testGetDocumentRef() {
		VdsHeader header = new VdsHeader.Builder("ALIENS_LAW").build();
		assertEquals(header.getDocumentRef(), 0x01fe);
	}

	@Test
	public void testGetEncoded_V3() {
		// RESIDENCE_PERMIT 0xfb06
		VdsHeader header = new VdsHeader.Builder("RESIDENCE_PERMIT")
				.setIssuingCountry("D<<")
				.setSignerIdentifier("DETS")
				.setCertificateReference("32")
				.setIssuingDate(LocalDate.parse("2024-09-27"))
				.setSigDate(LocalDate.parse("2024-09-27"))
				.build();
		byte[] headerBytes = header.getEncoded();
		System.out.println("Header bytes:\n" + Hex.toHexString(headerBytes));
		assertEquals("dc036abc6d32c8a72cb18d7ad88d7ad8fb06", Hex.toHexString(headerBytes));
	}

	@Test
	public void testGetEncoded_V2() {
		// RESIDENCE_PERMIT 0xfb06
		VdsHeader header = new VdsHeader.Builder("RESIDENCE_PERMIT")
				.setRawVersion(2)
				.setIssuingCountry("D<<")
				.setSignerIdentifier("DETS")
				.setCertificateReference("32")
				.setIssuingDate(LocalDate.parse("2024-09-27"))
				.setSigDate(LocalDate.parse("2024-09-27"))
				.build();
		byte[] headerBytes = header.getEncoded();
		System.out.println("Header bytes:\n" + Hex.toHexString(headerBytes));
		assertEquals("dc026abc6d32c8a51a1f8d7ad88d7ad8fb06", Hex.toHexString(headerBytes));
	}
	
	@Test
	public void testBuildHeader_2parameter() throws KeyStoreException {
		X509Certificate cert = (X509Certificate) keystore.getCertificate("dets32");
		LocalDate ldNow = LocalDate.now();
		byte[] encodedDate = DataEncoder.encodeDate(ldNow);

		VdsHeader vdsHeader = new VdsHeader.Builder("RESIDENCE_PERMIT")
				.setSignerCertRef(cert, true)
				.build();
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

		VdsHeader vdsHeader = new VdsHeader.Builder("RESIDENCE_PERMIT")
				.setSignerCertRef(cert, false)
				.setIssuingCountry("XYZ")
				.build();
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

		
		VdsHeader vdsHeader = new VdsHeader.Builder("RESIDENCE_PERMIT")
				.setSignerCertRef(cert, false)
				.setIssuingCountry("XYZ")
				.setRawVersion(3)
				.setIssuingDate(ldate)
				.build();

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

		VdsHeader vdsHeader = new VdsHeader.Builder("TEMP_PASSPORT")
				.setSignerCertRef(cert, false)
				.setIssuingCountry("XYZ")
				.setRawVersion(2)
				.setIssuingDate(ldate)
				.build();
		byte[] headerBytes = vdsHeader.getEncoded();
		byte[] expectedHeaderBytes = Arrays.concatenate(Hex.decode("dc02ed586d32c8a51a1f"), issuingDate, signDate,
				Hex.decode("f60d"));
		assertTrue(Arrays.areEqual(expectedHeaderBytes, headerBytes));
	}

}
