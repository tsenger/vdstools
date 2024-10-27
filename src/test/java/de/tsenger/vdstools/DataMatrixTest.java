package de.tsenger.vdstools;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tinylog.Logger;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;

import de.tsenger.vdstools.vds.VdsMessage;
import de.tsenger.vdstools.vds.seals.DigitalSeal;

public class DataMatrixTest {

	String keyStorePassword = "vdstools";
	String keyStoreFile = "src/test/resources/vdstools_testcerts.bks";

	@BeforeClass
	public static void loadBC() {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testSaveDataMatrixToFile() throws IOException, KeyStoreException, UnrecoverableKeyException,
			NoSuchAlgorithmException, InvalidKeySpecException {

		String mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<" + "6525845096USA7008038M2201018<<<<<<06";
		String passportNumber = "UFO001979";
		VdsMessage vdsMessage = new VdsMessage("RESIDENCE_PERMIT");
		vdsMessage.addDocumentFeature("MRZ", mrz);
		vdsMessage.addDocumentFeature("PASSPORT_NUMBER", passportNumber);

		KeyStore ks = getKeystore();
		ECPrivateKey ecKey = (ECPrivateKey) ks.getKey("utts5b", keyStorePassword.toCharArray());
		Signer signer = new Signer(ecKey);
		X509Certificate cert = (X509Certificate) ks.getCertificate("utts5b");

		DigitalSeal digitalSeal = DataEncoder.buildDigitalSeal(vdsMessage, cert, signer);

		DataMatrixWriter dmw = new DataMatrixWriter();
		BitMatrix bitMatrix = dmw.encode(DataEncoder.encodeBase256(digitalSeal.getEncodedBytes()),
				BarcodeFormat.DATA_MATRIX, 450, 450);

		// Define your own export Path and uncomment if needed
//		Path path = Path.of("test/test.png");
//		MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
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
