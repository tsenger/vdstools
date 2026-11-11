package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import kotlin.test.*

/**
 * Building a VDS seal applies the same rules as reading it: a seal with WRONG_FORMAT findings
 * (missing required message, wrong length, ...) is rejected.
 */
@OptIn(ExperimentalStdlibApi::class)
class VdsBuilderFormatCheckCommonTest {

    private val dummySigner = object : Signer {
        override val fieldSize = 256
        override fun sign(data: ByteArray) = ByteArray(64)
    }

    @BeforeTest
    fun setUp() {
        DataEncoder.resetToDefaults()
    }

    private fun socialInsuranceCard() = VdsSeal.Builder("SOCIAL_INSURANCE_CARD")
        .addMessage("SOCIAL_INSURANCE_NUMBER", "65170839J003")
        .addMessage("SURNAME", "Mustermann")

    @Test
    fun completeSeal_builds() {
        val seal = socialInsuranceCard().addMessage("FIRST_NAME", "Erika").build(dummySigner)
        assertEquals(emptyList(), seal.formatFindings)
    }

    @Test
    fun missingRequiredMessage_isRejected() {
        // TR-03137 §7.2: FIRST_NAME is required
        val e = assertFailsWith<IllegalArgumentException> { socialInsuranceCard().build(dummySigner) }
        assertTrue(e.message!!.contains("FIRST_NAME"), e.message)
    }

    @Test
    fun invalidLength_isRejected() {
        val e = assertFailsWith<IllegalArgumentException> {
            socialInsuranceCard().addMessage("FIRST_NAME", "x".repeat(91)).build(dummySigner)
        }
        assertTrue(e.message!!.contains("FIRST_NAME") && e.message!!.contains("91"), e.message)
    }

    @Test
    fun visaWithoutMrz_isRejected() {
        val e = assertFailsWith<IllegalArgumentException> {
            VdsSeal.Builder("VISA")
                .addMessage("DURATION_OF_STAY", "5a0000".hexToByteArray())
                .addMessage("PASSPORT_NUMBER", "ALAAF5099")
                .build(dummySigner)
        }
        assertTrue(e.message!!.contains("MRZ_MRVA or MRZ_MRVB"), e.message)
    }

    @Test
    fun typeWithoutSpecification_isNotRejectedForMissingMessages() {
        // a definition that is not verified against a specification is not enforced
        DataEncoder.addCustomVdsDocumentTypes(
            """[{"documentType": "UNVERIFIED_TYPE", "documentRef": "fcfd", "version": 4, "messages": [
                {"name": "A", "tag": 1, "coding": "BYTES", "required": true, "minBytes": 1, "maxBytes": 2},
                {"name": "B", "tag": 2, "coding": "BYTES", "required": false, "minBytes": 1, "maxBytes": 2}]}]"""
        )
        val seal = VdsSeal.Builder("UNVERIFIED_TYPE").addMessage("B", "aabbcc".hexToByteArray()).build(dummySigner)
        assertEquals("UNVERIFIED_TYPE", seal.documentType)
    }
}
