package de.tsenger.vdstools.idb;

public class IdbHeader {
	private byte[] countryIdentifier;
	private byte signatureAlgorithm;
	private byte[] certificateReference;
	private byte[] signatureCreationDate;
	
	public IdbHeader(byte[] countryIdentifier, byte signatureAlgorithm, byte[] certificateReference,
			byte[] signatureCreationDate) {
		super();
		this.countryIdentifier = countryIdentifier;
		this.signatureAlgorithm = signatureAlgorithm;
		this.certificateReference = certificateReference;
		this.signatureCreationDate = signatureCreationDate;
	}
	
	public byte[] getCountryIdentifier() {
		return countryIdentifier;
	}
	public void setCountryIdentifier(byte[] countryIdentifier) {
		this.countryIdentifier = countryIdentifier;
	}
	public byte getSignatureAlgorithm() {
		return signatureAlgorithm;
	}
	public void setSignatureAlgorithm(byte signatureAlgorithm) {
		this.signatureAlgorithm = signatureAlgorithm;
	}
	public byte[] getCertificateReference() {
		return certificateReference;
	}
	public void setCertificateReference(byte[] certificateReference) {
		this.certificateReference = certificateReference;
	}
	public byte[] getSignatureCreationDate() {
		return signatureCreationDate;
	}
	public void setSignatureCreationDate(byte[] signatureCreationDate) {
		this.signatureCreationDate = signatureCreationDate;
	}
}
