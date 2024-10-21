package de.tsenger.vdstools.idb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.cert.CertificateException;

import org.bouncycastle.util.encoders.Base32Encoder;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;

public class IcaoBarcode {
	public static final String BARCODE_IDENTIFIER = "NDB1";

	char barcodeFlag = 0x41;
	IdbPayload barcodePayload;

	public IcaoBarcode(char barcodeFlag, IdbPayload barcodePayload) {
		this.barcodeFlag = barcodeFlag;
		this.barcodePayload = barcodePayload;
	}

	public IcaoBarcode(boolean isSigned, boolean isZipped, IdbPayload barcodePayload) {
		if (isSigned)
			barcodeFlag = (char) (barcodeFlag + 0x01);
		if (isZipped)
			barcodeFlag = (char) (barcodeFlag + 0x02);
		this.barcodePayload = barcodePayload;
	}

	public boolean isSigned() {
		return ((byte) (((byte) barcodeFlag) - 0x41) & 0x01) == 0x01;
	}

	public boolean isZipped() {
		return ((byte) (((byte) barcodeFlag) - 0x41) & 0x02) == 0x02;
	}

	public IdbPayload getPayLoad() {
		return barcodePayload;
	}

	public String getEncoded() throws IOException {
		StringBuffer strBuffer = new StringBuffer(BARCODE_IDENTIFIER);
		strBuffer.append(barcodeFlag);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		if (isZipped()) {
			bos.write(DataEncoder.zip(barcodePayload.getEncoded()));
		} else {
			bos.write(barcodePayload.getEncoded());
		}
		byte[] payloadBytes = bos.toByteArray();
		bos.reset();

		Base32Encoder base32 = new Base32Encoder();
		base32.encode(payloadBytes, 0, payloadBytes.length, bos);
		String base32EncodedPayload = bos.toString().replace("=", "");

		strBuffer.append(base32EncodedPayload);
		return strBuffer.toString();
	}

	public static IcaoBarcode fromString(String barcodeString) throws IOException, CertificateException {
		StringBuffer strBuffer = new StringBuffer(barcodeString);
		if (!strBuffer.substring(0, 4).matches(BARCODE_IDENTIFIER))
			throw new IllegalArgumentException("Didn't found an ICAO Barcode in the given String: " + barcodeString);
		char barcodeFlag = strBuffer.charAt(4);
		boolean isSigned = ((byte) (((byte) barcodeFlag) - 0x41) & 0x01) == 0x01;
		boolean isZipped = ((byte) (((byte) barcodeFlag) - 0x41) & 0x02) == 0x02;

		StringBuffer base32EncodedPayload = new StringBuffer(strBuffer.substring(5));
		while (base32EncodedPayload.length() % 8 != 0) {
			base32EncodedPayload.append("=");
		}

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		Base32Encoder base32 = new Base32Encoder();
		base32.decode(base32EncodedPayload.toString(), bos);
		byte[] payloadBytes = bos.toByteArray();

		if (isZipped) {
			payloadBytes = DataParser.unzip(payloadBytes);
		}

		IdbPayload payload = IdbPayload.fromByteArray(payloadBytes, isSigned);
		return new IcaoBarcode(barcodeFlag, payload);

	}
}
