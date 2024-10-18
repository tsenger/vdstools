package de.tsenger.vdstools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.Random;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.junit.BeforeClass;
import org.junit.Test;

public class SignerTest {
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
	public void testKeyStoreConstructor() {
		Signer signer = new Signer(keystore, keyStorePassword, "dets32");
		assertEquals(224, signer.getFieldSize());
	}

	@Test
	public void testSign() throws InvalidKeyException, NoSuchAlgorithmException, SignatureException,
			InvalidAlgorithmParameterException, NoSuchProviderException, IOException {
		Signer signer = new Signer(keystore, keyStorePassword, "dets32");
		byte[] dataBytes = new byte[32];
		Random rnd = new Random();
		rnd.nextBytes(dataBytes);
		byte[] signatureBytes = signer.sign(dataBytes);
		System.out.println("Signature: " + Hex.toHexString(signatureBytes));
		assertTrue(signatureBytes.length * 4 == signer.getFieldSize());
	}

}
