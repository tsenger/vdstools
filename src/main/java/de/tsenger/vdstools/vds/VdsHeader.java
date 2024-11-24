package de.tsenger.vdstools.vds;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.Doc9303CountryCodes;
import org.tinylog.Logger;

import javax.naming.InvalidNameException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;
import java.time.LocalDate;

public class VdsHeader {

	public static final byte DC = (byte) 0xDC;

	private String issuingCountry;
	private String signerIdentifier;
	private String certificateReference;
	private LocalDate issuingDate;
	private LocalDate sigDate;
	private byte docFeatureRef;
	private byte docTypeCat;
	private byte rawVersion;

	private VdsHeader() {
	}

	private VdsHeader(Builder builder) {
		this.issuingCountry = builder.issuingCountry;
		this.signerIdentifier = builder.signerIdentifier;
		this.certificateReference = builder.certificateReference;
		this.issuingDate = builder.issuingDate;
		this.sigDate = builder.sigDate;
		this.docFeatureRef = builder.docFeatureRef;
		this.docTypeCat = builder.docTypeCat;
		this.rawVersion = builder.rawVersion;
	}

	public String getIssuingCountry() {
		return issuingCountry;
	}

	public String getSignerIdentifier() {
		return signerIdentifier;
	}

	public String getCertificateReference() {
		return certificateReference;
	}

	/**
	 * Returns a string that identifies the signer certificate. The SignerCertRef
	 * string is build from Signer Identifier (country code || signer id) and
	 * Certificate Reference. The Signer Identifier maps to the signer certificates
	 * subject (C || CN) The Certificate Reference will be interpreted as a hex
	 * string integer that represents the serial number of the signer certificate.
	 * Leading zeros in Certificate Reference will be cut off. e.g. Signer
	 * Identifier 'DETS' and CertificateReference '00027' will result in 'DETS27'
	 *
	 * @return Formated SignerCertRef all UPPERCASE
	 */
	public String getSignerCertRef() {
		BigInteger certRefInteger = new BigInteger(certificateReference, 16);
		return String.format("%s%x", signerIdentifier, certRefInteger).toUpperCase();
	}

	public LocalDate getIssuingDate() {
		return issuingDate;
	}

	public LocalDate getSigDate() {
		return sigDate;
	}

	public byte getDocFeatureRef() {
		return docFeatureRef;
	}

	public byte getDocTypeCat() {
		return docTypeCat;
	}

	public byte getRawVersion() {
		return rawVersion;
	}

	public int getDocumentRef() {
		return ((docFeatureRef & 0xFF) << 8) + (docTypeCat & 0xFF);
	}

	public String getVdsType() {
		String vdsType = DataEncoder.getVdsType(getDocumentRef());
		if (vdsType == null)
			return "UNKNOWN";
		else
			return vdsType;
	}

	public byte[] getEncoded() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write(DC);
			baos.write(rawVersion);
			baos.write(DataEncoder.encodeC40(issuingCountry));
			baos.write(DataEncoder.encodeC40(getEncodedSignerIdentifierandCertificateReference()));
			baos.write(DataEncoder.encodeDate(issuingDate));
			baos.write(DataEncoder.encodeDate(sigDate));
			baos.write(docFeatureRef);
			baos.write(docTypeCat);
		} catch (IOException e) {
			Logger.error("Error while encoding header data: " + e.getMessage());
		}
		return baos.toByteArray();
	}

	private String getEncodedSignerIdentifierandCertificateReference() {
		if (rawVersion == 2) {
			return String.format("%s%5s", signerIdentifier, certificateReference).toUpperCase().replace(' ', '0');
		} else if (rawVersion == 3) {
			return String.format("%s%02x%s", signerIdentifier, certificateReference.length(), certificateReference)
					.toUpperCase();
		} else {
			return "";
		}
	}

	public static VdsHeader fromByteBuffer(ByteBuffer rawdata) {
		// Magic Byte
		int magicByte = rawdata.get();
		if (magicByte != DC) {
			Logger.error(String.format("Magic Constant mismatch: 0x%02X instead of 0xdc", magicByte));
			throw new IllegalArgumentException(
					String.format("Magic Constant mismatch: 0x%02X instead of 0xdc", magicByte));
		}

		VdsHeader vdsHeader = new VdsHeader();

		vdsHeader.rawVersion = rawdata.get();
		/*
		 * In ICAO spec for "Visual Digital Seals for Non-Electronic Documents" value
		 * 0x02 stands for version 3 (uses fix length of Document Signer Reference: 5
		 * characters) value 0x03 stands for version 4 (uses variable length of Document
		 * Signer Reference) Problem: German "Arrival Attestation Document" uses value
		 * 0x03 for rawVersion 3 and static length of Document Signer Reference.
		 */
		if (!(vdsHeader.rawVersion == 0x02 || vdsHeader.rawVersion == 0x03)) {
			Logger.error(String.format("Unsupported rawVersion: 0x%02X", vdsHeader.rawVersion));
			throw new IllegalArgumentException(String.format("Unsupported rawVersion: 0x%02X", vdsHeader.rawVersion));
		}
		// 2 bytes stores the three-letter country
		vdsHeader.issuingCountry = DataParser.decodeC40(DataParser.getFromByteBuffer(rawdata, 2));

		rawdata.mark();

		// 4 bytes stores first 6 characters of Signer & Certificate Reference
		String signerIdentifierAndCertRefLength = DataParser.decodeC40(DataParser.getFromByteBuffer(rawdata, 4));
		vdsHeader.signerIdentifier = signerIdentifierAndCertRefLength.substring(0, 4);

		if (vdsHeader.rawVersion == 0x03) { // ICAO version 4
			// the last two characters store the length of the following Certificate
			// Reference
			int certRefLength = Integer.parseInt(signerIdentifierAndCertRefLength.substring(4), 16);
			Logger.debug("version 4: certRefLength: {}", certRefLength);

			/*
			 * GAAD HACK: If signer is DEME and rawVersion is 0x03 (which is version 4
			 * according to ICAO spec) then anyhow use fixed size certification reference
			 * length and the length characters also used as certificate reference. e.g.
			 * DEME03123 signerIdenfifier = DEME length of certificate reference: 03 certRef
			 * = 03123 <-see: here the length is part of the certificate reference which is
			 * not the case in all other seals except the German
			 * "Arrival Attestation Document"
			 */
			boolean gaadHack = (vdsHeader.signerIdentifier.equals("DEME") || vdsHeader.signerIdentifier.equals("DES1"));
			if (gaadHack) {
				Logger.debug("Maybe we found a German Arrival Attestation. GAAD Hack will be applied!");
				certRefLength = 3;
			}
			// get number of bytes we have to decode to get the given certification
			// reference length
			int bytesToDecode = ((certRefLength - 1) / 3 * 2) + 2;
			Logger.debug("version 4: bytesToDecode: {}", bytesToDecode);
			vdsHeader.certificateReference = DataParser.decodeC40(DataParser.getFromByteBuffer(rawdata, bytesToDecode));
			if (gaadHack) {
				vdsHeader.certificateReference = signerIdentifierAndCertRefLength.substring(4)
						+ vdsHeader.certificateReference;
			}
		} else { // rawVersion=0x02 -> ICAO version 3
			rawdata.reset();
			String signerCertRef = DataParser.decodeC40(DataParser.getFromByteBuffer(rawdata, 6));
			vdsHeader.certificateReference = signerCertRef.substring(4);
		}

		vdsHeader.issuingDate = DataParser.decodeDate(DataParser.getFromByteBuffer(rawdata, 3));
		vdsHeader.sigDate = DataParser.decodeDate(DataParser.getFromByteBuffer(rawdata, 3));
		vdsHeader.docFeatureRef = rawdata.get();
		vdsHeader.docTypeCat = rawdata.get();
		Logger.debug("VdsHeader: {}", vdsHeader);
		return vdsHeader;
	}

	public static class Builder {
		private String issuingCountry;
		private String signerIdentifier;
		private String certificateReference;
		private LocalDate issuingDate = LocalDate.now();
		private LocalDate sigDate = LocalDate.now();
		private byte docFeatureRef;
		private byte docTypeCat;
		private byte rawVersion = 3;

		public Builder(String vdsType) {
			setDocumentType(vdsType);
		}

		public Builder setIssuingCountry(String issuingCountry) {
			this.issuingCountry = issuingCountry;
			return this;
		}

		public Builder setSignerIdentifier(String signerIdentifier) {
			this.signerIdentifier = signerIdentifier;
			return this;
		}

		public Builder setCertificateReference(String certificateReference) {
			this.certificateReference = certificateReference;
			return this;
		}

		public Builder setIssuingDate(LocalDate issuingDate) {
			this.issuingDate = issuingDate;
			return this;
		}

		public Builder setSigDate(LocalDate sigDate) {
			this.sigDate = sigDate;
			return this;
		}

		public Builder setRawVersion(int rawVersion) {
			this.rawVersion = (byte) rawVersion;
			return this;
		}

		public VdsHeader build() {
			return new VdsHeader(this);
		}

		/**
		 * Get signerIdentifier and certificateReference from given X509Certificate.
		 * 
		 * @param x509Cert                      X509Certificate to get the
		 *                                      signerIdentifier and the
		 *                                      certificateReference from
		 * @param setIssuingCountryFromX509Cert If true also build the issuing country
		 *                                      code base on the X509Certificate. It
		 *                                      will take the Country code 'C' and
		 *                                      convert it to a 3-letter country code.
		 * @return updated Builder instance
		 */
		public Builder setSignerCertRef(X509Certificate x509Cert, boolean setIssuingCountryFromX509Cert) {
			String[] signerCertRef = null;
			try {
				signerCertRef = DataEncoder.getSignerCertRef(x509Cert);
			} catch (InvalidNameException e) {
				Logger.error("Couldn't build header, because getSignerCertRef throws error: " + e.getMessage());
			}
			this.signerIdentifier = signerCertRef[0];
			this.certificateReference = signerCertRef[1];
			if (setIssuingCountryFromX509Cert) {
				this.issuingCountry = Doc9303CountryCodes.convertToIcaoOrIso3(signerCertRef[0].substring(0, 2));
			}
			return this;
		}

		private void setDocumentType(String vdsType) {
			int docRef = DataEncoder.getDocumentRef(vdsType);
			this.docFeatureRef = (byte) ((docRef >> 8) & 0xFF);
			this.docTypeCat = (byte) (docRef & 0xFF);
		}
	}

}
