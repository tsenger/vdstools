package de.tsenger.vdstools.vds;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.DerTlv;
import de.tsenger.vdstools.Signer;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DigitalSeal {

	private final String vdsType;
	private final VdsHeader vdsHeader;
	private final VdsMessage vdsMessage;
	private final VdsSignature vdsSignature;

	private DigitalSeal(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
		this.vdsHeader = vdsHeader;
		this.vdsMessage = vdsMessage;
		this.vdsSignature = vdsSignature;
		this.vdsType = vdsHeader.getVdsType();
	}

	public DigitalSeal(VdsHeader vdsHeader, VdsMessage vdsMessage, Signer signer) {
		this.vdsHeader = vdsHeader;
		this.vdsMessage = vdsMessage;
		this.vdsSignature = createVdsSignature(vdsHeader, vdsMessage, signer);
		this.vdsType = vdsHeader.getVdsType();
	}

	public String getVdsType() {
		return vdsType;
	}

	public String getIssuingCountry() {
		return vdsHeader.getIssuingCountry();
	}

	public String getSignerCertRef() {
		return vdsHeader.getSignerCertRef();
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

	public List<Feature> getFeatureList() {
		return vdsMessage.getFeatureList();
	}

	public Optional<Feature> getFeature(String feature) {
		return vdsMessage.getFeature(feature);
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
