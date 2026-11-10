package de.tsenger.vdstools.idb

import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.FormatFinding
import de.tsenger.vdstools.generic.FormatFinding.Category
import de.tsenger.vdstools.generic.SealParser
import kotlin.test.*

/** Format findings of IDB seals (ICAO Doc 9303-13 App. D, format validation). */
@OptIn(ExperimentalStdlibApi::class)
class IdbFormatFindingsCommonTest {

    private fun tlv(tag: Int, hex: String) = DerTlv(tag.toByte(), hex.hexToByteArray())

    private fun seal(vararg tlvs: DerTlv) =
        IdbSeal(isSigned = false, isZipped = false, IdbPayload(IdbHeader("D<<"), IdbMessageGroup(tlvs.toList()), null, null))

    @Test
    fun fixtures_haveNoFindings() {
        for (raw in listOf(
            IcbRawStringsCommon.SubstituteIdentityDocument,
            IcbRawStringsCommon.EmergencyTravelDocument,
            IcbRawStringsCommon.TemporaryPassport,
            IcbRawStringsCommon.ArrivalAttestation,
            IcbRawStringsCommon.ProvisionalResidenceDocument,
            IcbRawStringsCommon.CertifyingPermanentResidence,
            IcbRawStringsCommon.FrontierWorkerPermit,
            IcbRawStringsCommon.SupplementarySheetResidencePermit,
            IcbRawStringsCommon.Visa,
        )) {
            assertEquals(emptyList(), SealParser().parse(raw).formatFindings, raw)
        }
    }

    @Test
    fun unknownTopLevelMessage_isUnknownTag() {
        val finding = seal(tlv(0x50, "abcd")).formatFindings.single()
        assertEquals(FormatFinding.UnknownTag(listOf(0x50)), finding)
        assertEquals(Category.UNKNOWN_FEATURE, finding.category)
    }

    @Test
    fun unknownSubMessage_isUnknownTagWithPath() {
        val visa = tlv(0x01, tlv(0x06, "aa").encoded.toHexString() + tlv(0x08, "abcd").encoded.toHexString())
        assertEquals(listOf(FormatFinding.UnknownTag(listOf(0x01, 0x08))), seal(visa).formatFindings)
    }

    @Test
    fun containerStartingWithUnknownSubTag_isStillParsed() {
        // the unknown sub-tag must not hide the known one behind it
        val visa = tlv(0x01, tlv(0x08, "abcd").encoded.toHexString() + tlv(0x06, "aa").encoded.toHexString())
        val parsed = seal(visa).messageList.single()

        assertEquals(listOf("0x08", "VISA_TYPE"), parsed.messageList.map { it.name })
        assertEquals(listOf(FormatFinding.UnknownTag(listOf(0x01, 0x08))), seal(visa).formatFindings)
    }

    @Test
    fun undecodableSubMessage_isDecodeFailedAndRaw() {
        // VISA DURATION_OF_STAY (sub-tag 0x04) needs exactly three bytes
        val visa = tlv(0x01, tlv(0x04, "0102").encoded.toHexString())
        val seal = seal(visa)

        val duration = seal.messageList.single().messageList.single()
        assertEquals("DURATION_OF_STAY", duration.name)
        assertTrue(duration.isRaw)
        assertEquals(listOf(FormatFinding.DecodeFailed(listOf(0x01, 0x04), "DURATION_OF_STAY")), seal.formatFindings)
    }

    @Test
    fun bytesCodedMessagesAndContainers_areNotRaw() {
        // EF_CARD_ACCESS is defined as BYTES; VISA is a container whose interpretation is its sub-messages
        val seal = seal(tlv(0x0A, "aabb"), tlv(0x01, tlv(0x06, "aa").encoded.toHexString()))

        assertEquals(listOf(false, false), seal.messageList.map { it.isRaw })
        assertEquals(emptyList(), seal.formatFindings)
    }

    @Test
    fun corruptedContainer_isMalformedSubMessages() {
        // EMERGENCY_TRAVEL_DOCUMENT whose content is no valid DER-TLV sequence
        val finding = seal(tlv(0x02, "02ff0001")).formatFindings.single()
        assertEquals(FormatFinding.MalformedSubMessages(listOf(0x02), "EMERGENCY_TRAVEL_DOCUMENT"), finding)
        assertEquals(Category.WRONG_FORMAT, finding.category)
    }

    @Test
    fun unknownNationalDocumentType_isUndetermined() {
        val finding = seal(tlv(0x86, "7f")).formatFindings.single()
        assertEquals(FormatFinding.UnknownDocumentType(0x7F), finding)
        assertEquals(Category.UNDETERMINED, finding.category)
    }
}
