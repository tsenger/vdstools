package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


class VdsTypeIosTest {
    @Test
    fun testGetValue() {
        assertEquals(0xf908, DataEncoder.vdsDocumentTypes.getDocumentRef("ADDRESS_STICKER_ID"))
    }

    @Test
    fun testValueOf() {
        assertEquals("ADDRESS_STICKER_ID", DataEncoder.vdsDocumentTypes.getVdsType(0xf908))
    }

    @Test
    fun testValueOf_unknown() {
        assertNull(DataEncoder.vdsDocumentTypes.getVdsType(0x6666))
    }
}
