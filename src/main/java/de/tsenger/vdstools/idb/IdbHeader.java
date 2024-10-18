package de.tsenger.vdstools.idb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.time.LocalDate;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;

public class IdbHeader {
	private byte[] countryIdentifier = null;
	private byte signatureAlgorithm = 0;
	private byte[] certificateReference = null;
	private byte[] signatureCreationDate = null;

	private IdbHeader(byte[] countryIdentifier, byte signatureAlgorithm, byte[] certificateReference,
			byte[] signatureCreationDate) {
		this.countryIdentifier = countryIdentifier;
		this.signatureAlgorithm = signatureAlgorithm;
		this.certificateReference = certificateReference;
		this.signatureCreationDate = signatureCreationDate;
	}

	public IdbHeader(String countryIdentifier) {
		this(countryIdentifier, null, null, null);
	}

	public IdbHeader(String countryIdentifier, IdbSignatureAlgorithm signatureAlgorithm, byte[] certificateReference) {
		this(countryIdentifier, signatureAlgorithm, certificateReference, LocalDate.now().toString());
	}

	public IdbHeader(String countryIdentifier, IdbSignatureAlgorithm signatureAlgorithm, byte[] certificateReference,
			String signatureCreationDate) {
		if (countryIdentifier.length() != 3)
			throw new IllegalArgumentException("countryIdentifier must be a 3-letter String");
		this.countryIdentifier = DataEncoder.encodeC40(countryIdentifier);
		if (signatureAlgorithm != null)
			this.signatureAlgorithm = signatureAlgorithm.getValue();
		this.certificateReference = certificateReference;
		if (signatureCreationDate != null)
			this.signatureCreationDate = DataEncoder.encodeMaskedDate(signatureCreationDate);
	}

	public String getCountryIdentifier() {
		if (countryIdentifier == null)
			return null;
		return DataParser.decodeC40(countryIdentifier).replaceAll(" ", "<");
	}

	public IdbSignatureAlgorithm getSignatureAlgorithm() {
		if (signatureAlgorithm == 0)
			return null;
		return IdbSignatureAlgorithm.valueOf(signatureAlgorithm);
	}

	public byte[] getCertificateReference() {
		return certificateReference;
	}

	public String getSignatureCreationDate() {
		if (signatureCreationDate == null)
			return null;
		return DataParser.decodeMaskedDate(signatureCreationDate);
	}

	public byte[] getEncoded() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bos.write(countryIdentifier);
		if (signatureAlgorithm != 0)
			bos.write(signatureAlgorithm);
		if (certificateReference != null)
			bos.write(certificateReference);
		if (signatureCreationDate != null)
			bos.write(signatureCreationDate);
		return bos.toByteArray();
	}

	public static IdbHeader fromByteArray(byte[] rawBytes) {
		if (rawBytes.length > 12 || rawBytes.length < 2)
			throw new IllegalArgumentException("Header must have a length between 2 and 12 bytes");
		ByteBuffer rawData = ByteBuffer.wrap(rawBytes);
		byte[] countryIdentifier = null;
		byte signatureAlgorithm = 0;
		byte[] certificateReference = null;
		byte[] signatureCreationDate = null;
		try {
			countryIdentifier = DataParser.getFromByteBuffer(rawData, 2);
			signatureAlgorithm = rawData.get();
			certificateReference = DataParser.getFromByteBuffer(rawData, 5);
			signatureCreationDate = DataParser.getFromByteBuffer(rawData, 4);
		} catch (BufferUnderflowException e) {
			Logger.info("Header length is under 12 bytes.");
		}
		return new IdbHeader(countryIdentifier, signatureAlgorithm, certificateReference, signatureCreationDate);
	}
}
