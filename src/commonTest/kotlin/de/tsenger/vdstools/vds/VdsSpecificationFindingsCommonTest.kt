package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.FormatFinding
import de.tsenger.vdstools.generic.FormatFinding.Category
import kotlin.test.*

/**
 * Format findings of VDS document types whose definitions are verified against their specification
 * (BSI TR-03137-1 V3.0, ICAO Doc 9303-7 and 9303-8): required messages, lengths, additional features.
 */
@OptIn(ExperimentalStdlibApi::class)
class VdsSpecificationFindingsCommonTest {

    @BeforeTest
    fun setUp() {
        DataEncoder.resetToDefaults()
    }

    private fun tlv(tag: Int, hex: String) = DerTlv(tag.toByte(), hex.hexToByteArray())
    private fun utf8(tag: Int, text: String) = DerTlv(tag.toByte(), text.encodeToByteArray())

    private fun group(vdsType: String, vararg tlvs: DerTlv) = VdsMessageGroup(vdsType, tlvs.toList())

    /** Social Insurance Card (TR-03137 §7.2): 0x01 number (8 bytes C40), 0x02/0x03 names (1–90 bytes UTF-8). */
    private val insuranceNumber = DerTlv(0x01, DataEncoder.encodeC40("65170839J003"))
    private val surname = utf8(0x02, "Mustermann")
    private val firstName = utf8(0x03, "Erika")

    @Test
    fun socialInsuranceCard_complete_hasNoFindings() {
        assertEquals(emptyList(), group("SOCIAL_INSURANCE_CARD", insuranceNumber, surname, firstName).formatFindings())
    }

    @Test
    fun socialInsuranceCard_missingRequiredMessage() {
        val findings = group("SOCIAL_INSURANCE_CARD", insuranceNumber, surname).formatFindings()
        assertEquals(listOf(FormatFinding.MissingRequired(listOf(0x03), "FIRST_NAME")), findings)
    }

    @Test
    fun socialInsuranceCard_tooLongName_isInvalidLength() {
        val findings = group("SOCIAL_INSURANCE_CARD", insuranceNumber, utf8(0x02, "x".repeat(91)), firstName)
            .formatFindings()
        val finding = findings.single()
        assertEquals(FormatFinding.InvalidLength(listOf(0x02), "SURNAME", 91, 1, 90), finding)
        assertEquals(Category.WRONG_FORMAT, finding.category)
    }

    @Test
    fun socialInsuranceCard_additionalFeature_isNotAllowed() {
        // TR-03137 §7.2.2: other additional document features are not permitted
        val findings = group("SOCIAL_INSURANCE_CARD", insuranceNumber, surname, firstName, tlv(0x05, "aa"))
            .formatFindings()
        assertEquals(listOf(FormatFinding.NotAllowed(listOf(0x05), "0x05")), findings)
    }

    @Test
    fun arrivalAttestation_additionalFeature_isUnknownTag() {
        // TR-03137 §6.2 does not forbid additional features
        val mrz = DerTlv(0x02, DataEncoder.encodeC40("ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06"))
        val azr = DerTlv(0x03, DataEncoder.encodeC40("ABC123456DEF"))
        val findings = group("ARRIVAL_ATTESTATION", mrz, azr, tlv(0x05, "aa")).formatFindings()
        assertEquals(listOf(FormatFinding.UnknownTag(listOf(0x05))), findings)
    }

    @Test
    fun visa_withMrvbOnlyAndWithoutNumberOfEntries_hasNoFindings() {
        // ICAO 9303-7 §9.1.3: MRV-A or MRV-B; NUMBER_OF_ENTRIES is optional
        val group = VdsMessageGroup.Builder("VISA")
            .addMessage("MRZ_MRVB", "V<D<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<T000007366D<<6408120F3012310")
            .addMessage("DURATION_OF_STAY", "5a0000".hexToByteArray())
            .addMessage("PASSPORT_NUMBER", "ALAAF5099")
            .build()
        assertEquals(emptyList(), group.formatFindings())
    }

    @Test
    fun visa_tr03137WorkedExample_hasNoFindings() {
        // BSI TR-03137-1 V3.0 Annex F: German visa with MRV-B, duration of stay and passport number
        val raw = ("dc036abc6d32c8a72cb10f7134b798155d01022cdd52134a74da1347c6fed95cb89f9fce133c133c133c133c" +
                "203833734aaf47f0c32f1a1e20eb2625393afe310403a00000050633be1fed20c6ff38d5573247608c9b64cae0" +
                "22c1c5249660e51ae316d600f974aac2129c3f093c35955314026304705cd24c1b8100711bd4411f068e8d93e8b7")
            .hexToByteArray()
        val seal = VdsSeal.fromByteArray(raw) as VdsSeal
        assertEquals("VISA", seal.documentType)
        assertEquals(listOf("MRZ_MRVB", "DURATION_OF_STAY", "PASSPORT_NUMBER"), seal.messageList.map { it.name })
        assertEquals(emptyList(), seal.formatFindings)
    }

    @Test
    fun visa_withoutMrz_isMissingRequired() {
        val group = VdsMessageGroup.Builder("VISA")
            .addMessage("DURATION_OF_STAY", "5a0000".hexToByteArray())
            .addMessage("PASSPORT_NUMBER", "ALAAF5099")
            .build()
        val finding = group.formatFindings().single()
        assertEquals(FormatFinding.MissingRequired(emptyList(), "MRZ_MRVA or MRZ_MRVB"), finding)
        assertEquals(Category.WRONG_FORMAT, finding.category)
    }
}
