package de.tsenger.vdstools;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
