package de.tsenger.vdstools;

import de.tsenger.vdstools.vds.DigitalSeal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.tinylog.Logger;

import java.security.*;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;

public class Verifier {

	public enum Result {
		SignatureValid, SignatureInvalid, VerifyError,
	}

	private final ECPublicKey ecPubKey;
	private final int fieldBitLength;
	private final byte[] messageBytes;
	private final byte[] signatureBytes;

	String signatureAlgorithmName = "SHA256WITHECDSA";

	public Verifier(DigitalSeal digitalSeal, X509Certificate sealSignerCertificate) {
		Security.addProvider(new BouncyCastleProvider());
		if (!(sealSignerCertificate.getPublicKey() instanceof ECPublicKey)) {
			throw new IllegalArgumentException("Certificate should contain EC public key!");
		}
		ecPubKey = (ECPublicKey) sealSignerCertificate.getPublicKey();
		this.fieldBitLength = ecPubKey.getParams().getCurve().getField().getFieldSize();
		this.messageBytes = digitalSeal.getHeaderAndMessageBytes();
		this.signatureBytes = digitalSeal.getSignatureBytes();

		Logger.debug("Public Key bytes: 0x{}", Hex.toHexString(ecPubKey.getEncoded()));
		Logger.debug("Field bit length: {}", this.fieldBitLength);
		Logger.debug("Message bytes: {}", Hex.toHexString(messageBytes));
		Logger.debug("Signature bytes: {}", Hex.toHexString(signatureBytes));
	}

	public Result verify() {
		// Changed 2024-10-20
		// Signature Algorithm is selected based on the field bit length of the curve
		// as defined in ICAO9303 p13 ch2.4

		if (fieldBitLength <= 224) {
			signatureAlgorithmName = "SHA224withPLAIN-ECDSA";
		} else if (fieldBitLength <= 256) {
			signatureAlgorithmName = "SHA256withPLAIN-ECDSA";
		} else if (fieldBitLength <= 384) {
			signatureAlgorithmName = "SHA384withPLAIN-ECDSA";
		} else if (fieldBitLength <= 512) {
			signatureAlgorithmName = "SHA512withPLAIN-ECDSA";
		} else {
			Logger.error("Bit length of Field is out of definied value: " + fieldBitLength);
			return Result.VerifyError;
		}

		try {
			Logger.debug("Verify with signatureAlgorithmName: " + signatureAlgorithmName);
			Signature ecdsaVerify = Signature.getInstance(signatureAlgorithmName, "BC");
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
