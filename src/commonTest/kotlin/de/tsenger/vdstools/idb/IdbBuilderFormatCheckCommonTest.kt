package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import kotlin.test.*

/**
 * Building an IDB seal applies the same rules as reading it: a seal with WRONG_FORMAT findings
 * (missing required message, message not allowed, wrong length) is rejected, before it is signed.
 */
class IdbBuilderFormatCheckCommonTest {

    @BeforeTest
    fun setUp() {
        DataEncoder.resetToDefaults()
    }

    private fun permanentResidence() = IdbSeal.Builder("CERTIFYING_PERMANENT_RESIDENCE").countryIdentifier("D<<")
        .addMessage("DOCUMENT_REFERENCE", "ABC123456")

    @Test
    fun completeSeal_builds() {
        val seal = permanentResidence().addMessage("DOCUMENT_NUMBER", "ZZ1234567").build()
        assertEquals(emptyList(), seal.formatFindings)
    }

    @Test
    fun missingRequiredMessage_isRejected() {
        val e = assertFailsWith<IllegalArgumentException> { permanentResidence().build() }
        assertEquals(
            "Invalid seal content (CERTIFYING_PERMANENT_RESIDENCE): DOCUMENT_NUMBER (tag 0x85) is required",
            e.message
        )
    }

    @Test
    fun messageNotAllowed_isRejected() {
        val e = assertFailsWith<IllegalArgumentException> {
            permanentResidence().addMessage("DOCUMENT_NUMBER", "ZZ1234567").addMessage("CAN", "123456").build()
        }
        assertTrue(e.message!!.contains("CAN (tag 0x09) must not be present"), e.message)
    }

    @Test
    fun faceImageTooLarge_isRejectedWithoutDocumentType() {
        val e = assertFailsWith<IllegalArgumentException> {
            IdbSeal.Builder().countryIdentifier("D<<").addMessage("FACE_IMAGE", ByteArray(1200)).build()
        }
        assertEquals("Invalid seal content (IDB): FACE_IMAGE (tag 0x80) has length 1200, allowed 900..1000", e.message)
    }

    @Test
    fun invalidSeal_isNotSigned() {
        var signed = false
        val signer = object : Signer {
            override val fieldSize = 256
            override fun sign(data: ByteArray): ByteArray {
                signed = true
                return ByteArray(64)
            }
        }
        assertFailsWith<IllegalArgumentException> { permanentResidence().build(signer) }
        assertFalse(signed)
    }
}
