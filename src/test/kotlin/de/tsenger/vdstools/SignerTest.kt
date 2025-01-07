package de.tsenger.vdstools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
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
	public void testSign_224() throws InvalidKeyException, NoSuchAlgorithmException, SignatureException,
			InvalidAlgorithmParameterException, NoSuchProviderException, IOException {
		Signer signer = new Signer(keystore, keyStorePassword, "dets32");
		byte[] dataBytes = new byte[32];
		Random rnd = new Random();
		rnd.nextBytes(dataBytes);
		byte[] signatureBytes = signer.sign(dataBytes);
		System.out.println("Signature: " + Hex.toHexString(signatureBytes));
		assertTrue(signatureBytes.length * 4 == signer.getFieldSize());
	}

	@Test
	public void testSign_256() throws InvalidKeyException, NoSuchAlgorithmException, SignatureException,
			InvalidAlgorithmParameterException, NoSuchProviderException, IOException, InvalidKeySpecException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
		SecureRandom rnd = new SecureRandom();
		keyGen.initialize(256, rnd);
		KeyPair pair = keyGen.generateKeyPair();

		byte[] dataBytes = new byte[32];
		rnd.nextBytes(dataBytes);

		Signer signer = new Signer((ECPrivateKey) pair.getPrivate());
		byte[] signatureBytes = signer.sign(dataBytes);
		System.out.println("Signature: " + Hex.toHexString(signatureBytes));
		assertTrue(signatureBytes.length * 4 == signer.getFieldSize());
	}

	@Test
	public void testSign_384() throws InvalidKeyException, NoSuchAlgorithmException, SignatureException,
			InvalidAlgorithmParameterException, NoSuchProviderException, IOException, InvalidKeySpecException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
		SecureRandom rnd = new SecureRandom();
		keyGen.initialize(384, rnd);
		KeyPair pair = keyGen.generateKeyPair();

		byte[] dataBytes = new byte[64];
		rnd.nextBytes(dataBytes);

		Signer signer = new Signer((ECPrivateKey) pair.getPrivate());
		byte[] signatureBytes = signer.sign(dataBytes);
		System.out.println("Signature: " + Hex.toHexString(signatureBytes));
		assertTrue(signatureBytes.length * 4 == signer.getFieldSize());
	}

	@Test
	public void testSign_512() throws InvalidKeyException, NoSuchAlgorithmException, SignatureException,
			InvalidAlgorithmParameterException, NoSuchProviderException, IOException, InvalidKeySpecException {
		ECGenParameterSpec kpgparams = new ECGenParameterSpec("brainpoolP512r1");
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
		keyGen.initialize(kpgparams);
		KeyPair pair = keyGen.generateKeyPair();

		Random rnd = new Random();
		byte[] dataBytes = new byte[128];
		rnd.nextBytes(dataBytes);

		Signer signer = new Signer((ECPrivateKey) pair.getPrivate());
		byte[] signatureBytes = signer.sign(dataBytes);
		System.out.println("Signature: " + Hex.toHexString(signatureBytes));
		assertTrue(signatureBytes.length * 4 == signer.getFieldSize());
	}

	@Test(expected = InvalidAlgorithmParameterException.class)
	public void testSign_521() throws InvalidKeyException, NoSuchAlgorithmException, SignatureException,
			InvalidAlgorithmParameterException, NoSuchProviderException, IOException, InvalidKeySpecException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
		SecureRandom rnd = new SecureRandom();
		keyGen.initialize(521, rnd);
		KeyPair pair = keyGen.generateKeyPair();

		byte[] dataBytes = new byte[128];
		rnd.nextBytes(dataBytes);

		Signer signer = new Signer((ECPrivateKey) pair.getPrivate());
		byte[] signatureBytes = signer.sign(dataBytes);
		System.out.println("Signature: " + Hex.toHexString(signatureBytes));
		assertTrue(signatureBytes.length * 4 == signer.getFieldSize());
	}

}
