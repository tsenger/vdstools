package de.tsenger.vdstools.idb

import de.tsenger.vdstools.IdbMessageTypeParser
import de.tsenger.vdstools.readTextResource
import de.tsenger.vdstools.vds.FeatureCoding
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
        assertEquals("FACE_IMAGE", parser.getMessageTypeName(128))
        assertEquals(1, parser.getMessageTypeTag("VISA"))
        assertEquals(FeatureCoding.BYTES, parser.getMessageTypeCoding("FACE_IMAGE"))
    }
}