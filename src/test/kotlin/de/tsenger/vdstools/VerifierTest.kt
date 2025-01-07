package de.tsenger.vdstools;

import de.tsenger.vdstools.vds.DigitalSeal;
import de.tsenger.vdstools.vds.VdsRawBytes;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class VerifierTest {

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

    public static String getCCNString(X509Certificate x509) {
        X500Name x500name = new X500Name(x509.getSubjectX500Principal().getName());
        RDN c = x500name.getRDNs(BCStyle.C)[0];
        RDN cn = x500name.getRDNs(BCStyle.CN)[0];
        String cString = IETFUtils.valueToString(c.getFirst().getValue());
        String cnString = IETFUtils.valueToString(cn.getFirst().getValue());
        return cString + cnString;
    }

    @Test
    public void testVerifyArrivalAttestationDETS00027() throws ParseException, KeyStoreException, IOException {
        DigitalSeal digitalSeal = DigitalSeal.fromByteArray(VdsRawBytes.arrivalAttestation);
        assert digitalSeal != null;
        String signerCertRef = digitalSeal.getSignerCertRef();
        assertEquals("DETS27", signerCertRef); // input validation

        X509Certificate cert = (X509Certificate) keystore.getCertificate(signerCertRef.toLowerCase());

        String signerIdentifier = getCCNString(cert);
        int serialNumber = cert.getSerialNumber().intValue();
        String x509SignerCertRef = String.format("%s%x", signerIdentifier, serialNumber);
        assertEquals(signerCertRef, x509SignerCertRef);

        LocalDate sigLocalDate = digitalSeal.getSigDate().getValue$kotlinx_datetime();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String formattedString = sigLocalDate.format(dtf);
        assertEquals("13.01.2020", formattedString);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        Date sigDate = sdf.parse(formattedString);
        try {
            cert.checkValidity(sigDate);
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            fail(e.getMessage());
        }

        Verifier verifier = new Verifier(digitalSeal, cert);
        assertEquals(Verifier.Result.SignatureValid, verifier.verify());
    }

    @Test
    public void testVerifyResidentPermit256BitSig() throws KeyStoreException, IOException {
        DigitalSeal digitalSeal = DigitalSeal.fromByteArray(VdsRawBytes.residentPermit);
        String signerCertRef = digitalSeal.getSignerCertRef();
        assertEquals("UTTS5B", signerCertRef);
        X509Certificate cert = (X509Certificate) keystore.getCertificate(signerCertRef.toLowerCase());

        Verifier verifier = new Verifier(digitalSeal, cert);
        assertEquals(Verifier.Result.SignatureValid, verifier.verify());
    }

    @Test
    public void testVerifyVisa224BitSig() throws KeyStoreException, IOException {
        DigitalSeal digitalSeal = DigitalSeal.fromByteArray(VdsRawBytes.visa_224bitSig);
        String signerCertRef = digitalSeal.getSignerCertRef();
        assertEquals("DETS32", signerCertRef);
        X509Certificate cert = (X509Certificate) keystore.getCertificate(signerCertRef.toLowerCase());

        Verifier verifier = new Verifier(digitalSeal, cert);
        assertEquals(Verifier.Result.SignatureValid, verifier.verify());
    }

}
