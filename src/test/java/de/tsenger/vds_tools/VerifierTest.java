package de.tsenger.vds_tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.junit.Test;

import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.Verifier;
import de.tsenger.vdstools.seals.DigitalSeal;

public class VerifierTest {

    @Test
    public void testVerifyResidentPermit() {
        DigitalSeal digitalSeal = DataParser.parseVdsSeal(DataParserTest.residentPermit_rawBytes);
        X509Certificate cert = null;
        try {
            FileInputStream inStream = new FileInputStream("src/test/resources/sealgen_UTTS5B.crt");
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) cf.generateCertificate(inStream);
        } catch (FileNotFoundException | CertificateException e) {
            fail(e.getMessage());
        }

        Verifier verifier = new Verifier(digitalSeal, cert);
        assertEquals(Verifier.Result.SignatureValid, verifier.verify());
    }

    @Test
    public void testVerifyVisa224BitSig() {
        DigitalSeal digitalSeal = DataParser.parseVdsSeal(DataParserTest.visa_224bitSig_rawBytes);
        X509Certificate cert = null;
        try {
            FileInputStream inStream = new FileInputStream("src/test/resources/sealgen_DETS32.crt");
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) cf.generateCertificate(inStream);
        } catch (FileNotFoundException | CertificateException e) {
            fail(e.getMessage());
        }

        Verifier verifier = new Verifier(digitalSeal, cert);
        assertEquals(Verifier.Result.SignatureValid, verifier.verify());
    }

    @Test
    public void testString() {
        System.out.println(String.format("test 0x%02X", 0xF234));
    }

}
