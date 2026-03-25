package de.tsenger.vdstools.generic

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.vds.VdsRawBytesCommon
import de.tsenger.vdstools.vds.VdsSeal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MessageResolverCommonTest {

    @Test
    fun testResolveVdsMessage() {
        // MRZ tag=0x02 for EMERGENCY_TRAVEL_DOCUMENT, encoded as C40
        val mrzBytes = DataEncoder.encodeC40("I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06")
        val derTlv = DerTlv(0x02.toByte(), mrzBytes)
        val resolver = DataEncoder.vdsDocumentTypes.asResolver("EMERGENCY_TRAVEL_DOCUMENT")
        val message = MessageResolver.resolve(derTlv, resolver)
        assertNotNull(message)
        assertEquals("02", message.tag)
        assertEquals("MRZ", message.name)
        assertEquals(MessageCoding.MRZ, message.coding)
    }

    @Test
    fun testResolveIdbMessage() {
        val value = byteArrayOf(0x0D) // BYTE value
        val derTlv = DerTlv(0x86.toByte(), value)
        val resolver = DataEncoder.idbMessageTypes.asResolver()
        val message = MessageResolver.resolve(derTlv, resolver)
        assertNotNull(message)
        assertEquals("86", message.tag)
        assertEquals("NATIONAL_DOCUMENT_IDENTIFIER", message.name)
        assertEquals(MessageCoding.BYTE, message.coding)
    }

    @Test
    fun testVdsSealMessageListUnchanged() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.emergenyTravelDoc) as VdsSeal
        assertEquals(1, seal.messageList.size)
        assertEquals("MRZ", seal.messageList[0].name)
        assertTrue(seal.messageList[0].value is MessageValue.MrzValue)
    }

    @Suppress("DEPRECATION")
    @Test
    fun testDeprecatedEncodeDerTlvStillWorks() {
        val mrzBytes = DataEncoder.encodeC40("I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06")
        val derTlv = DerTlv(0x02.toByte(), mrzBytes)
        val message = DataEncoder.vdsDocumentTypes.encodeDerTlv("EMERGENCY_TRAVEL_DOCUMENT", derTlv)
        assertNotNull(message)
        assertEquals("MRZ", message.name)
    }
}
