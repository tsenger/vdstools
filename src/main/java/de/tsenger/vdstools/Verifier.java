package de.tsenger.vdstools;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.tinylog.Logger;

import de.tsenger.vdstools.seals.DigitalSeal;

public class Verifier {

	public enum Result {
		SignatureValid, SignatureInvalid, VerifyError,
	}

	private ECPublicKey ecPubKey;
	private int keySize = 256;
	private byte[] messageBytes;
	private byte[] signatureBytes;

	String signaturAlgorithmName = "SHA256WITHECDSA";

	public Verifier(DigitalSeal digitalSeal, X509Certificate sealSignerCertificate) {
		Security.addProvider(new BouncyCastleProvider());
		if (!(sealSignerCertificate.getPublicKey() instanceof ECPublicKey)) {
			throw new IllegalArgumentException("Certificate should contain EC public key!");
		}
		ecPubKey = (ECPublicKey) sealSignerCertificate.getPublicKey();
		this.keySize = ecPubKey.getParams().getCurve().getField().getFieldSize();
		this.messageBytes = digitalSeal.getHeaderAndMessageBytes();
		this.signatureBytes = digitalSeal.getSignatureBytes();

		Logger.debug("Public Key bytes: 0x{}", Hex.toHexString(ecPubKey.getEncoded()));
		Logger.debug("Public Key size: {}", this.keySize);
		Logger.debug("Message bytes: {}", Hex.toHexString(messageBytes));
		Logger.debug("Signature bytes: {}", Hex.toHexString(signatureBytes));
	}

	public Result verify() {
		// Based on the length of the signature, the hash algorithm is determined
		// TODO is there a better solution? Maybe based on the Public Key size?
		// TODO in ICAO9303 p13 ch2.4 it is defined!
		// Should be based on bit length of the order of the base point generator G
		if (signatureBytes[1] > 0x46) {
			signaturAlgorithmName = "SHA384WITHECDSA";
		} else if (signatureBytes[1] < 0x3F) {
			signaturAlgorithmName = "SHA224WITHECDSA";
		}

		try {
			Signature ecdsaVerify = Signature.getInstance(signaturAlgorithmName, "BC");
			ecdsaVerify.initVerify(ecPubKey);
			ecdsaVerify.update(messageBytes);

			if (ecdsaVerify.verify(signatureBytes)) {
				return Result.SignatureValid;
			} else {
				return Result.SignatureInvalid;
			}
		} catch (NoSuchAlgorithmException e1) {
			Logger.error("NoSuchAlgorithmException: {}", e1.getLocalizedMessage());
			return Result.VerifyError;
		} catch (InvalidKeyException e2) {
			Logger.error("InvalidKeyException: {}", e2.getLocalizedMessage());
			return Result.VerifyError;
		} catch (SignatureException e3) {
			Logger.error("SignatureException: {}", e3.getLocalizedMessage());
			return Result.VerifyError;
		} catch (NoSuchProviderException e4) {
			Logger.error("NoSuchProviderException: {}", e4.getLocalizedMessage());
			return Result.VerifyError;
		}

	}

}
