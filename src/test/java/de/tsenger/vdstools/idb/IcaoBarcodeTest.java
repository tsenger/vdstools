package de.tsenger.vdstools.idb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.cert.CertificateException;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

public class IcaoBarcodeTest {

	@Test
	public void testIsNotSignedIsNotZipped() {
		IcaoBarcode icb = new IcaoBarcode('A', new IdbPayload(null, null, null, null));
		assertFalse(icb.isSigned());
		assertFalse(icb.isZipped());
	}

	@Test
	public void testIsSignedIsNotZipped() {
		IcaoBarcode icb = new IcaoBarcode('B', new IdbPayload(null, null, null, null));
		assertTrue(icb.isSigned());
		assertFalse(icb.isZipped());
	}

	@Test
	public void testIsNotSignedIsZipped() {
		IcaoBarcode icb = new IcaoBarcode('C', new IdbPayload(null, null, null, null));
		assertFalse(icb.isSigned());
		assertTrue(icb.isZipped());
	}

	@Test
	public void testIsSignedIsZipped() {
		IcaoBarcode icb = new IcaoBarcode('D', new IdbPayload(null, null, null, null));
		assertTrue(icb.isSigned());
		assertTrue(icb.isZipped());
	}

	@Test
	public void testConstructor_signed_zipped() throws CertificateException, IOException {
		IdbPayload payload = IdbPayload.fromByteArray(
				Hex.decode("6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbb"
						+ "b332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7"),
				true);
		IcaoBarcode icb = new IcaoBarcode(true, true, payload);
		System.out.println(icb.getEncoded());
		assertEquals(
				"NDB1DPDNACWQAUX7WVPABAUCAGAQBACNV3CDBCICBBMFRWKZ3JNNWW64LTOV3XS635P37HASLXOZTF5LCVFHUQ7NWEO4NWVOEUZNZZ5JSVFMYIOTKGTQRP5LDIOUU2XQYP4UCMKKD3BCXTL2G2REAJT3DFD5FEPDSP7ZKYE",
				icb.getEncoded());
	}

	@Test
	public void testConstructor_signed_notZipped() throws CertificateException, IOException {
		IdbPayload payload = IdbPayload.fromByteArray(
				Hex.decode("6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbb"
						+ "b332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7"),
				true);
		IcaoBarcode icb = new IcaoBarcode(true, false, payload);
		System.out.println(icb.getEncoded());
		assertEquals(
				"NDB1BNK6ACBIEAMBACAE3LWEGCEQECCYLDMVTWS23NN5YXG5LXPF5X27X6OBEXO5TGL2WFKKPJB63MI5Y3NK4JJS3TT2TFKKZQQ5GUNHBC72WGQ5JJVPBQ7ZIEYUUHWCFPGXUNVCIATHWGKH2KI6H",
				icb.getEncoded());
	}

	@Test
	public void testConstructor_notSigned_zipped() throws CertificateException, IOException {
		IdbPayload payload = IdbPayload.fromByteArray(Hex.decode("6abc61120510b0b1b2b3b4b5b6b7b8b9babbbcbdbebf"),
				false);
		IcaoBarcode icb = new IcaoBarcode(false, true, payload);
		System.out.println(icb.getEncoded());
		assertEquals("NDB1CPDNACFQA5H7WVPDBCICRBMFRWKZ3JNNWW64LTOV3XS635P4DDIGSO", icb.getEncoded());
	}

	@Test
	public void testConstructor_notSigned_notZipped() throws CertificateException, IOException {
		IdbPayload payload = IdbPayload.fromByteArray(Hex.decode("6abc61120510b0b1b2b3b4b5b6b7b8b9babbbcbdbebf"),
				false);
		IcaoBarcode icb = new IcaoBarcode(false, false, payload);
		System.out.println(icb.getEncoded());
		assertEquals("NDB1ANK6GCEQFCCYLDMVTWS23NN5YXG5LXPF5X27Q", icb.getEncoded());
	}

	@Test
	public void testGetEncoded() throws CertificateException, IOException {
		IdbPayload payload = IdbPayload.fromByteArray(
				Hex.decode("6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbb"
						+ "b332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7"),
				true);
		IcaoBarcode icb = new IcaoBarcode('D', payload);
		System.out.println(icb.getEncoded());
		assertEquals(
				"NDB1DPDNACWQAUX7WVPABAUCAGAQBACNV3CDBCICBBMFRWKZ3JNNWW64LTOV3XS635P37HASLXOZTF5LCVFHUQ7NWEO4NWVOEUZNZZ5JSVFMYIOTKGTQRP5LDIOUU2XQYP4UCMKKD3BCXTL2G2REAJT3DFD5FEPDSP7ZKYE",
				icb.getEncoded());
	}

	@Test
	public void testFromString_signed_zipped() throws CertificateException, IOException {
		IcaoBarcode barcode = IcaoBarcode.fromString(
				"NDB1DPDNACWQAUX7WVPABAUCAGAQBACNV3CDBCICBBMFRWKZ3JNNWW64LTOV3XS635P37HASLXOZTF5LCVFHUQ7NWEO4NWVOEUZNZZ5JSVFMYIOTKGTQRP5LDIOUU2XQYP4UCMKKD3BCXTL2G2REAJT3DFD5FEPDSP7ZKYE");
		assertEquals(
				"6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7",
				Hex.toHexString(barcode.getPayLoad().getEncoded()));
//		System.out.println(Hex.toHexString(barcode.getPayLoad().getEncoded()));
	}

	@Test
	public void testFromString_signed_notZipped() throws CertificateException, IOException {
		IcaoBarcode barcode = IcaoBarcode.fromString(
				"NDB1BNK6ACBIEAMBACAE3LWEGCEQECCYLDMVTWS23NN5YXG5LXPF5X27X6OBEXO5TGL2WFKKPJB63MI5Y3NK4JJS3TT2TFKKZQQ5GUNHBC72WGQ5JJVPBQ7ZIEYUUHWCFPGXUNVCIATHWGKH2KI6H");
		assertEquals(
				"6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7",
				Hex.toHexString(barcode.getPayLoad().getEncoded()));
//		System.out.println(Hex.toHexString(barcode.getPayLoad().getEncoded()));
	}

	@Test
	public void testFromString_notSigned_zipped() throws CertificateException, IOException {
		IcaoBarcode barcode = IcaoBarcode.fromString("NDB1CPDNACFQA5H7WVPDBCICRBMFRWKZ3JNNWW64LTOV3XS635P4DDIGSO");
		assertEquals("6abc61120510b0b1b2b3b4b5b6b7b8b9babbbcbdbebf",
				Hex.toHexString(barcode.getPayLoad().getEncoded()));
//		System.out.println(Hex.toHexString(barcode.getPayLoad().getEncoded()));
	}

	@Test
	public void testFromString_notSigned_notZipped() throws CertificateException, IOException {
		IcaoBarcode barcode = IcaoBarcode.fromString("NDB1ANK6GCEQFCCYLDMVTWS23NN5YXG5LXPF5X27Q");
		assertEquals("6abc61120510b0b1b2b3b4b5b6b7b8b9babbbcbdbebf",
				Hex.toHexString(barcode.getPayLoad().getEncoded()));
//		System.out.println(Hex.toHexString(barcode.getPayLoad().getEncoded()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFromString_invalid_BarcodeIdentifier() throws CertificateException, IOException {
		IcaoBarcode.fromString("ADB1ANK6GCEQFCCYLDMVTWS23NN5YXG5LXPF5X27Q");
	}

}
