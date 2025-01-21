package de.tsenger.vdstools.idb

import kotlin.test.Test
import kotlin.test.assertContentEquals

@OptIn(ExperimentalStdlibApi::class)
class IdbSignatureIosTest {

    var idbSignatureBytes: ByteArray = (
            "7f40a1bb9cc00b2ff63050bdfafe845396588ee3035fe2267d51549f55a6688aa9873e1c5a171c8983a1630e85642df982b510cf8eb28b0ce022cf0057582a7f0086"
            ).hexToByteArray()

    @Test
    fun testGetDerSignatureBytes() {
        val signature = IdbSignature.fromByteArray(idbSignatureBytes)
        assertContentEquals(
            "3045022100a1bb9cc00b2ff63050bdfafe845396588ee3035fe2267d51549f55a6688aa98702203e1c5a171c8983a1630e85642df982b510cf8eb28b0ce022cf0057582a7f0086".hexToByteArray(),
            signature!!.derSignatureBytes
        )
    }

    @Test
    fun testGetPlainSignatureBytes() {
        val signature = IdbSignature.fromByteArray(idbSignatureBytes)
        assertContentEquals(
            "a1bb9cc00b2ff63050bdfafe845396588ee3035fe2267d51549f55a6688aa9873e1c5a171c8983a1630e85642df982b510cf8eb28b0ce022cf0057582a7f0086".hexToByteArray(),
            signature!!.plainSignatureBytes
        )
    }

    @Test
    fun testGetEncoded() {
        val signature = IdbSignature(
            "a1bb9cc00b2ff63050bdfafe845396588ee3035fe2267d51549f55a6688aa9873e1c5a171c8983a1630e85642df982b510cf8eb28b0ce022cf0057582a7f0086".hexToByteArray()
        )
        assertContentEquals(idbSignatureBytes, signature.encoded)
    }

    @Test
    fun testFromByteArray() {
        val signature = IdbSignature.fromByteArray(idbSignatureBytes)
        assertContentEquals(
            "a1bb9cc00b2ff63050bdfafe845396588ee3035fe2267d51549f55a6688aa9873e1c5a171c8983a1630e85642df982b510cf8eb28b0ce022cf0057582a7f0086".hexToByteArray(),
            signature!!.plainSignatureBytes
        )
    }
}
