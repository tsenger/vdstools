package de.tsenger.vdstools_mp

import dev.whyoleg.cryptography.CryptographyProvider

actual fun getCryptoProvider(): CryptographyProvider {
    return CryptographyProvider.Default
}