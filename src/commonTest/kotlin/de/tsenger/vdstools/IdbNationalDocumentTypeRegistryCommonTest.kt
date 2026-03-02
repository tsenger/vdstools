package de.tsenger.vdstools

import de.tsenger.vdstools.idb.dto.IdbMessageTypeRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IdbNationalDocumentTypeRegistryCommonTest {

    private val registry = IdbNationalDocumentTypeRegistry(
        readTextResource("IdbNationalDocumentTypes.json")
    )

    @Test
    fun testParseJsonLoadsAllEntries() {
        assertEquals(18, registry.availableDocumentTypes().size)
    }

    @Test
    fun testGetDocumentTypeByTag() {
        assertEquals("SUBSTITUTE_IDENTITY_DOCUMENT", registry.getDocumentType(1))
        assertEquals("CERTIFYING_PERMANENT_RESIDENCE", registry.getDocumentType(16))
        assertEquals("SUPPLEMENTARY_SHEET_FOR_RESIDENCE_PERMIT_CARD", registry.getDocumentType(18))
    }

    @Test
    fun testGetDocumentTypeByName() {
        assertEquals(1, registry.getDocumentType("SUBSTITUTE_IDENTITY_DOCUMENT"))
        assertEquals(16, registry.getDocumentType("CERTIFYING_PERMANENT_RESIDENCE"))
        assertEquals(18, registry.getDocumentType("SUPPLEMENTARY_SHEET_FOR_RESIDENCE_PERMIT_CARD"))
    }

    @Test
    fun testGetExpectedMessagesByTag_returnsCorrectMessages() {
        val messages = registry.getExpectedMessages(1)
        assertEquals(4, messages.size)
        assertTrue(messages.any { it.name == "FACE_IMAGE" })
        assertTrue(messages.any { it.name == "MRZ_TD2" })
        assertTrue(messages.any { it.name == "EXPIRY_DATE" })
        assertTrue(messages.any { it.name == "NATIONAL_DOCUMENT_IDENTIFIER" })
    }

    @Test
    fun testGetExpectedMessagesByName_returnsCorrectMessages() {
        val messages = registry.getExpectedMessages("SUBSTITUTE_IDENTITY_DOCUMENT")
        assertEquals(4, messages.size)
        assertTrue(messages.any { it.name == "FACE_IMAGE" })
        assertTrue(messages.any { it.name == "MRZ_TD2" })
        assertTrue(messages.any { it.name == "EXPIRY_DATE" })
        assertTrue(messages.any { it.name == "NATIONAL_DOCUMENT_IDENTIFIER" })
    }

    @Test
    fun testGetExpectedMessages_allRequired() {
        // All messages for SUBSTITUTE_IDENTITY_DOCUMENT are mandatory
        val messages = registry.getExpectedMessages(1)
        assertTrue(messages.all { it.required })
    }

    @Test
    fun testGetExpectedMessages_differentCompositions() {
        // Temporary passports use MRZ_TD3, identity docs use MRZ_TD2
        val passportMessages = registry.getExpectedMessages(6) // TEMPORARY_PASSPORT
        assertTrue(passportMessages.any { it.name == "MRZ_TD3" })

        val identityMessages = registry.getExpectedMessages(1) // SUBSTITUTE_IDENTITY_DOCUMENT
        assertTrue(identityMessages.any { it.name == "MRZ_TD2" })
    }

    @Test
    fun testGetExpectedMessages_azrDocumentsHaveAzr() {
        // Immigration legal documents (11-13) contain AZR instead of EXPIRY_DATE
        val messages = registry.getExpectedMessages(11) // PERMISSION_TO_RETAIN_PENDING_THE_ASYLUM_DECISION
        assertTrue(messages.any { it.name == "AZR" })
        assertTrue(messages.none { it.name == "EXPIRY_DATE" })
    }

    @Test
    fun testGetExpectedMessages_provisionalResidenceHasMrzTd1AndAzr() {
        val messages = registry.getExpectedMessages(14) // PROVISIONAL_RESIDENCE_DOCUMENT
        assertTrue(messages.any { it.name == "MRZ_TD1" })
        assertTrue(messages.any { it.name == "AZR" })
        assertTrue(messages.any { it.name == "DOCUMENT_REFERENCE" })
        assertTrue(messages.any { it.name == "NATIONAL_DOCUMENT_IDENTIFIER" })
    }

    @Test
    fun testGetExpectedMessages_certifyingPermanentResidenceMinimalMessages() {
        // CERTIFYING_PERMANENT_RESIDENCE only has DOCUMENT_NUMBER and NATIONAL_DOCUMENT_IDENTIFIER
        val messages = registry.getExpectedMessages(16)
        assertEquals(2, messages.size)
        assertTrue(messages.any { it.name == "DOCUMENT_NUMBER" })
        assertTrue(messages.any { it.name == "NATIONAL_DOCUMENT_IDENTIFIER" })
    }

    @Test
    fun testGetExpectedMessages_unknownTagReturnsEmpty() {
        val messages = registry.getExpectedMessages(99)
        assertTrue(messages.isEmpty())
    }

    @Test
    fun testGetExpectedMessages_unknownNameReturnsEmpty() {
        val messages = registry.getExpectedMessages("NOT_A_REAL_TYPE")
        assertTrue(messages.isEmpty())
    }
}