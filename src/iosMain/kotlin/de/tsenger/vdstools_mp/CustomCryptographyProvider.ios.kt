package de.tsenger.vdstools_mp

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.openssl3.Openssl3

actual fun getCryptoProvider(): CryptographyProvider {
    return CryptographyProvider.Openssl3
}