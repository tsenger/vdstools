package de.tsenger.vdstools.idb

import de.tsenger.vdstools.IdbMessageTypeParser
import de.tsenger.vdstools.readTextResource
import de.tsenger.vdstools.vds.MessageCoding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class IdbMessageTypeParserCommonTest {

    @Test
    fun testParseJson() {
        val jsonString = readTextResource("IdbMessageTypes.json")
        val parser = IdbMessageTypeParser(jsonString)
        assertNotNull(parser)
        println(parser.availableMessageTypes())
        assertEquals("FACE_IMAGE", parser.getMessageType(128))
        assertEquals(1, parser.getMessageType("VISA"))
        assertEquals(MessageCoding.BYTES, parser.getMessageTypeCoding("FACE_IMAGE"))
    }
}