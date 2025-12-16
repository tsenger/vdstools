package de.tsenger.vdstools

import okio.FileNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReadTextResourceIosTest {

    @Test
    fun testReadSealCodings() {
        val content = readTextResource(DEFAULT_SEAL_CODINGS)
        assertNotNull(content)
        assertTrue(content.isNotEmpty())
        assertTrue(content.contains("documentType") || content.contains("features"))
    }

    @Test
    fun testReadIdbMessageTypes() {
        val content = readTextResource(DEFAULT_IDB_MESSAGE_TYPES)
        assertNotNull(content)
        assertTrue(content.isNotEmpty())
        assertTrue(content.contains("VISA") || content.contains("EMERGENCY_TRAVEL_DOCUMENT"))
    }

    @Test
    fun testReadIdbDocumentTypes() {
        val content = readTextResource(DEFAULT_IDB_DOCUMENT_TYPES)
        assertNotNull(content)
        assertTrue(content.isNotEmpty())
        assertTrue(content.contains("SUBSTITUTE_IDENTITY_DOCUMENT") || content.contains("TEMPORARY_IDENTITY_CARD"))
    }

    @Test
    fun testReadNonExistentResource() {
        assertFailsWith<FileNotFoundException> {
            readTextResource("NonExistentFile.json")
        }
    }

    @Test
    fun testReadEmptyFileName() {
        assertFailsWith<FileNotFoundException> {
            readTextResource("")
        }
    }
}
