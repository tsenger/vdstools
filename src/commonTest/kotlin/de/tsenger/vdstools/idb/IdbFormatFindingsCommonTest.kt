package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.FormatFinding
import de.tsenger.vdstools.generic.FormatFinding.Category
import de.tsenger.vdstools.generic.SealParser
import kotlin.test.*

/** Format findings of IDB seals (ICAO Doc 9303-13 App. D, format validation). */
@OptIn(ExperimentalStdlibApi::class)
class IdbFormatFindingsCommonTest {

    private fun tlv(tag: Int, hex: String) = DerTlv(tag.toByte(), hex.hexToByteArray())

    private fun seal(vararg tlvs: DerTlv) = seal("D<<", *tlvs)

    private fun seal(country: String, vararg tlvs: DerTlv) =
        IdbSeal(isSigned = false, isZipped = false, IdbPayload(IdbHeader(country), IdbMessageGroup(tlvs.toList()), null, null))

    private fun c40(tag: Int, text: String) = DerTlv(tag.toByte(), DataEncoder.encodeC40(text))

    /** Document Certifying Permanent Residence (0x10), TR-03137 Table 11: 0x82, 0x85, 0x86 required, nothing else. */
    private val permanentResidence = tlv(0x86, "10")
    private val documentReference = c40(0x82, "ABC123456")
    private val documentNumber = c40(0x85, "ZZ1234567")

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
    fun germanDocumentType_missingRequiredMessage() {
        val findings = seal(permanentResidence, documentReference).formatFindings
        assertEquals(listOf(FormatFinding.MissingRequired(listOf(0x85), "DOCUMENT_NUMBER")), findings)
    }

    @Test
    fun germanDocumentType_knownMessageNotBelongingToType_isNotAllowed() {
        val findings = seal(permanentResidence, documentReference, documentNumber, c40(0x09, "123456")).formatFindings
        assertEquals(listOf(FormatFinding.NotAllowed(listOf(0x09), "CAN")), findings)
        assertEquals(Category.WRONG_FORMAT, findings.single().category)
    }

    @Test
    fun germanDocumentType_complete_hasNoFindings() {
        assertEquals(emptyList(), seal(permanentResidence, documentReference, documentNumber).formatFindings)
    }

    @Test
    fun foreignSeal_documentTypeIsNotChecked() {
        // 0x86 is a German national message type; other countries may use it differently
        assertEquals(emptyList(), seal("UTO", permanentResidence).formatFindings)
        assertEquals(emptyList(), seal("UTO", tlv(0x86, "7f")).formatFindings)
    }

    @Test
    fun unknownNationalDocumentType_isUndetermined() {
        val finding = seal(tlv(0x86, "7f")).formatFindings.single()
        assertEquals(FormatFinding.UnknownDocumentType(0x7F), finding)
        assertEquals(Category.UNDETERMINED, finding.category)
    }
}
