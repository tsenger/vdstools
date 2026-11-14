package de.tsenger.vdstools

import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.generic.FormatFinding
import de.tsenger.vdstools.idb.IdbHeader
import de.tsenger.vdstools.idb.IdbMessageGroup
import de.tsenger.vdstools.idb.IdbPayload
import de.tsenger.vdstools.idb.IdbSeal
import de.tsenger.vdstools.vds.VdsMessageGroup
import de.tsenger.vdstools.vds.VdsRawBytesCommon
import de.tsenger.vdstools.vds.VdsSeal
import kotlin.test.*

/**
 * A seal binds its definitions (document type, profile, message types) when it is parsed or built.
 * Definitions loaded or replaced afterwards do not change it; the seal must be parsed again.
 */
@OptIn(ExperimentalStdlibApi::class)
class DefinitionBindingCommonTest {

    @BeforeTest
    fun setUp() {
        DataEncoder.resetToDefaults()
    }

    @AfterTest
    fun tearDown() {
        DataEncoder.resetToDefaults()
    }

    private val renamedSocialInsuranceCard = """
        [{
            "documentType": "RENAMED_SOCIAL_CARD",
            "documentRef": "fc04",
            "version": 4,
            "messages": [
                {"name": "RENAMED_NUMBER", "tag": 1, "coding": "C40", "required": true, "minBytes": 8, "maxBytes": 8},
                {"name": "RENAMED_SURNAME", "tag": 2, "coding": "UTF8_STRING", "required": true, "minBytes": 1, "maxBytes": 90},
                {"name": "RENAMED_FIRST_NAME", "tag": 3, "coding": "UTF8_STRING", "required": false, "minBytes": 1, "maxBytes": 90},
                {"name": "RENAMED_BIRTH_NAME", "tag": 4, "coding": "UTF8_STRING", "required": false, "minBytes": 1, "maxBytes": 90}
            ]
        }]
    """.trimIndent()

    private fun parseSocialInsuranceCard() = VdsSeal.fromByteArray(VdsRawBytesCommon.socialInsurance) as VdsSeal

    @Test
    fun vds_addedDefinitionsAfterParsing_doNotChangeTheSeal() {
        val seal = parseSocialInsuranceCard()
        val names = seal.messageList.map { it.name }

        DataEncoder.addCustomVdsDocumentTypes(renamedSocialInsuranceCard)

        assertEquals(names, seal.messageList.map { it.name })
        assertEquals("SOCIAL_INSURANCE_NUMBER", seal.getMessageByTag(1)?.name)
        assertEquals("RENAMED_NUMBER", parseSocialInsuranceCard().getMessageByTag(1)?.name, "a new parse sees the new definition")
    }

    @Test
    fun vds_replacedDefinitionsAfterParsing_doNotChangeTheSeal() {
        val seal = parseSocialInsuranceCard()
        val names = seal.messageList.map { it.name }

        DataEncoder.replaceCustomVdsDocumentTypes(renamedSocialInsuranceCard)

        assertEquals(names, seal.messageList.map { it.name })
        assertNotNull(seal.getMessageByName("SURNAME"))
    }

    @Test
    fun vds_formatFindingsUseTheDefinitionsBoundAtParsing() {
        // TR-03137 §7.2: FIRST_NAME is required; the replacement does not require it
        val group = VdsMessageGroup(
            "SOCIAL_INSURANCE_CARD",
            listOf(DerTlv(0x01, DataEncoder.encodeC40("65170839J003")), DerTlv(0x02, "Mustermann".encodeToByteArray()))
        )

        DataEncoder.addCustomVdsDocumentTypes(renamedSocialInsuranceCard.replace("RENAMED_SOCIAL_CARD", "SOCIAL_INSURANCE_CARD"))

        assertEquals(listOf(FormatFinding.MissingRequired(listOf(0x03), "FIRST_NAME")), group.formatFindings())
    }

    @Test
    fun vds_builtSeal_keepsItsDefinitions() {
        val group = VdsMessageGroup.Builder("SOCIAL_INSURANCE_CARD")
            .addMessage("SOCIAL_INSURANCE_NUMBER", "65170839J003")
            .addMessage("SURNAME", "Mustermann")
            .addMessage("FIRST_NAME", "Erika")
            .build()

        DataEncoder.addCustomVdsDocumentTypes(renamedSocialInsuranceCard.replace("RENAMED_SOCIAL_CARD", "SOCIAL_INSURANCE_CARD"))

        assertEquals(listOf("SOCIAL_INSURANCE_NUMBER", "SURNAME", "FIRST_NAME"), group.messageList.map { it.name })
    }

    @Test
    fun tr03171_profileLoadedAfterParsing_doesNotResolveTheSeal() {
        val uuid = "AABBCCDD11223344AABBCCDD11223344"
        val raw = ("dc036abc6d38dc0519a620372ce13372401c46ad535759e866926d2379bd19ce2678fe348d7ad88d7ad801c9" +
                DerTlv(0, uuid.hexToByteArray()).encoded.toHexString() +
                DerTlv(0x0A, "Mustermann".encodeToByteArray()).encoded.toHexString()).hexToByteArray()
        val seal = VdsSeal.fromByteArray(raw) as VdsSeal
        assertEquals(listOf("0x0A"), seal.messageList.map { it.name })

        DataEncoder.loadVdsProfileDefinitionFromXml(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <profile>
                <profileNumber>$uuid</profileNumber>
                <versionTR>0.9</versionTR>
                <profileName>TEST_V9_PROFILE</profileName>
                <creator>Test</creator>
                <validFromPresent>false</validFromPresent>
                <validToPresent>false</validToPresent>
                <entry tag="10"><name>SURNAME</name><description>Familienname</description><type>UTF8String</type></entry>
            </profile>
            """.trimIndent()
        )

        assertNull(seal.baseDocumentType)
        assertEquals(listOf("0x0A"), seal.messageList.map { it.name })
        val reparsed = VdsSeal.fromByteArray(raw) as VdsSeal
        assertEquals(listOf("SURNAME"), reparsed.messageList.map { it.name }, "a new parse resolves the profile")
    }

    private fun idbSeal(vararg tlvs: DerTlv) =
        IdbSeal(isSigned = false, isZipped = false, IdbPayload(IdbHeader("D<<"), IdbMessageGroup(tlvs.toList()), null, null))

    @Test
    fun idb_addedMessageTypesAfterParsing_doNotChangeTheSeal() {
        val seal = idbSeal(DerTlv(0x09, DataEncoder.encodeC40("123456")))
        assertEquals("CAN", seal.messageList.single().name)

        DataEncoder.addCustomIdbMessageTypes("""[{"name": "MY_CAN", "tag": 9, "coding": "BYTES"}]""")

        assertEquals("CAN", seal.messageList.single().name)
        assertEquals("123456", seal.messageList.single().value.toString())
    }

    @Test
    fun idb_addedDocumentTypesAfterParsing_doNotChangeTheSeal() {
        val seal = idbSeal(
            DerTlv(0x82.toByte(), DataEncoder.encodeC40("ABC123456")),
            DerTlv(0x85.toByte(), DataEncoder.encodeC40("ZZ1234567")),
            DerTlv(0x86.toByte(), byteArrayOf(0x10))
        )
        assertEquals("CERTIFYING_PERMANENT_RESIDENCE", seal.documentType)

        DataEncoder.addCustomIdbDocumentTypes(
            """[{"tag": 16, "name": "MY_DOCUMENT", "messages": [{"name": "CAN", "required": true}]}]"""
        )

        assertEquals("CERTIFYING_PERMANENT_RESIDENCE", seal.documentType)
        assertEquals(emptyList(), seal.formatFindings)
    }
}
