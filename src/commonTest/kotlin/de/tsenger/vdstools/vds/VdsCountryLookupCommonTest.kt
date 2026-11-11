package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.generic.FormatFinding
import de.tsenger.vdstools.generic.FormatFinding.Category
import kotlin.test.*

/**
 * National document types (even document type category, ICAO 9303-13 §3.1.1) belong to their issuing
 * country. Lookup: the seal's country first, then international (ICAO) types, then a unique national
 * type of another country, reported as [FormatFinding.ForeignNationalDefinition].
 */
class VdsCountryLookupCommonTest {

    private val dummySigner = object : Signer {
        override val fieldSize = 256
        override fun sign(data: ByteArray) = ByteArray(64)
    }

    @BeforeTest
    fun setUp() {
        DataEncoder.resetToDefaults()
    }

    @AfterTest
    fun tearDown() {
        DataEncoder.resetToDefaults()
    }

    private fun parse(raw: ByteArray) = VdsSeal.fromByteArray(raw) as VdsSeal

    private fun foreign(seal: VdsSeal) = seal.formatFindings.filterIsInstance<FormatFinding.ForeignNationalDefinition>()

    @Test
    fun germanSeal_usesGermanTypeWithoutFinding() {
        // "D" and "D  " (C40 fillers) are the same country
        for (raw in listOf(VdsRawBytesCommon.ankunftsnwPapier, VdsRawBytesCommon.arrivalAttestation)) {
            val seal = parse(raw)
            assertEquals("ARRIVAL_ATTESTATION", seal.documentType)
            assertEquals(emptyList(), foreign(seal), seal.issuingCountry)
        }
    }

    @Test
    fun specimenSeal_usesGermanTypeWithFinding() {
        // BSI specimens carry UTO (ICAO 9303-3: Utopia for sample documents)
        val seal = parse(VdsRawBytesCommon.socialInsurance)
        assertEquals("UTO", seal.issuingCountry)
        assertEquals("SOCIAL_INSURANCE_CARD", seal.documentType)
        val finding = foreign(seal).single()
        assertEquals(FormatFinding.ForeignNationalDefinition(emptyList(), "D"), finding)
        assertEquals(Category.UNDETERMINED, finding.category)
    }

    @Test
    fun icaoType_hasNoCountryAndNoFinding() {
        val seal = parse(VdsRawBytesCommon.visa_224bitSig)
        assertEquals("VISA", seal.documentType)
        assertEquals(emptyList(), foreign(seal))
    }

    private fun addFrenchArrivalAttestation() = DataEncoder.addCustomVdsDocumentTypes(
        """[{"documentType": "FR_ARRIVAL_ATTESTATION", "documentRef": "fd02", "country": "F", "version": 4,
            "messages": [{"name": "FR_FIELD", "tag": 2, "coding": "BYTES", "required": false, "minBytes": 1, "maxBytes": 99}]}]"""
    )

    @Test
    fun ownCountryIsPreferred() {
        addFrenchArrivalAttestation()

        assertEquals("ARRIVAL_ATTESTATION", parse(VdsRawBytesCommon.ankunftsnwPapier).documentType)
        val french = VdsSeal.Builder("FR_ARRIVAL_ATTESTATION").issuingCountry("F<<")
            .addMessage("FR_FIELD", "aa".hexToByteArray()).build(dummySigner)
        val parsed = parse(french.encoded)
        assertEquals("FR_ARRIVAL_ATTESTATION", parsed.documentType)
        assertEquals(emptyList(), foreign(parsed))
    }

    @Test
    fun ambiguousForeignTypes_areUnknown() {
        // a specimen (UTO) whose reference two other countries define: no guessing
        addFrenchArrivalAttestation()
        val seal = parse(VdsRawBytesCommon.arrivalAttestationV02)
        assertEquals("UTO", seal.issuingCountry)
        assertEquals(VdsHeader.UNKNOWN_TYPE, seal.documentType)
        assertTrue(seal.formatFindings.any { it is FormatFinding.UnknownDocumentType })
    }

    @Test
    fun builderKeepsTheIssuingCountryOfTheCaller() {
        val seal = VdsSeal.Builder("SOCIAL_INSURANCE_CARD").issuingCountry("F<<")
            .addMessage("SOCIAL_INSURANCE_NUMBER", "65170839J003")
            .addMessage("SURNAME", "Mustermann")
            .addMessage("FIRST_NAME", "Erika")
            .build(dummySigner)
        val parsed = parse(seal.encoded)
        assertEquals("SOCIAL_INSURANCE_CARD", parsed.documentType)
        assertEquals(listOf(FormatFinding.ForeignNationalDefinition(emptyList(), "D")), foreign(parsed))
    }

    @Test
    fun customEntryWithoutCountry_doesNotReplaceTheGermanOne() {
        DataEncoder.addCustomVdsDocumentTypes(
            """[{"documentType": "MY_ARRIVAL_ATTESTATION", "documentRef": "fd02", "version": 4, "messages": []}]"""
        )
        assertEquals("ARRIVAL_ATTESTATION", parse(VdsRawBytesCommon.ankunftsnwPapier).documentType)
        assertEquals("MY_ARRIVAL_ATTESTATION", parse(VdsRawBytesCommon.arrivalAttestationV02).documentType)
    }

    @Test
    fun customEntryWithTheSameName_replacesTheType() {
        // the name identifies the type: an override without country makes it international
        DataEncoder.addCustomVdsDocumentTypes(
            """[{"documentType": "ARRIVAL_ATTESTATION", "documentRef": "fd02", "version": 4, "messages": []}]"""
        )
        assertEquals(1, DataEncoder.vdsDocumentTypes.allDocumentTypes.count { it.documentType == "ARRIVAL_ATTESTATION" })
        val seal = parse(VdsRawBytesCommon.arrivalAttestationV02)
        assertEquals("ARRIVAL_ATTESTATION", seal.documentType)
        assertEquals(emptyList(), foreign(seal))
    }
}
