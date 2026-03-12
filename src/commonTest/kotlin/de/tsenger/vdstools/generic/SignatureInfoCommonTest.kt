package de.tsenger.vdstools.generic

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class SignatureInfoCommonTest {

    @Test
    fun testGetDerSignatureBytes1() {
        val plainSignatureBytes: ByteArray =
            "3c8b104fd4a8ad11157f87dadd05407f0cefa3ad0155c1179765933089896357e1b6fdbb3b2b003d6ee34875d6db833e05fffe9d99378eb01ae988c638c2eb27".hexToByteArray()
        val signatureInfo = SignatureInfo(plainSignatureBytes, "DEUT08150", LocalDate(2015, 10, 11), byteArrayOf(0, 1))
        assertEquals(
            "304502203c8b104fd4a8ad11157f87dadd05407f0cefa3ad0155c1179765933089896357022100e1b6fdbb3b2b003d6ee34875d6db833e05fffe9d99378eb01ae988c638c2eb27",
            signatureInfo.derSignatureBytes.toHexString()
        )
    }

    @Test
    fun testGetDerSignatureBytes2() {
        val plainSignatureBytes: ByteArray =
            "a1bb9cc00b2ff63050bdfafe845396588ee3035fe2267d51549f55a6688aa9873e1c5a171c8983a1630e85642df982b510cf8eb28b0ce022cf0057582a7f0086".hexToByteArray()
        val signatureInfo = SignatureInfo(plainSignatureBytes, "DEUT08150", LocalDate(2015, 10, 11), byteArrayOf(0, 1))
        assertEquals(
            "3045022100a1bb9cc00b2ff63050bdfafe845396588ee3035fe2267d51549f55a6688aa98702203e1c5a171c8983a1630e85642df982b510cf8eb28b0ce022cf0057582a7f0086",
            signatureInfo.derSignatureBytes.toHexString()
        )
    }
}