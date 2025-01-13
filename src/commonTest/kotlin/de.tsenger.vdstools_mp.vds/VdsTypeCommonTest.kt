package de.tsenger.vdstools_mp.vds

import de.tsenger.vdstools_mp.DataEncoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


class VdsTypeCommonTest {
    @Test
    fun testGetValue() {
        assertEquals(0xf908, DataEncoder.getDocumentRef("ADDRESS_STICKER_ID"))
    }

    @Test
    fun testValueOf() {
        assertEquals("ADDRESS_STICKER_ID", DataEncoder.getVdsType(0xf908))
    }

    @Test
    fun testValueOf_unknown() {
        assertNull(DataEncoder.getVdsType(0x6666))
    }
}
