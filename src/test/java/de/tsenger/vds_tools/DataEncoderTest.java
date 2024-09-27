package de.tsenger.vds_tools;

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.time.LocalDate;

import javax.naming.InvalidNameException;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.seals.DocumentFeatureDto;
import de.tsenger.vdstools.seals.VdsHeader;
import de.tsenger.vdstools.seals.VdsMessage;
import de.tsenger.vdstools.seals.VdsType;
import junit.framework.TestCase;

public class DataEncoderTest extends TestCase{
	
	 @Test
	 public void testEncodeDate_Now() {
		 LocalDate ldNow = LocalDate.now();
		 System.out.println("LocalDate.now(): "+ ldNow);
		 byte[] encodedDate = DataEncoder.encodeDate(ldNow);
		 System.out.println("encodedDate: " + Hex.toHexString(encodedDate));
		 assertEquals(ldNow, DataParser.decodeDate(encodedDate));
	 }
	 
	 @Test
	 public void testEncodeDate_String() throws ParseException {
		 byte[] encodedDate = DataEncoder.encodeDate("2024-09-27");
		 System.out.println("encodedDate: " + Hex.toHexString(encodedDate));
		 assertEquals("8d7ad8", Hex.toHexString(encodedDate));
	 }
	 
	 @Test
	 public void testGetSignerCertRef_V3() throws InvalidNameException {
		 X509Certificate cert = null;
        try {
            String certFilename = "src/test/resources/DETS32.crt";
            FileInputStream inStream = new FileInputStream(certFilename);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) cf.generateCertificate(inStream);
        } catch (FileNotFoundException | CertificateException e) {
            fail(e.getMessage());
        }
		 
		String signerCertRef = DataEncoder.getSignerCertRef(cert, (byte)0x03);
		assertEquals("DETS0232", signerCertRef);
	 }
	 
	 @Test
	 public void testGetSignerCertRef_V2() throws InvalidNameException {
		 X509Certificate cert = null;
        try {
            String certFilename = "src/test/resources/DETS32.crt";
            FileInputStream inStream = new FileInputStream(certFilename);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) cf.generateCertificate(inStream);
        } catch (FileNotFoundException | CertificateException e) {
            fail(e.getMessage());
        }
		 
		String signerCertRef = DataEncoder.getSignerCertRef(cert, (byte)0x02);
		assertEquals("DETS00032", signerCertRef);
	 }
	 
	 @Test
	 public void testGetEncodedBytes() {
		 
	 }
	 
	 private VdsHeader buildHeader() {
		 VdsHeader header = new VdsHeader();
		// RESIDENCE_PERMIT 0xfb06
		header.setDocumentType(VdsType.RESIDENCE_PERMIT);
		header.signerIdentifier = "DETS";
		header.certificateReference = "32";
		header.issuingDate = LocalDate.parse("2024-09-27");
		header.sigDate = LocalDate.parse("2024-09-27");
		header.issuingCountry = "D<<";
		header.rawVersion = 0x03;
		return header;
	 }
	 
	 // TODO find a goo solution to build a message.....
	 private VdsMessage buildMessage() throws IOException {
		String mrz = "ARD<<FOLKMANN<<JOSEF<<<<<<<<<<<<<<<<"
		            + "6525845096USA7008038M2201018<<<<<<06";
		String passportNumber = "652584509";
		 VdsMessage vdsMessage = new VdsMessage();
		 ByteArrayOutputStream baos = new ByteArrayOutputStream();
		 baos.write(DataEncoder.buildTLVStructure((byte) 0x02, DataEncoder.encodeC40(mrz)));
		 baos.write(DataEncoder.buildTLVStructure((byte) 0x03, DataEncoder.encodeC40(passportNumber)));
		 byte[] message = baos.toByteArray();
		 vdsMessage.
	 }

}
