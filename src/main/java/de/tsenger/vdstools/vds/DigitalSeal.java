package de.tsenger.vdstools.vds;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.tinylog.Logger;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.DerTlv;
import de.tsenger.vdstools.Signer;

public class DigitalSeal {

	private String vdsType;
	private VdsHeader vdsHeader;
	private VdsMessage vdsMessage;
	private VdsSignature vdsSignature;

	private DigitalSeal(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
		this.vdsHeader = vdsHeader;
		this.vdsMessage = vdsMessage;
		this.vdsSignature = vdsSignature;
		this.vdsType = vdsHeader.getVdsType();
	}

	public String getVdsType() {
		return vdsType;
	}

	public String getIssuingCountry() {
		return vdsHeader.getIssuingCountry();
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
		BigInteger certRefInteger = new BigInteger(vdsHeader.getCertificateReference(), 16);
		return String.format("%s%x", vdsHeader.getSignerIdentifier(), certRefInteger).toUpperCase();
	}

	public String getSignerIdentifier() {
		return vdsHeader.getSignerIdentifier();
	}

	public String getCertificateReference() {
		return vdsHeader.getCertificateReference();
	}

	public LocalDate getIssuingDate() {
		return vdsHeader.getIssuingDate();
	}

	public LocalDate getSigDate() {
		return vdsHeader.getSigDate();
	}

	public byte getDocFeatureRef() {
		return vdsHeader.getDocFeatureRef();
	}

	public byte getDocTypeCat() {
		return vdsHeader.getDocTypeCat();
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
		VdsSignature vdsSignature = null;

		int messageStartPosition = rawData.position();

		List<DerTlv> derTlvList = DataParser
				.parseDerTLvs(Arrays.copyOfRange(rawBytes, messageStartPosition, rawBytes.length));

		List<DerTlv> featureList = new ArrayList<DerTlv>(derTlvList.size() - 1);

		for (DerTlv derTlv : derTlvList) {
			if (derTlv.getTag() == (byte) 0xff) {
				vdsSignature = VdsSignature.fromByteArray(derTlv.getEncoded());
			} else {
				featureList.add(derTlv);
			}
		}
		VdsMessage vdsMessage = new VdsMessage(vdsHeader.getVdsType(), featureList);
		return new DigitalSeal(vdsHeader, vdsMessage, vdsSignature);

	}

	public static class Builder {

		private VdsHeader vdsHeader;
		private VdsMessage vdsMessage;
		private VdsSignature vdsSignature;
		private Signer signer;

		public Builder() {
		}

		public Builder setHeader(VdsHeader vdsHeader) {
			this.vdsHeader = vdsHeader;
			return this;
		}

		public Builder setMessage(VdsMessage vdsMessage) {
			this.vdsMessage = vdsMessage;
			return this;
		}

		public Builder setSigner(Signer signer) {
			this.signer = signer;
			return this;
		}

		public DigitalSeal build() {
			this.vdsSignature = createVdsSignature(vdsHeader, vdsMessage, signer);
			return new DigitalSeal(vdsHeader, vdsMessage, vdsSignature);
		}

		private VdsSignature createVdsSignature(VdsHeader vdsHeader, VdsMessage vdsMessage, Signer signer) {
			byte[] headerMessage = Arrays.concatenate(vdsHeader.getEncoded(), vdsMessage.getEncoded());
			try {
				byte[] signatureBytes = signer.sign(headerMessage);
				return new VdsSignature(signatureBytes);
			} catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException
					| InvalidAlgorithmParameterException | NoSuchProviderException | IOException e) {
				Logger.error("Signature creation failed: " + e.getMessage());
				return null;
			}
		}
	}

}
