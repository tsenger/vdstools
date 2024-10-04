package de.tsenger.vdstools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.junit.Test;

import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.Verifier;
import de.tsenger.vdstools.seals.DigitalSeal;

public class VerifierTest {

    @Test
    public void testVerifyArrivalAttestationDETS00027() throws ParseException {
        DigitalSeal digitalSeal = DataParser.parseVdsSeal(DataParserTest.arrivalAttestation_rawBytes);
        assert digitalSeal != null;
        String signerCertRef = digitalSeal.getSignerCertRef();
        assertEquals("DETS27", signerCertRef); // input validation
        X509Certificate cert = null;
        try {
            // do not copy this to production code as signerCertRef is external data and
            // must be input validated
            String certFilename = String.format("src/test/resources/%s.crt", signerCertRef);
            FileInputStream inStream = new FileInputStream(certFilename);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) cf.generateCertificate(inStream);

            String signerIdentifier = getCCNString(cert);
            int serialNumber = cert.getSerialNumber().intValue();
            String x509SignerCertRef = String.format("%s%x", signerIdentifier, serialNumber);
            assertEquals(signerCertRef, x509SignerCertRef);
        } catch (FileNotFoundException | CertificateException e) {
            fail(e.getMessage());
        }

        { // check validity dates
            LocalDate sigLocalDate = digitalSeal.getSigDate();
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
        }
        Verifier verifier = new Verifier(digitalSeal, cert);
        assertEquals(Verifier.Result.SignatureValid, verifier.verify());
    }

    @Test
    public void testVerifyResidentPermit() {
        DigitalSeal digitalSeal = DataParser.parseVdsSeal(DataParserTest.residentPermit_rawBytes);
        String signerCertRef = digitalSeal.getSignerCertRef();
        assertEquals("UTTS5B", signerCertRef);
        X509Certificate cert = null;
        try {
            String certFilename = String.format("src/test/resources/%s.crt", signerCertRef);
            FileInputStream inStream = new FileInputStream(certFilename);
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
        String signerCertRef = digitalSeal.getSignerCertRef();
        assertEquals("DETS32", signerCertRef);
        X509Certificate cert = null;
        try {
            String certFilename = String.format("src/test/resources/%s.crt", signerCertRef);
            FileInputStream inStream = new FileInputStream(certFilename);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) cf.generateCertificate(inStream);
        } catch (FileNotFoundException | CertificateException e) {
            fail(e.getMessage());
        }

        Verifier verifier = new Verifier(digitalSeal, cert);
        assertEquals(Verifier.Result.SignatureValid, verifier.verify());
    }

    public static String getCCNString(X509Certificate x509) {
        X500Name x500name = new X500Name(x509.getSubjectX500Principal().getName());
        RDN c = x500name.getRDNs(BCStyle.C)[0];
        RDN cn = x500name.getRDNs(BCStyle.CN)[0];
        String cString = IETFUtils.valueToString(c.getFirst().getValue());
        String cnString = IETFUtils.valueToString(cn.getFirst().getValue());
        return cString + cnString;
    }

}
