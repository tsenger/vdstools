package de.tsenger.vdstools_mp.idb

import kotlin.test.assertEquals
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalStdlibApi::class)
class IdbHeaderIosTest {

    @Test
    fun testConstructor_minimal() {
        val header = IdbHeader("D<<")
        assertEquals("6abc", header.encoded.toHexString())
    }

    @Test
    fun testConstructor_full() {
        val header = IdbHeader(
            "D<<", IdbSignatureAlgorithm.SHA256_WITH_ECDSA, byteArrayOf(1, 2, 3, 4, 5),
            "2014-10-18"
        )
        assertEquals("6abc010102030405009b5d7e", header.encoded.toHexString())
    }

    @Test
    fun testGetCountryIdentifier() {
        val header = IdbHeader.fromByteArray("6abc010102030405009b5d7e".hexToByteArray())
        assertEquals("D<<", header.getCountryIdentifier())
    }

    @Test
    fun testGetSignatureAlgorithm() {
        val header = IdbHeader.fromByteArray("6abc010102030405009b5d7e".hexToByteArray())
        assertEquals(IdbSignatureAlgorithm.SHA256_WITH_ECDSA, header.getSignatureAlgorithm())
    }

    @Test
    fun testGetSignatureAlgorithm_null() {
        val header = IdbHeader.fromByteArray("6abc".hexToByteArray())
        assertNull(header.getSignatureAlgorithm())
    }

    @Test
    fun testGetCertificateReference() {
        val header = IdbHeader.fromByteArray("6abc010102030405009b5d7e".hexToByteArray())
        assertNotNull(header)
        assertNotNull(header.certificateReference)
        assertEquals("0102030405", header.certificateReference!!.toHexString())
    }

    @Test
    fun testGetCertificateReference_null() {
        val header = IdbHeader.fromByteArray("6abc".hexToByteArray())
        assertNull(header.certificateReference)
    }

    @Test
    fun testGetSignatureCreationDate() {
        val header = IdbHeader.fromByteArray("6abc010102030405009b5d7e".hexToByteArray())
        assertEquals("2014-10-18", header.getSignatureCreationDate())
    }

    @Test
    fun testGetSignatureCreationDate_null() {
        val header = IdbHeader.fromByteArray("6abc".hexToByteArray())
        assertNull(header.getSignatureCreationDate())
    }

    @Test
    fun testFromByteArray_minimal() {
        val header = IdbHeader.fromByteArray("eb11".hexToByteArray())
        assertEquals("XKC", header.getCountryIdentifier())
        assertNull(header.getSignatureAlgorithm())
        assertNull(header.certificateReference)
        assertNull(header.getSignatureCreationDate())
    }

    @Test
    fun testFromByteArray_full() {
        val header = IdbHeader.fromByteArray("eb1101aabbccddee00bbddbf".hexToByteArray())
        assertEquals("XKC", header.getCountryIdentifier())
        assertEquals(IdbSignatureAlgorithm.SHA256_WITH_ECDSA, header.getSignatureAlgorithm())
        assertEquals("aabbccddee", header.certificateReference!!.toHexString())
        assertEquals("1999-12-31", header.getSignatureCreationDate())
    }
}
