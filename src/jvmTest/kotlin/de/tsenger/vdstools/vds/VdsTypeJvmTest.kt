package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VdsTypeJvmTest {
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
