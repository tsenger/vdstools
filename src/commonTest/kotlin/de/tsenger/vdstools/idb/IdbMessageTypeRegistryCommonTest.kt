package de.tsenger.vdstools.idb

import de.tsenger.vdstools.generated.ResourceConstants
import de.tsenger.vdstools.IdbMessageTypeRegistry
import de.tsenger.vdstools.readTextResource
import de.tsenger.vdstools.generic.MessageCoding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class IdbMessageTypeRegistryCommonTest {

    private fun bundledRegistry() =
        IdbMessageTypeRegistry("[]").apply { ResourceConstants.IDB_MESSAGE_TYPES_JSON.forEach(::addEntriesFromJson) }

    @Test
    fun testParseJson() {
        val registry = bundledRegistry()
        assertNotNull(registry)
        println(registry.availableMessageTypes())
        assertEquals("FACE_IMAGE", registry.getMessageType(128))
        assertEquals(1, registry.getMessageType("VISA"))
        assertEquals(MessageCoding.BYTES, registry.getMessageTypeCoding("FACE_IMAGE"))
    }

    @Test
    fun testNameLookupIgnoresCase() {
        val registry = bundledRegistry()
        assertEquals(1, registry.getMessageType("visa"))
        assertEquals(MessageCoding.BYTES, registry.getMessageTypeCoding("face_image"))
        assertEquals("FACE_IMAGE", registry.getMessageTypeDto("Face_Image")?.name)
    }
}
