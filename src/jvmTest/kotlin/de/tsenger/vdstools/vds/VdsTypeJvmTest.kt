package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VdsTypeJvmTest {
    @Test
    fun testGetValue() {
        assertEquals(0xf908, DataEncoder.sealCodings.getDocumentRef("ADDRESS_STICKER_ID"))
    }

    @Test
    fun testValueOf() {
        assertEquals("ADDRESS_STICKER_ID", DataEncoder.sealCodings.getVdsType(0xf908))
    }

    @Test
    fun testValueOf_unknown() {
        assertNull(DataEncoder.sealCodings.getVdsType(0x6666))
    }
}
