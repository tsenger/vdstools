package vdstools

import co.touchlab.kermit.Logger
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey

import java.io.IOException
import java.security.*
import java.security.interfaces.ECPrivateKey

class Signer {
    private var ecPrivKey: BCECPrivateKey? = null

    constructor(privKey: ECPrivateKey?) {
        this.ecPrivKey = privKey as BCECPrivateKey?
    }

    constructor(keyStore: KeyStore, keyStorePassword: String, keyAlias: String?) {
        try {
            this.ecPrivKey = keyStore.getKey(keyAlias, keyStorePassword.toCharArray()) as BCECPrivateKey
        } catch (e: KeyStoreException) {
            Logger.e("getPrivateKeyByAlias failed: " + e.message)
        } catch (e: UnrecoverableKeyException) {
            Logger.e("getPrivateKeyByAlias failed: " + e.message)
        } catch (e: NoSuchAlgorithmException) {
            Logger.e("getPrivateKeyByAlias failed: " + e.message)
        }
    }

    val fieldSize: Int
        get() = ecPrivKey!!.parameters.curve.fieldSize

    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeyException::class,
        SignatureException::class,
        InvalidAlgorithmParameterException::class,
        IOException::class,
        NoSuchProviderException::class
    )
    fun sign(dataToSign: ByteArray): ByteArray {
        if (ecPrivKey == null) {
            throw InvalidKeyException("private key not initialized. Load from file or generate new one.")
        }

        // Changed 02.12.2021:
        // Signature depends now on the curves bit length according to BSI TR-03116-2
        // 2024-10-20: even more precise Doc9309-13 chapter 2.4
        val fieldBitLength = fieldSize
        val ecdsaSign = if (fieldBitLength <= 224) {
            Signature.getInstance("SHA224withPLAIN-ECDSA", "BC")
        } else if (fieldBitLength <= 256) {
            Signature.getInstance("SHA256withPLAIN-ECDSA", "BC")
        } else if (fieldBitLength <= 384) {
            Signature.getInstance("SHA384withPLAIN-ECDSA", "BC")
        } else if (fieldBitLength <= 512) {
            Signature.getInstance("SHA512withPLAIN-ECDSA", "BC")
        } else {
            Logger.e("Bit length of Field is out of defined value: $fieldBitLength")
            throw InvalidAlgorithmParameterException(
                "Bit length of Field is out of defined value (224 to 512 bits): $fieldBitLength"
            )
        }

        Logger.i("ECDSA algorithm: " + ecdsaSign.algorithm)

        ecdsaSign.initSign(ecPrivKey)
        ecdsaSign.update(dataToSign)

        return ecdsaSign.sign()
    }
}
