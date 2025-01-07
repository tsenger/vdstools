package de.tsenger.vdstools;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tinylog.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;

public class DataMatrixTest {

    String keyStorePassword = "vdstools";
    String keyStoreFile = "src/test/resources/vdstools_testcerts.bks";

    @BeforeClass
    public static void loadBC() {
        Security.addProvider(new BouncyCastleProvider());
    }

//	@Test
//	public void testSaveDataMatrixToFile() throws IOException, KeyStoreException, UnrecoverableKeyException,
//			NoSuchAlgorithmException, InvalidKeySpecException {
//
//		String mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<" + "6525845096USA7008038M2201018<<<<<<06";
//		String passportNumber = "UFO001979";
//		VdsMessage vdsMessage = new VdsMessage.Builder("RESIDENCE_PERMIT").addDocumentFeature("MRZ", mrz)
//				.addDocumentFeature("PASSPORT_NUMBER", passportNumber).build();
//
//		KeyStore ks = getKeystore();
//		ECPrivateKey ecKey = (ECPrivateKey) ks.getKey("utts5b", keyStorePassword.toCharArray());
//		Signer signer = new Signer(ecKey);
//		X509Certificate cert = (X509Certificate) ks.getCertificate("utts5b");
//
//		VdsHeader vdsHeader = new VdsHeader.Builder(vdsMessage.getVdsType()).setSignerCertRef(cert, true).build();
//		DigitalSeal digitalSeal = new DigitalSeal(vdsHeader, vdsMessage, signer);
//
//		DataMatrixWriter dmw = new DataMatrixWriter();
//		BitMatrix bitMatrix = dmw.encode(DataEncoder.encodeBase256(digitalSeal.getEncoded()), BarcodeFormat.DATA_MATRIX,
//				450, 450);
//
//		// Define your own export Path and uncomment if needed

    /// /		Path path = Path.of("test/test.png");
    /// /		MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
//	}
    @Test
    public void testDmFromRawString() throws IOException {
        String rawString = "Ü\u0003j¼m4\u008A(4\u0016OÕ\u0096OÕ\u0096]\u0001\u0002,ÝR\u0013SÙ¢us[Ô\u0013KÙu·t\u0013<\u0013<\u0013<\u0013<\u0019¥\u0019¥\u0019¥\u001Er°Á\u001B\u000EL|&uKýþ1\u0004\u0003 \u0001 \u0005\u0006Ï7\u0019¦'\u008Dÿ8FÈ `B2·|\u008Aå4jóà&Ì\u0093à,ã\u001E\u0084\u0092\u001BTe¤Ô®Àh\u009D\u0001Õ\u008C\u0011ì\u0093Ü\"\u001E\u0002)ìo3\u001C\u009A\u0090\u008A\u00ADBÈiM#";
        DataMatrixWriter dmw = new DataMatrixWriter();
        BitMatrix bitMatrix = dmw.encode(rawString, BarcodeFormat.DATA_MATRIX,
                450, 450);
        Path path = Path.of("test/test.png");
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
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
