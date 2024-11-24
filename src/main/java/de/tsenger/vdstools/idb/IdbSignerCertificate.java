package de.tsenger.vdstools.idb;

import de.tsenger.vdstools.DerTlv;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class IdbSignerCertificate {

	public final static byte TAG = 0x7E;

	private final X509Certificate cert;

	public IdbSignerCertificate(X509Certificate cert) {
		if (cert == null)
			throw new IllegalArgumentException("Certificate must not be null!");
		this.cert = cert;
	}

	public static IdbSignerCertificate fromByteArray(byte[] rawBytes) throws CertificateException, IOException {
		if (rawBytes[0] != TAG) {
			throw new IllegalArgumentException(String.format(
					"IdbSignerCertificate shall have tag %2X, but tag %2X was found instead.", TAG, rawBytes[0]));
		}
		DerTlv derTlv = DerTlv.fromByteArray(rawBytes);
		X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
				.generateCertificate(new ByteArrayInputStream(derTlv.getValue()));
		return new IdbSignerCertificate(cert);
	}

	public byte[] getEncoded() throws CertificateEncodingException, IOException {
		return new DerTlv(TAG, cert.getEncoded()).getEncoded();
	}

	public X509Certificate getX509Certificate() {
		return this.cert;
	}

}
