package de.tsenger.vdstools_mp.idb

import de.tsenger.vdstools_mp.idb.IdbSignature.Companion.fromByteArray
import org.bouncycastle.util.Arrays
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test
import java.io.IOException

class IdbSignatureTest {
    var idbSignatureBytes: ByteArray = Hex.decode(
        "7f40a1bb9cc00b2ff63050bdfafe845396588ee3035fe2267d51549f55a6688aa9873e1c5a171c8983a1630e85642df982b510cf8eb28b0ce022cf0057582a7f0086"
    )

    @Test
    @Throws(IOException::class)
    fun testGetDerSignatureBytes() {
        val signature = fromByteArray(idbSignatureBytes)
        Assert.assertEquals(
            "3045022100a1bb9cc00b2ff63050bdfafe845396588ee3035fe2267d51549f55a6688aa98702203e1c5a171c8983a1630e85642df982b510cf8eb28b0ce022cf0057582a7f0086",
            Hex.toHexString(signature!!.derSignatureBytes)
        )
    }

    @Test
    @Throws(IOException::class)
    fun testGetPlainSignatureBytes() {
        val signature = fromByteArray(idbSignatureBytes)
        Assert.assertEquals(
            "a1bb9cc00b2ff63050bdfafe845396588ee3035fe2267d51549f55a6688aa9873e1c5a171c8983a1630e85642df982b510cf8eb28b0ce022cf0057582a7f0086",
            Hex.toHexString(signature!!.plainSignatureBytes)
        )
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncoded() {
        val signature = IdbSignature(
            Hex.decode(
                "a1bb9cc00b2ff63050bdfafe845396588ee3035fe2267d51549f55a6688aa9873e1c5a171c8983a1630e85642df982b510cf8eb28b0ce022cf0057582a7f0086"
            )
        )
        Assert.assertTrue(Arrays.areEqual(idbSignatureBytes, signature.encoded))
    }

    @Test
    @Throws(IOException::class)
    fun testFromByteArray() {
        val signature = fromByteArray(idbSignatureBytes)
        Assert.assertEquals(
            "a1bb9cc00b2ff63050bdfafe845396588ee3035fe2267d51549f55a6688aa9873e1c5a171c8983a1630e85642df982b510cf8eb28b0ce022cf0057582a7f0086",
            Hex.toHexString(signature!!.plainSignatureBytes)
        )
    }
}
