package de.tsenger.vdstools

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.openssl3.Openssl3

actual fun getCryptoProvider(): CryptographyProvider {
    // Use openssl3 for Brainpool curves support in ios
    return CryptographyProvider.Openssl3
}