package de.tsenger.vdstools.idb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.BeforeClass;
import org.junit.Test;

public class IdbSignerCertificateTest {

	static String keyStorePassword = "vdstools";
	static String keyStoreFile = "src/test/resources/vdstools_testcerts.bks";
	static KeyStore keystore;

	byte[] encodedIdbSignerCertificate = Hex
			.decode("7e8201bc308201b83082013ea00302010202015b300a06082a8648ce3d040302303e"
					+ "310b30090603550406130255543110300e060355040a13077473656e6765723110300e060355040b13077365616c67656e310b3009"
					+ "060355040313025453301e170d3230303631303037313530305a170d3330303631303037313530305a303e310b3009060355040613"
					+ "0255543110300e060355040a13077473656e6765723110300e060355040b13077365616c67656e310b300906035504031302545330"
					+ "5a301406072a8648ce3d020106092b24030302080101070342000408132a7243b3ccc29c271097081c96a729eefb8eb93630e53649"
					+ "8e9b7ce1ced25d68a789d93bef39c04715c5ad3915d281c0754ecc08508bf66687efc630df88a32c302a30090603551d1304023000"
					+ "301d0603551d0e04160414adc6bafc76d49aa2d92fface93d71033832c6e96300a06082a8648ce3d040302036800306502310087f8"
					+ "5c8aa332659ed7ec30b8b61653353158f5ee6841c45c3b98fd1f14f0366203c934136c7444398f7fed359300203402307a95090526"
					+ "35c0faceeb83b00ad56d345a48e9af9b7e27c1301b5c47c347a91e464223551174dfba9f85beda2350f452");

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
	public void testConstructor() throws KeyStoreException {
		IdbSignerCertificate signCert = new IdbSignerCertificate((X509Certificate) keystore.getCertificate("dets32"));
		assertNotNull(signCert);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor_null() throws KeyStoreException {
		new IdbSignerCertificate(null);
	}

	@Test
	public void testFromByteArray() throws CertificateException, IOException {
		IdbSignerCertificate signCert = IdbSignerCertificate.fromByteArray(encodedIdbSignerCertificate);
		assertNotNull(signCert);
	}

	@Test
	public void testGetEncoded() throws KeyStoreException, CertificateEncodingException, IOException {
		IdbSignerCertificate signCert = new IdbSignerCertificate((X509Certificate) keystore.getCertificate("utts5b"));
		assertTrue(Arrays.areEqual(encodedIdbSignerCertificate, signCert.getEncoded()));
//		System.out.println(Hex.toHexString(signCert.getEncoded()));
	}

	@Test
	public void testGetX509Certificate() throws CertificateException, IOException {
		IdbSignerCertificate signCert = IdbSignerCertificate.fromByteArray(encodedIdbSignerCertificate);
		assertNotNull(signCert.getX509Certificate());
	}

}
