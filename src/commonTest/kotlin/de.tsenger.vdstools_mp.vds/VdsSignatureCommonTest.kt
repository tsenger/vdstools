package de.tsenger.vdstools_mp.vds

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@OptIn(ExperimentalStdlibApi::class)
class VdsSignatureCommonTest {

    @Test
    fun testGetPlainSignatureBytes() {
        val signature = VdsSignature(plainSignature)
        assertEquals(
            "3c8b104fd4a8ad11157f87dadd05407f0cefa3ad0155c1179765933089896357e1b6fdbb3b2b003d6ee34875d6db833e05fffe9d99378eb01ae988c638c2eb27",
            signature.plainSignatureBytes.toHexString()
        )
    }

    @Test
    fun testGetDerSignatureBytes() {
        val signature = VdsSignature(plainSignature)
        assertEquals(
            "304502203c8b104fd4a8ad11157f87dadd05407f0cefa3ad0155c1179765933089896357022100e1b6fdbb3b2b003d6ee34875d6db833e05fffe9d99378eb01ae988c638c2eb27",
            signature.derSignatureBytes.toHexString()
        )
    }

    @Test
    fun testFromByteArray() {
        val vdsSigBytes = byteArrayOf(0xff.toByte(), 6, 1, 2, 3, 4, 5, 6)
        val signature = VdsSignature.fromByteArray(vdsSigBytes)
        assertNotNull(signature)
        assertEquals("010203040506", signature.plainSignatureBytes.toHexString())
        assertEquals("300a02030102030203040506", signature.derSignatureBytes.toHexString())
    }

    @Test
    fun testFromByteArray_IllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            VdsSignature.fromByteArray(plainSignature)
        }

    }

    companion object {
        val plainSignature: ByteArray =
            "3c8b104fd4a8ad11157f87dadd05407f0cefa3ad0155c1179765933089896357e1b6fdbb3b2b003d6ee34875d6db833e05fffe9d99378eb01ae988c638c2eb27".hexToByteArray()
    }
}
