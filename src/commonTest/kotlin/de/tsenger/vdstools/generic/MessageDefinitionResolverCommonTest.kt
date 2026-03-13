package de.tsenger.vdstools.generic

import de.tsenger.vdstools.DataEncoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MessageDefinitionResolverCommonTest {

    @Test
    fun testIdbResolveByTag() {
        val resolver = DataEncoder.idbMessageTypes.asResolver()
        val def = resolver.resolveByTag("08")
        assertNotNull(def)
        assertEquals("MRZ_TD3", def.name)
        assertEquals(MessageCoding.MRZ, def.coding)
        assertEquals("08", def.tag)
    }

    @Test
    fun testIdbResolveByName() {
        val resolver = DataEncoder.idbMessageTypes.asResolver()
        val def = resolver.resolveByName("MRZ_TD3")
        assertNotNull(def)
        assertEquals("08", def.tag)
        assertEquals(MessageCoding.MRZ, def.coding)
    }

    @Test
    fun testIdbResolveUnknownTag() {
        val resolver = DataEncoder.idbMessageTypes.asResolver()
        assertNull(resolver.resolveByTag("FF"))
    }

    @Test
    fun testIdbRoundTrip() {
        val resolver = DataEncoder.idbMessageTypes.asResolver()
        val byName = resolver.resolveByName("FACE_IMAGE")
        assertNotNull(byName)
        val byTag = resolver.resolveByTag(byName.tag)
        assertNotNull(byTag)
        assertEquals(byName.name, byTag.name)
        assertEquals(byName.coding, byTag.coding)
    }

    @Test
    fun testVdsResolveByTag() {
        val resolver = DataEncoder.vdsDocumentTypes.asResolver("ICAO_EMERGENCY_TRAVEL_DOCUMENT")
        val def = resolver.resolveByTag("02")
        assertNotNull(def)
        assertEquals("MRZ", def.name)
        assertEquals(MessageCoding.MRZ, def.coding)
    }

    @Test
    fun testVdsResolveByName() {
        val resolver = DataEncoder.vdsDocumentTypes.asResolver("ICAO_EMERGENCY_TRAVEL_DOCUMENT")
        val def = resolver.resolveByName("MRZ")
        assertNotNull(def)
        assertEquals("02", def.tag)
    }

    @Test
    fun testVdsRoundTrip() {
        val resolver = DataEncoder.vdsDocumentTypes.asResolver("ARRIVAL_ATTESTATION")
        val byName = resolver.resolveByName("AZR")
        assertNotNull(byName)
        val byTag = resolver.resolveByTag(byName.tag)
        assertNotNull(byTag)
        assertEquals("AZR", byTag.name)
    }
}
