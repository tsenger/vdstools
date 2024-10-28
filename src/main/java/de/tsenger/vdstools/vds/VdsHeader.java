package de.tsenger.vdstools.vds;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDate;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;

/**
 * @author Tobias Senger
 *
 */
public class VdsHeader {

	public static final byte DC = (byte) 0xDC;

	private byte[] rawBytes = null;

	public String issuingCountry;
	public String signerIdentifier;
	public String certificateReference;

	public LocalDate issuingDate;
	public LocalDate sigDate;

	public byte docFeatureRef;
	public byte docTypeCat;

	public byte rawVersion;

	private VdsHeader() {
	}

	public VdsHeader(String vdsType) {
		super();
		this.setDocumentType(vdsType);
	}

	public int getDocumentRef() {
		return ((docFeatureRef & 0xFF) << 8) + (docTypeCat & 0xFF);
	}

	public void setDocumentType(String vdsType) {
		int docRef = DataEncoder.getFeatureEncoder().getDocumentRef(vdsType);
		docFeatureRef = (byte) ((docRef >> 8) & 0xFF);
		docTypeCat = (byte) (docRef & 0xFF);
	}

	public String getVdsType() {
		return DataEncoder.getFeatureEncoder().getVdsType(getDocumentRef());
	}

	public byte[] getEncoded() {
		if (rawBytes == null) {
			encode();
		}
		return rawBytes;
	}

	private void encode() {
		if (sigDate == null) {
			sigDate = LocalDate.now();
		}
		if (issuingDate == null) {
			issuingDate = LocalDate.now();
		}
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
		rawBytes = baos.toByteArray();
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
		 * new in ICAO spec for "Visual Digital Seals for Non-Electronic Documents":
		 * value 0x02 stands for version 3 (uses fix length of Document Signer
		 * Reference: 5 characters) value 0x03 stands for version 4 (uses variable
		 * length of Document Signer Reference) Problem: German "Arrival Attestation
		 * Document" uses value 0x03 for rawVersion 3 and static length of Document
		 * Signer Reference.
		 */
		if (!(vdsHeader.rawVersion == 0x02 || vdsHeader.rawVersion == 0x03)) {
			Logger.error(String.format("Unsupported rawVersion: 0x%02X", vdsHeader.rawVersion));
			throw new IllegalArgumentException(String.format("Unsupported rawVersion: 0x%02X", vdsHeader.rawVersion));
		}
		vdsHeader.issuingCountry = DataParser.decodeC40(DataParser.getFromByteBuffer(rawdata, 2)); // 2 bytes stores the
																									// three letter
																									// country
		// code
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
			 * length and the length characters also used as certificate reference. eg.
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
//        vdsHeader.setRawBytes(Arrays.copyOfRange(rawdata.array(), 0, rawdata.position()));
		Logger.debug("VdsHeader: {}", vdsHeader);
		return vdsHeader;
	}

	@Override
	public String toString() {
		return ("rawVersion: " + (rawVersion & 0xff) + "\nissuingCountry: " + issuingCountry + "\nsignerIdentifier: "
				+ signerIdentifier + "\ncertificateReference: " + certificateReference + "\nissuingDate: " + issuingDate
				+ "\nsigDate: " + sigDate + "\ndocFeatureRef: " + String.format("%02X ", docFeatureRef)
				+ ", docTypeCat: " + String.format("%02X ", docTypeCat));
	}

}
