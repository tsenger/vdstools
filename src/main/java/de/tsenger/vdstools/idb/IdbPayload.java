package de.tsenger.vdstools.idb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.DerTlv;

public class IdbPayload {

	private IdbHeader idbHeader;
	private IdbMessageGroup idbMessageGroup;
	private IdbSignerCertificate idbSignerCertificate;
	private IdbSignature idbSignature;

	public IdbPayload(IdbHeader idbHeader, IdbMessageGroup idbMessageGroup,
			IdbSignerCertificate idbSignerCertificate, IdbSignature idbSignature) {
		this.idbHeader = idbHeader;
		this.idbMessageGroup = idbMessageGroup;
		this.idbSignerCertificate = idbSignerCertificate;
		this.idbSignature = idbSignature;
	}

	public IdbHeader getIdbHeader() {
		return idbHeader;
	}

	public IdbMessageGroup getIdbMessageGroup() {
		return idbMessageGroup;
	}

	public IdbSignerCertificate getIdbSignerCertificate() {
		return idbSignerCertificate;
	}

	public IdbSignature getIdbSignature() {
		return idbSignature;
	}

	public byte[] getEncoded() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bos.write(idbHeader.getEncoded());
		bos.write(idbMessageGroup.getEncoded());
		if (idbSignerCertificate != null)
			bos.write(idbMessageGroup.getEncoded());
		if (idbSignature != null) {
			bos.write(idbSignature.getEncoded());
		} else if (idbHeader.getSignatureAlgorithm() != null) {
			Logger.error(
					"Missing Signature Field! This field should be present if a signature algorithm has been specified in the header.");
			return null;
		}
		return bos.toByteArray();
	}

	public static IdbPayload fromByteArray(byte[] rawBytes, boolean isSigned)
			throws CertificateException, IOException {
		IdbHeader idbHeader = null;
		IdbMessageGroup idbMessageGroup = null;
		IdbSignerCertificate idbSignerCertificate = null;
		IdbSignature idbSignature = null;
		int offset = 0;
		if (isSigned) {
			idbHeader = IdbHeader.fromByteArray(Arrays.copyOfRange(rawBytes, offset, offset += 12));
		} else {
			idbHeader = IdbHeader.fromByteArray(Arrays.copyOfRange(rawBytes, offset, offset += 2));
		}
		List<DerTlv> derTlvList = DataParser.parseDerTLvs(Arrays.copyOfRange(rawBytes, offset, rawBytes.length));

		for (DerTlv derTlv : derTlvList) {
			switch (derTlv.getTag()) {
			case IdbMessageGroup.TAG:
				idbMessageGroup = IdbMessageGroup.fromByteArray(derTlv.getValue());
				break;
			case IdbSignerCertificate.TAG:
				idbSignerCertificate = IdbSignerCertificate.fromByteArray(derTlv.getValue());
				break;
			case IdbSignature.TAG:
				idbSignature = new IdbSignature(derTlv.getValue());
				break;
			}
		}

		return new IdbPayload(idbHeader, idbMessageGroup, idbSignerCertificate, idbSignature);
	}

}
