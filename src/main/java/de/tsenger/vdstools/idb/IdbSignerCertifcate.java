package de.tsenger.vdstools.idb;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.bouncycastle.util.Arrays;

public class IdbSignerCertifcate {

	public final static byte TAG = 0x7E;

	private X509Certificate cert;

	public IdbSignerCertifcate(X509Certificate cert) {
		this.cert = cert;
	}

	public IdbSignerCertifcate(byte[] certBytes) throws CertificateException {
		if (certBytes[0] == TAG) {
			certBytes = Arrays.copyOfRange(certBytes, 1, certBytes.length);
		}
		this.cert = (X509Certificate) CertificateFactory.getInstance("X.509")
				.generateCertificate(new ByteArrayInputStream(certBytes));
	}

	public byte[] getEncoded() throws CertificateEncodingException {
		return Arrays.concatenate(new byte[] { TAG }, cert.getEncoded());
	}

	public X509Certificate getX509Certificate() {
		return this.cert;
	}

}
