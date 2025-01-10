package de.tsenger.vdstools_mp

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.jdk.JDK
import org.bouncycastle.jce.provider.BouncyCastleProvider

actual fun getCryptoProvider(): CryptographyProvider {
    return CryptographyProvider.JDK(BouncyCastleProvider())
}