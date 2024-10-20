package de.tsenger.vdstools.idb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.security.cert.CertificateException;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

public class BarcodePayloadTest {

	@Test
	public void testConstructor_null() {
		BarcodePayload payload = new BarcodePayload(null, null, null, null);
		assertNotNull(payload);
	}

	@Test
	public void testConstructorWithoutSignature() {
		IdbHeader header = new IdbHeader("D<<");
		IdbMessageGroup messageGroup = new IdbMessageGroup();
		messageGroup.addMessage(
				new IdbMessage(IdbMessageType.PROOF_OF_RECOVERY, Hex.decode("b0b1b2b3b4b5b6b7b8b9babbbcbdbebf")));
		BarcodePayload payload = new BarcodePayload(header, messageGroup, null, null);
		assertNotNull(payload);
	}

	@Test
	public void testConstructorWithoutCertificate() throws IOException {
		IdbHeader header = new IdbHeader("D<<", IdbSignatureAlgorithm.SHA256_WITH_ECDSA, new byte[] { 5, 4, 3, 2, 1 });
		IdbMessageGroup messageGroup = new IdbMessageGroup(
				new IdbMessage(IdbMessageType.PROOF_OF_VACCINATION, Hex.decode("b0b1b2b3b4b5b6b7b8b9babbbcbdbebf")));
		IdbSignature signature = new IdbSignature(Hex.decode(
				"24bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7"));
		BarcodePayload payload = new BarcodePayload(header, messageGroup, null, signature);
		assertNotNull(payload);
	}

	@Test
	public void testGetIdbHeader() throws IOException, CertificateException {
		byte[] rawBytes = Hex.decode("6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb33"
				+ "2f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7");
		BarcodePayload payload = BarcodePayload.fromByteArray(rawBytes, true);
		IdbHeader header = payload.getIdbHeader();
		assertEquals("6abc010504030201009b5d88", Hex.toHexString(header.getEncoded()));
	}

	@Test
	public void testGetIdbMessageGroup() throws CertificateException, IOException {
		byte[] rawBytes = Hex.decode("6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb33"
				+ "2f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7");
		BarcodePayload payload = BarcodePayload.fromByteArray(rawBytes, true);
		IdbMessageGroup messageGroup = payload.getIdbMessageGroup();
		assertNotNull(messageGroup);
		assertEquals("61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf", Hex.toHexString(messageGroup.getEncoded()));
	}

	@Test
	public void testGetIdbSignerCertificate_null() throws CertificateException, IOException {
		byte[] rawBytes = Hex.decode("6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb33"
				+ "2f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7");
		BarcodePayload payload = BarcodePayload.fromByteArray(rawBytes, true);
		IdbSignerCertificate signerCert = payload.getIdbSignerCertificate();
		assertNull(signerCert);
	}

	@Test
	public void testGetIdbSignature() throws CertificateException, IOException {
		byte[] rawBytes = Hex.decode("6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb33"
				+ "2f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7");
		BarcodePayload payload = BarcodePayload.fromByteArray(rawBytes, true);
		IdbSignature signature = payload.getIdbSignature();
		assertNotNull(signature);
		assertEquals(
				"24bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7",
				Hex.toHexString(signature.getPlainSignatureBytes()));
	}

	@Test
	public void testGetEncodedWithoutSignature() throws IOException {
		IdbHeader header = new IdbHeader("D<<");
		IdbMessageGroup messageGroup = new IdbMessageGroup();
		messageGroup.addMessage(
				new IdbMessage(IdbMessageType.PROOF_OF_RECOVERY, Hex.decode("b0b1b2b3b4b5b6b7b8b9babbbcbdbebf")));
		BarcodePayload payload = new BarcodePayload(header, messageGroup, null, null);
		byte[] encodedBytes = payload.getEncoded();
		assertEquals("6abc61120510b0b1b2b3b4b5b6b7b8b9babbbcbdbebf", Hex.toHexString(encodedBytes));
	}

	@Test
	public void testGetEncodedWithoutCertificate() throws IOException {
		IdbHeader header = new IdbHeader("D<<", IdbSignatureAlgorithm.SHA256_WITH_ECDSA, new byte[] { 5, 4, 3, 2, 1 },
				"2024-10-18");
		IdbMessageGroup messageGroup = new IdbMessageGroup(
				new IdbMessage(IdbMessageType.PROOF_OF_VACCINATION, Hex.decode("b0b1b2b3b4b5b6b7b8b9babbbcbdbebf")));
		IdbSignature signature = new IdbSignature(Hex.decode(
				"24bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7"));
		BarcodePayload payload = new BarcodePayload(header, messageGroup, null, signature);
		byte[] encodedBytes = payload.getEncoded();
		assertEquals("6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb33"
				+ "2f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7",
				Hex.toHexString(encodedBytes));
	}

	@Test
	public void testFromByteArray() throws CertificateException, IOException {
		byte[] rawBytes = Hex.decode("6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb33"
				+ "2f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7");
		BarcodePayload payload = BarcodePayload.fromByteArray(rawBytes, true);
		assertNotNull(payload);
	}

}
