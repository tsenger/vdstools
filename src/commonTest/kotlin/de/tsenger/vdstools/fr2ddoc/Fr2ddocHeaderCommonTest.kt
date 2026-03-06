package de.tsenger.vdstools.fr2ddoc

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Fr2ddocHeaderCommonTest {

    private fun parseHeader(raw: String): Fr2ddocHeader =
        Fr2ddocHeader.fromStringBuffer(Fr2ddocSeal.BufferReader(raw))

    // --- getDateFromDaysSince2000 ---

    @Test
    fun testGetDateFromDaysSince2000_zero() {
        assertEquals(LocalDate(2000, 1, 1), Fr2ddocHeader.getDateFromDaysSince2000(0))
    }

    @Test
    fun testGetDateFromDaysSince2000_ffff_returnsNull() {
        assertNull(Fr2ddocHeader.getDateFromDaysSince2000(0xffffL))
    }

    // --- version 4 headers ---

    @Test
    fun testVersion4_FR05_header() {
        // DC04FR0500011FD71FD70701FR
        val h = parseHeader("DC04FR0500011FD71FD70701FR")
        assertEquals(4, h.version.toInt())
        assertEquals("FR05", h.signerIdentifier)
        assertEquals("0001", h.certificateReference)
        assertEquals(LocalDate(2022, 4, 26), h.issuingDate)   // 0x1FD7 = 8151 days
        assertEquals(LocalDate(2022, 4, 26), h.sigDate)
        assertEquals("07", h.docType)
        assertEquals("01", h.perimeterId)
        assertEquals("FR", h.issuingCountry)
    }

    @Test
    fun testVersion4_docTypeAA() {
        // DC04FR000001125E125BAA01FR
        val h = parseHeader("DC04FR000001125E125BAA01FR")
        assertEquals(4, h.version.toInt())
        assertEquals("FR00", h.signerIdentifier)
        assertEquals("0001", h.certificateReference)
        assertEquals(LocalDate(2012, 11, 15), h.issuingDate)  // 0x125E = 4702 days
        assertEquals(LocalDate(2012, 11, 12), h.sigDate)      // 0x125B = 4699 days
        assertEquals("AA", h.docType)
        assertEquals("01", h.perimeterId)
        assertEquals("FR", h.issuingCountry)
    }

    @Test
    fun testVersion4_docTypeC7() {
        // DC04FR0000011F4B1F54C701FR
        val h = parseHeader("DC04FR0000011F4B1F54C701FR")
        assertEquals(4, h.version.toInt())
        assertEquals("FR00", h.signerIdentifier)
        assertEquals("0001", h.certificateReference)
        assertEquals(LocalDate(2021, 12, 7), h.issuingDate)   // 0x1F4B = 8011 days
        assertEquals(LocalDate(2021, 12, 17), h.sigDate)      // 0x1F54 = 8020 days
        assertEquals("C7", h.docType)
        assertEquals("01", h.perimeterId)
        assertEquals("FR", h.issuingCountry)
    }

    @Test
    fun testVersion4_issuingDateNull() {
        // DC04FR000001FFFF226C2401FR
        val h = parseHeader("DC04FR000001FFFF226C2401FR")
        assertEquals(4, h.version.toInt())
        assertEquals("FR00", h.signerIdentifier)
        assertEquals("0001", h.certificateReference)
        assertNull(h.issuingDate)                              // 0xFFFF → null
        assertEquals(LocalDate(2024, 2, 16), h.sigDate)       // 0x226C = 8812 days
        assertEquals("24", h.docType)
        assertEquals("01", h.perimeterId)
        assertEquals("FR", h.issuingCountry)
    }

    // --- version 3 headers (has perimeterId, no issuingCountry) ---

    @Test
    fun testVersion3_docType00() {
        // DC03FR000001123F16360001
        val h = parseHeader("DC03FR000001123F16360001")
        assertEquals(3, h.version.toInt())
        assertEquals("FR00", h.signerIdentifier)
        assertEquals("0001", h.certificateReference)
        assertEquals(LocalDate(2012, 10, 15), h.issuingDate)  // 0x123F = 4671 days
        assertEquals(LocalDate(2015, 7, 27), h.sigDate)       // 0x1636 = 5686 days
        assertEquals("00", h.docType)
        assertEquals("01", h.perimeterId)
        assertNull(h.issuingCountry)
    }

    @Test
    fun testVersion3_issuingDateNull() {
        // DC03FR000001FFFF16360501
        val h = parseHeader("DC03FR000001FFFF16360501")
        assertEquals(3, h.version.toInt())
        assertEquals("FR00", h.signerIdentifier)
        assertEquals("0001", h.certificateReference)
        assertNull(h.issuingDate)
        assertEquals(LocalDate(2015, 7, 27), h.sigDate)       // 0x1636 = 5686 days
        assertEquals("05", h.docType)
        assertEquals("01", h.perimeterId)
        assertNull(h.issuingCountry)
    }

    // --- version 2 headers (no perimeterId, no issuingCountry) ---

    @Test
    fun testVersion2_docType01() {
        // DC02FR000001125E125B01
        val h = parseHeader("DC02FR000001125E125B01")
        assertEquals(2, h.version.toInt())
        assertEquals("FR00", h.signerIdentifier)
        assertEquals("0001", h.certificateReference)
        assertEquals(LocalDate(2012, 11, 15), h.issuingDate)  // 0x125E = 4702 days
        assertEquals(LocalDate(2012, 11, 12), h.sigDate)      // 0x125B = 4699 days
        assertEquals("01", h.docType)
        assertNull(h.perimeterId)
        assertNull(h.issuingCountry)
    }

    @Test
    fun testVersion2_docType07() {
        // DC02FR000001092D149D07
        val h = parseHeader("DC02FR000001092D149D07")
        assertEquals(2, h.version.toInt())
        assertEquals("FR00", h.signerIdentifier)
        assertEquals("0001", h.certificateReference)
        assertEquals(LocalDate(2006, 6, 7), h.issuingDate)    // 0x092D = 2349 days
        assertEquals(LocalDate(2014, 6, 13), h.sigDate)       // 0x149D = 5277 days
        assertEquals("07", h.docType)
        assertNull(h.perimeterId)
        assertNull(h.issuingCountry)
    }
}