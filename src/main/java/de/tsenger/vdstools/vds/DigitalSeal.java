package de.tsenger.vdstools.vds;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.List;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.tinylog.Logger;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.DerTlv;

/**
 * @author Tobias Senger
 *
 */
public class DigitalSeal {

	private String vdsType;
	private VdsHeader vdsHeader;
	private VdsMessage vdsMessage;
	private VdsSignature vdsSignature;

	public DigitalSeal(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
		this.vdsHeader = vdsHeader;
		this.vdsMessage = vdsMessage;
		this.vdsSignature = vdsSignature;
		this.vdsType = vdsHeader.getVdsType();
	}

	public String getVdsType() {
		return vdsType;
	}

	public String getIssuingCountry() {
		return vdsHeader.issuingCountry;
	}

	/**
	 * Returns a string that identifies the signer certificate. The SignerCertRef
	 * string is build from Signer Identifier (country code || signer id) and
	 * Certificate Reference. The Signer Identifier maps to the signer certificates
	 * subject (C || CN) The Certificate Reference will be interpreted as an hex
	 * string integer that represents the serial number of the signer certificate.
	 * Leading zeros in Certificate Reference the will be cut off. e.g. Signer
	 * Identifier 'DETS' and CertificateReference '00027' will result in 'DETS27'
	 * 
	 * @return Formated SignerCertRef all UPPERCASE
	 */
	public String getSignerCertRef() {
		BigInteger certRefInteger = new BigInteger(vdsHeader.certificateReference, 16);
		return String.format("%s%x", vdsHeader.signerIdentifier, certRefInteger).toUpperCase();
	}

	public String getSignerIdentifier() {
		return vdsHeader.signerIdentifier;
	}

	public String getCertificateReference() {
		return vdsHeader.certificateReference;
	}

	public LocalDate getIssuingDate() {
		return vdsHeader.issuingDate;
	}

	public LocalDate getSigDate() {
		return vdsHeader.sigDate;
	}

	public byte getDocFeatureRef() {
		return vdsHeader.docFeatureRef;
	}

	public byte getDocTypeCat() {
		return vdsHeader.docTypeCat;
	}

	public byte[] getHeaderAndMessageBytes() {
		return Arrays.concatenate(vdsHeader.getEncoded(), vdsMessage.getEncoded());
	}

	public byte[] getEncoded() throws IOException {
		return Arrays.concatenate(vdsHeader.getEncoded(), vdsMessage.getEncoded(), vdsSignature.getEncoded());
	}

	public byte[] getSignatureBytes() {
		return vdsSignature.getPlainSignatureBytes();
	}

	public String getRawString() throws IOException {
		return DataEncoder.encodeBase256(getEncoded());
	}

	public <T> T getFeature(String feature) {
		return vdsMessage.getDocumentFeature(feature);
	}

	public <T> void addFeature(String feature, T value) {
		vdsMessage.addDocumentFeature(feature, value);
	}

	public static DigitalSeal fromRawString(String rawString) {
		DigitalSeal seal = null;
		try {
			seal = parseVdsSeal(DataParser.decodeBase256(rawString));
		} catch (IOException e) {
			Logger.error(e.getMessage());
		}
		return seal;
	}

	public static DigitalSeal fromByteArray(byte[] rawBytes) {
		DigitalSeal seal = null;
		try {
			seal = parseVdsSeal(rawBytes);
		} catch (IOException e) {
			Logger.error(e.getMessage());
		}
		return seal;
	}

	private static DigitalSeal parseVdsSeal(byte[] rawBytes) throws IOException {

		ByteBuffer rawData = ByteBuffer.wrap(rawBytes);
		Logger.trace("rawData: {}", () -> Hex.toHexString(rawBytes));

		VdsHeader vdsHeader = VdsHeader.fromByteBuffer(rawData);
		VdsMessage vdsMessage = new VdsMessage(vdsHeader.getVdsType());
		VdsSignature vdsSignature = null;

		int messageStartPosition = rawData.position();

		List<DerTlv> derTlvList = DataParser
				.parseDerTLvs(Arrays.copyOfRange(rawBytes, messageStartPosition, rawBytes.length));

		for (DerTlv derTlv : derTlvList) {
			if (derTlv.getTag() == (byte) 0xff) {
				vdsSignature = VdsSignature.fromByteArray(derTlv.getEncoded());
			} else {
				vdsMessage.addDerTlv(derTlv);
			}
		}
		return new DigitalSeal(vdsHeader, vdsMessage, vdsSignature);

	}

}
