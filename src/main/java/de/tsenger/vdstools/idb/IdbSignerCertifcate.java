package de.tsenger.vdstools.idb;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import de.tsenger.vdstools.DerTlv;

public class IdbSignerCertifcate {

	public final static byte TAG = 0x7E;

	private X509Certificate cert;

	public IdbSignerCertifcate(X509Certificate cert) {
		this.cert = cert;
	}

	public IdbSignerCertifcate(byte[] certBytes) throws CertificateException, IOException {
		if (certBytes[0] == TAG) {
			certBytes = DerTlv.fromByteArray(certBytes).getValue();
		}
		this.cert = (X509Certificate) CertificateFactory.getInstance("X.509")
				.generateCertificate(new ByteArrayInputStream(certBytes));
	}

	public byte[] getEncoded() throws CertificateEncodingException, IOException {
		return new DerTlv(TAG, cert.getEncoded()).getEncoded();
	}

	public X509Certificate getX509Certificate() {
		return this.cert;
	}

}
