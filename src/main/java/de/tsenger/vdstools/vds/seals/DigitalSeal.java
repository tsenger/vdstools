package de.tsenger.vdstools.vds.seals;

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;

import org.bouncycastle.util.Arrays;
import org.tinylog.Logger;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.DerTlv;
import de.tsenger.vdstools.Signer;
import de.tsenger.vdstools.vds.Feature;
import de.tsenger.vdstools.vds.VdsHeader;
import de.tsenger.vdstools.vds.VdsMessage;
import de.tsenger.vdstools.vds.VdsSignature;
import de.tsenger.vdstools.vds.VdsType;

/**
 * @author Tobias Senger
 *
 */
public class DigitalSeal {

	private VdsType vdsType;
	private VdsHeader vdsHeader;
	private VdsMessage vdsMessage;
	private VdsSignature vdsSignature;

	protected EnumMap<Feature, Object> featureMap = new EnumMap<>(Feature.class);

	public DigitalSeal(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
		this.vdsHeader = vdsHeader;
		this.vdsMessage = vdsMessage;
		this.vdsSignature = vdsSignature;
		this.vdsType = VdsType.valueOf(vdsHeader.getDocumentRef());
	}

	public static DigitalSeal getInstance(String rawString) {
		DigitalSeal seal = null;
		try {
			seal = DataParser.parseVdsSeal(rawString);
		} catch (IOException e) {
			Logger.error(e.getMessage());
		}
		return seal;
	}

	public static DigitalSeal getInstance(byte[] rawBytes) {
		DigitalSeal seal = null;
		try {
			seal = DataParser.parseVdsSeal(rawBytes);
		} catch (IOException e) {
			Logger.error(e.getMessage());
		}
		return seal;
	}

	public static DigitalSeal getInstance(VdsMessage vdsMessage, X509Certificate cert, Signer signer) {
		DigitalSeal seal = DataEncoder.buildDigitalSeal(vdsMessage, cert, signer);
		return seal;
	}

	public static DigitalSeal getInstance(VdsHeader vdsHeader, VdsMessage vdsMessage, Signer signer) {
		DigitalSeal seal = DataEncoder.buildDigitalSeal(vdsHeader, vdsMessage, signer);
		return seal;
	}

	public VdsType getVdsType() {
		return vdsType;
	}

	public List<DerTlv> getMessageTlvList() {
		return vdsMessage.getDerTlvList();
	}

	public EnumMap<Feature, Object> getFeatureMap() {
		return featureMap.clone();
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

	public byte[] getEncodedBytes() throws IOException {
		return Arrays.concatenate(vdsHeader.getEncoded(), vdsMessage.getEncoded(), vdsSignature.getEncoded());
	}

	public byte[] getSignatureBytes() {
		return vdsSignature.getPlainSignatureBytes();
	}

	public String getRawString() throws IOException {
		return DataEncoder.encodeBase256(getEncodedBytes());
	}

	public Object getFeature(Feature feature) {
		return vdsMessage.getDocumentFeature(feature);
	}

	public void setFeature(Feature feature, Object object) {
		vdsMessage.addDocumentFeature(feature, object);

	}

}
