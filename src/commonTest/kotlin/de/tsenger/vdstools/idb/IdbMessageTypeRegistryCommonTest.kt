package de.tsenger.vdstools.idb

import de.tsenger.vdstools.IdbMessageTypeRegistry
import de.tsenger.vdstools.readTextResource
import de.tsenger.vdstools.generic.MessageCoding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class IdbMessageTypeRegistryCommonTest {

    @Test
    fun testParseJson() {
        val jsonString = readTextResource("IdbMessageTypes.json")
        val registry = IdbMessageTypeRegistry(jsonString)
        assertNotNull(registry)
        println(registry.availableMessageTypes())
        assertEquals("FACE_IMAGE", registry.getMessageType(128))
        assertEquals(1, registry.getMessageType("VISA"))
        assertEquals(MessageCoding.BYTES, registry.getMessageTypeCoding("FACE_IMAGE"))
    }
}