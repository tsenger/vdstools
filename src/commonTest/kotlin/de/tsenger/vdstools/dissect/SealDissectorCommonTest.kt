package de.tsenger.vdstools.dissect

import de.tsenger.vdstools.generic.Seal
import de.tsenger.vdstools.idb.IcbRawStringsCommon
import de.tsenger.vdstools.idb.IdbSeal
import de.tsenger.vdstools.vds.VdsRawBytesCommon
import de.tsenger.vdstools.vds.VdsSeal
import kotlin.test.*

class SealDissectorCommonTest {

    // --- VDS v3 (residentPermit) ---

    @Test
    fun vds_v3_headerStartsAtZero() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.residentPermit) as VdsSeal
        assertEquals(0, seal.dissect().header.range.offset)
    }

    @Test
    fun vds_v3_messageGroupStartsWhereHeaderEnds() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.residentPermit) as VdsSeal
        val d = seal.dissect()
        assertEquals(
            d.header.range.offset + d.header.range.length,
            d.messageGroup.range.offset
        )
    }

    @Test
    fun vds_v3_signatureStartsWhereMessageGroupEnds() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.residentPermit) as VdsSeal
        val d = seal.dissect()
        val sig = assertNotNull(d.signature)
        assertEquals(
            d.messageGroup.range.offset + d.messageGroup.range.length,
            sig.range.offset
        )
    }

    @Test
    fun vds_v3_totalRangeCoverAllBytes() {
        val raw = VdsRawBytesCommon.residentPermit
        val seal = VdsSeal.fromByteArray(raw) as VdsSeal
        val d = seal.dissect()
        val sig = assertNotNull(d.signature)
        assertEquals(raw.size, sig.range.offset + sig.range.length)
    }

    @Test
    fun vds_v3_magicByteAtHeaderOffset0() {
        val raw = VdsRawBytesCommon.residentPermit
        val seal = VdsSeal.fromByteArray(raw) as VdsSeal
        val magicRange = seal.dissect().header.children.first().range
        assertEquals(0xDC.toByte(), raw[magicRange.offset])
    }

    @Test
    fun vds_v3_signatureTagByteIs0xFF() {
        val raw = VdsRawBytesCommon.residentPermit
        val seal = VdsSeal.fromByteArray(raw) as VdsSeal
        val sigRange = assertNotNull(seal.dissect().signature).range
        assertEquals(0xFF.toByte(), raw[sigRange.offset])
    }

    @Test
    fun vds_v3_messageFieldRangesWithinBounds() {
        val raw = VdsRawBytesCommon.residentPermit
        val seal = VdsSeal.fromByteArray(raw) as VdsSeal
        for (field in seal.dissect().messageGroup.children) {
            assertTrue(field.range.offset >= 0, "negative offset: ${field.label}")
            assertTrue(field.range.offset + field.range.length <= raw.size, "out of bounds: ${field.label}")
        }
    }

    @Test
    fun vds_v3_messageFieldNamesMatch() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.residentPermit) as VdsSeal
        val fieldNames = seal.dissect().messageGroup.children.map { it.label }
        assertTrue(fieldNames.any { it.contains("MRZ") }, "Expected MRZ field, got: $fieldNames")
    }

    @Test
    fun vds_v3_noSignerCertificate() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.residentPermit) as VdsSeal
        assertNull(seal.dissect().signerCertificate)
    }

    // --- VDS v2 (socialInsurance) ---

    @Test
    fun vds_v2_headerAndMessageGroupAreContiguous() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.socialInsurance) as VdsSeal
        val d = seal.dissect()
        assertEquals(
            d.header.range.offset + d.header.range.length,
            d.messageGroup.range.offset
        )
    }

    @Test
    fun vds_v2_headerHas8SubFields() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.socialInsurance) as VdsSeal
        // Magic, Version, Country, SignerCertRef, IssuingDate, SigDate, DocFeatureRef, DocTypeCat
        assertEquals(8, seal.dissect().header.children.size)
    }

    @Test
    fun vds_v2_messageFieldValueRangesDoNotOverlap() {
        val seal = VdsSeal.fromByteArray(VdsRawBytesCommon.socialInsurance) as VdsSeal
        val children = seal.dissect().messageGroup.children
        for (i in 0 until children.size - 1) {
            val current = children[i].range
            val next = children[i + 1].range
            assertTrue(
                current.offset + current.length <= next.offset,
                "Overlap: ${children[i].label} and ${children[i + 1].label}"
            )
        }
    }

    // --- IDB (signed) ---

    @Test
    fun idb_headerStartsAtZero() {
        val seal = IdbSeal.fromString(IcbRawStringsCommon.SubstituteIdentityDocument) as IdbSeal
        assertEquals(0, seal.dissect().header.range.offset)
    }

    @Test
    fun idb_messageGroupStartsWhereHeaderEnds() {
        val seal = IdbSeal.fromString(IcbRawStringsCommon.SubstituteIdentityDocument) as IdbSeal
        val d = seal.dissect()
        assertEquals(
            d.header.range.offset + d.header.range.length,
            d.messageGroup.range.offset
        )
    }

    @Test
    fun idb_messageGroupTagByteIs0x61() {
        val seal = IdbSeal.fromString(IcbRawStringsCommon.SubstituteIdentityDocument) as IdbSeal
        val d = seal.dissect()
        val payloadBytes = seal.payLoad.idbHeader.encoded + seal.payLoad.idbMessageGroup.encoded
        assertEquals(0x61.toByte(), payloadBytes[d.messageGroup.range.offset])
    }

    @Test
    fun idb_signedHeader_has4SubFields() {
        val seal = IdbSeal.fromString(IcbRawStringsCommon.SubstituteIdentityDocument) as IdbSeal
        // Country, SignatureAlgorithm, CertificateReference, SignatureCreationDate
        assertEquals(4, seal.dissect().header.children.size)
    }

    @Test
    fun idb_messageFieldRangesWithinBounds() {
        val seal = IdbSeal.fromString(IcbRawStringsCommon.SubstituteIdentityDocument) as IdbSeal
        val d = seal.dissect()
        val totalLen = seal.payLoad.idbHeader.encoded.size +
                seal.payLoad.idbMessageGroup.encoded.size +
                (seal.payLoad.idbSignature?.encoded?.size ?: 0)
        for (field in d.messageGroup.children) {
            assertTrue(field.range.offset >= 0, "negative offset: ${field.label}")
            assertTrue(field.range.offset + field.range.length <= totalLen, "out of bounds: ${field.label}")
        }
    }

    // --- Generic Seal.dissect() dispatcher ---

    @Test
    fun seal_dissect_vdsViaBaseType() {
        val seal: Seal = VdsSeal.fromByteArray(VdsRawBytesCommon.residentPermit)
        val d = seal.dissect()
        assertEquals(0, d.header.range.offset)
        assertNull(d.signerCertificate)
    }

    @Test
    fun seal_dissect_idbViaBaseType() {
        val seal: Seal = IdbSeal.fromString(IcbRawStringsCommon.SubstituteIdentityDocument)
        val d = seal.dissect()
        assertEquals(0, d.header.range.offset)
        assertNotNull(d.signature)
    }

    // --- IDB signed (CertifyingPermanentResidence) ---

    @Test
    fun idb_signed_headerHas4SubFields() {
        val seal = IdbSeal.fromString(IcbRawStringsCommon.CertifyingPermanentResidence) as IdbSeal
        // Country, SignatureAlgorithm, CertificateReference, SignatureCreationDate
        assertEquals(4, seal.dissect().header.children.size)
    }

    @Test
    fun idb_signed_hasSignature() {
        val seal = IdbSeal.fromString(IcbRawStringsCommon.CertifyingPermanentResidence) as IdbSeal
        assertNotNull(seal.dissect().signature)
    }
}
