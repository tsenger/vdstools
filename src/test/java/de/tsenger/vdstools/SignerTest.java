package de.tsenger.vdstools;

import static org.junit.Assert.*;

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
import org.tinylog.Logger;

public class SignerTest {
	String keyStorePassword = "jFd853v_+RL4";
	String keyStoreFile = "src/test/resources/sealgen_ds.bks";

	@BeforeClass
	public static void loadBC() {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testKeyStoreConstructor() {
		Signer signer = new Signer(getKeystore(), keyStorePassword, "dets32");
		assertEquals(224, signer.getFieldSize());
	}
	
	@Test
	public void testSign() throws InvalidKeyException, NoSuchAlgorithmException, SignatureException, InvalidAlgorithmParameterException, NoSuchProviderException, IOException {
		Signer signer = new Signer(getKeystore(), keyStorePassword, "dets32");		
		byte[] dataBytes = new byte[32];
		Random rnd = new Random();
		rnd.nextBytes(dataBytes);		
		byte[] signatureBytes = signer.sign(dataBytes);
		System.out.println(Hex.toHexString(signatureBytes));
		assertTrue(signatureBytes.length*4==signer.getFieldSize());
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
