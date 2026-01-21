package de.tsenger.vdstools.generic

import de.tsenger.vdstools.idb.*
import de.tsenger.vdstools.vds.MessageCoding
import de.tsenger.vdstools.vds.MessageValue
import de.tsenger.vdstools.vds.VdsHeader
import de.tsenger.vdstools.vds.VdsMessageGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Generic test cases demonstrating the creation of IDB and VDS seals
 * with focus on Message and MessageGroup creation.
 *
 * IDB Message Type Tags (from IdbMessageTypes.json):
 * - 0x01 (1): VISA
 * - 0x02 (2): EMERGENCY_TRAVEL_DOCUMENT
 * - 0x03 (3): PROOF_OF_TESTING
 * - 0x04 (4): PROOF_OF_VACCINATION
 * - 0x05 (5): PROOF_OF_RECOVERY
 * - 0x06 (6): DIGITAL_TRAVEL_AUTHORIZATION
 * - 0x07 (7): MRZ_TD1
 * - 0x08 (8): MRZ_TD3
 * - 0x09 (9): CAN
 * - 0x0A (10): EF_CARD_ACCESS
 * - 0x80 (128): FACE_IMAGE
 * - 0x81 (129): MRZ_TD2
 * - 0x82 (130): DOCUMENT_REFERENCE
 * - 0x83 (131): AZR
 * - 0x84 (132): EXPIRY_DATE
 * - 0x85 (133): DOCUMENT_NUMBER
 * - 0x86 (134): NATIONAL_DOCUMENT_IDENTIFIER
 */
@OptIn(ExperimentalStdlibApi::class)
class SealCreationCommonTest {

    // ========== VDS MessageGroup and Message Tests ==========

    @Test
    fun testVdsMessageGroup_ResidencePermit() {
        // Create a VDS MessageGroup for RESIDENCE_PERMIT
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06"
        val passportNumber = "UFO001979"

        val messageGroup = VdsMessageGroup.Builder("RESIDENCE_PERMIT")
            .addMessage("MRZ", mrz)
            .addMessage("PASSPORT_NUMBER", passportNumber)
            .build()

        assertNotNull(messageGroup)
        assertEquals("RESIDENCE_PERMIT", messageGroup.vdsType)

        // Verify messages are accessible
        val messageList = messageGroup.messageList
        assertEquals(2, messageList.size)

        // Check MRZ message
        val mrzFeature = messageGroup.getMessage("MRZ")
        assertNotNull(mrzFeature)
        assertEquals("MRZ", mrzFeature.name)
        assertEquals(MessageCoding.MRZ, mrzFeature.coding)

        // Check PASSPORT_NUMBER message
        val passportFeature = messageGroup.getMessage("PASSPORT_NUMBER")
        assertNotNull(passportFeature)
        assertEquals("PASSPORT_NUMBER", passportFeature.name)
    }

    @Test
    fun testVdsMessageGroup_EmergencyTravelDocument() {
        val mrz = "PPD<<FOLKS<<TALLULAH<<<<<<<<<<<<<<<<<<<<<<<<\n3113883489D<<9709155F1601013<<<<<<<<<<<<<<04"

        val messageGroup = VdsMessageGroup.Builder("ICAO_EMERGENCY_TRAVEL_DOCUMENT")
            .addMessage("MRZ", mrz)
            .build()

        assertNotNull(messageGroup)
        assertEquals("ICAO_EMERGENCY_TRAVEL_DOCUMENT", messageGroup.vdsType)

        val mrzFeature = messageGroup.getMessage("MRZ")
        assertNotNull(mrzFeature)
        assertEquals(0x02, mrzFeature.tag)
    }

    @Test
    fun testVdsMessageGroup_FeatureAccessByTag() {
        val messageGroup = VdsMessageGroup.Builder("RESIDENCE_PERMIT")
            .addMessage("MRZ", "ATD<<TEST<<DATA<<<<<<<<<<<<<<<<<<<<1234567890USA7001011M2501011<<<<<<00")
            .build()

        // Access by tag (MRZ has tag 0x02)
        val featureByTag = messageGroup.getMessage(0x02)
        assertNotNull(featureByTag)
        assertEquals("MRZ", featureByTag.name)

        // Access by name
        val featureByName = messageGroup.getMessage("MRZ")
        assertNotNull(featureByName)
        assertEquals(0x02, featureByName.tag)
    }

    @Test
    fun testVdsHeader_Creation() {
        val header = VdsHeader.Builder("RESIDENCE_PERMIT")
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .build()

        assertNotNull(header)
        assertEquals("D<<", header.issuingCountry)
        assertEquals("DETS", header.signerIdentifier)
        assertEquals("32", header.certificateReference)
        assertEquals("DETS32", header.signerCertRef)
    }

    // ========== IDB MessageGroup and Message Tests ==========

    @Test
    fun testIdbMessageGroup_SubstituteIdentityDocument() {
        // Create an IDB MessageGroup for a substitute identity document
        // MRZ_TD2 has tag 0x81 (129)
        val mrzTd2 = "IDD<<KOEPPENIK<<JONATHAN<GERALD<<<<<2L1T3QPB04D<<8506210M2604239"
        val faceImage = "faceImageBytesHere".encodeToByteArray()

        val messageGroup = IdbMessageGroup.Builder()
            .addMessage(0x80, faceImage)                    // FACE_IMAGE (tag 128)
            .addMessage(0x81, mrzTd2)                       // MRZ_TD2 (tag 129)
            .addMessage(0x84, "2026-04-23")                 // EXPIRY_DATE (tag 132)
            .addMessage(0x86, 0x01)                         // NATIONAL_DOCUMENT_IDENTIFIER (tag 134)
            .build()

        assertNotNull(messageGroup)

        // Verify messages
        val messageList = messageGroup.messageList
        assertEquals(4, messageList.size)

        // Check by tag
        val faceFeature = messageGroup.getMessage(0x80)
        assertNotNull(faceFeature)
        assertEquals("FACE_IMAGE", faceFeature.name)
        assertEquals(MessageCoding.BYTES, faceFeature.coding)

        // Check by name
        val mrzFeature = messageGroup.getMessage("MRZ_TD2")
        assertNotNull(mrzFeature)
        assertEquals(0x81, mrzFeature.tag)
        assertEquals(MessageCoding.MRZ, mrzFeature.coding)

        // Check date message
        val expiryFeature = messageGroup.getMessage("EXPIRY_DATE")
        assertNotNull(expiryFeature)
        assertEquals(MessageCoding.MASKED_DATE, expiryFeature.coding)

        // Check byte message (document type)
        val docTypeFeature = messageGroup.getMessage(0x86)
        assertNotNull(docTypeFeature)
        assertEquals("NATIONAL_DOCUMENT_IDENTIFIER", docTypeFeature.name)
        assertEquals(MessageCoding.BYTE, docTypeFeature.coding)
        assertTrue(docTypeFeature.value is MessageValue.ByteValue)
        assertEquals(0x01, (docTypeFeature.value).value)
    }

    @Test
    fun testIdbMessageGroup_ArrivalAttestation() {
        // MRZ_TD2 with tag 0x81
        val mrzTd2 = "AUD<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<6525845096USA7008038M2201018"

        val messageGroup = IdbMessageGroup.Builder()
            .addMessage("MRZ_TD2", mrzTd2)
            .addMessage("AZR", "ABC123456DEF")
            .addMessage("NATIONAL_DOCUMENT_IDENTIFIER", 0x0D)  // Arrival Attestation
            .build()

        assertNotNull(messageGroup)
        assertEquals(3, messageGroup.messageList.size)

        val docType = messageGroup.getMessage("NATIONAL_DOCUMENT_IDENTIFIER")
        assertNotNull(docType)
        assertEquals(0x0D, (docType.value as MessageValue.ByteValue).value)
    }

    @Test
    fun testIdbMessageGroup_WithStringNames() {
        // Test using message names instead of tags
        val messageGroup = IdbMessageGroup.Builder()
            .addMessage("PROOF_OF_VACCINATION", "b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray())
            .build()

        assertNotNull(messageGroup)

        val feature = messageGroup.getMessage("PROOF_OF_VACCINATION")
        assertNotNull(feature)
        assertEquals(0x04, feature.tag)
        assertEquals(MessageCoding.BYTES, feature.coding)
    }

    @Test
    fun testIdbHeader_Creation() {
        val header = IdbHeader(
            countryIdentifier = "D<<",
            signatureAlgorithm = IdbSignatureAlgorithm.SHA256_WITH_ECDSA,
            certificateReference = byteArrayOf(0x05, 0x04, 0x03, 0x02, 0x01),
            signatureCreationDate = "2025-01-19"
        )

        assertNotNull(header)
        assertEquals("D", header.getCountryIdentifier())  // trim() removes fill characters
        assertEquals(IdbSignatureAlgorithm.SHA256_WITH_ECDSA, header.getSignatureAlgorithm())
        assertEquals("2025-01-19", header.getSignatureCreationDate())
    }

    @Test
    fun testIdbHeader_UnsignedCreation() {
        val header = IdbHeader("UTO")

        assertNotNull(header)
        assertEquals("UTO", header.getCountryIdentifier())
        assertEquals(null, header.getSignatureAlgorithm())
    }

    // ========== IDB Seal Creation (without actual signing) ==========

    @Test
    fun testIdbSeal_UnsignedCreation() {
        val header = IdbHeader("D<<")

        val messageGroup = IdbMessageGroup.Builder()
            .addMessage("PROOF_OF_RECOVERY", "b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray())
            .build()

        val payload = IdbPayload(header, messageGroup, null, null)
        val seal = IdbSeal(isSigned = false, isZipped = false, barcodePayload = payload)

        assertNotNull(seal)
        assertEquals(false, seal.isSigned)
        assertEquals(false, seal.isZipped)
        assertEquals("D", seal.issuingCountry)  // trim() removes fill characters

        // Check that rawString starts with barcode identifier
        assertTrue(seal.rawString.startsWith("RDB1"))
    }

    @Test
    fun testIdbSeal_WithSignature() {
        val header = IdbHeader(
            countryIdentifier = "D<<",
            signatureAlgorithm = IdbSignatureAlgorithm.SHA256_WITH_ECDSA,
            certificateReference = byteArrayOf(0x05, 0x04, 0x03, 0x02, 0x01),
            signatureCreationDate = "2025-01-19"
        )

        val messageGroup = IdbMessageGroup.Builder()
            .addMessage("PROOF_OF_VACCINATION", "b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray())
            .build()

        // Use a dummy signature for testing
        val signature = IdbSignature(
            "24bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7".hexToByteArray()
        )

        val payload = IdbPayload(header, messageGroup, null, signature)
        val seal = IdbSeal(isSigned = true, isZipped = false, barcodePayload = payload)

        assertNotNull(seal)
        assertEquals(true, seal.isSigned)
        assertEquals(false, seal.isZipped)
        assertEquals("D", seal.issuingCountry)
        assertNotNull(seal.signature)

        // Verify message can be retrieved
        val msg = seal.getMessage("PROOF_OF_VACCINATION")
        assertNotNull(msg)
        assertEquals(0x04, msg.messageTypeTag)
    }

    @Test
    fun testIdbSeal_RoundTrip() {
        // Create a seal and parse it back from its rawString
        val header = IdbHeader(
            countryIdentifier = "UTO",
            signatureAlgorithm = IdbSignatureAlgorithm.SHA256_WITH_ECDSA,
            certificateReference = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05),
            signatureCreationDate = "2025-01-19"
        )

        val messageGroup = IdbMessageGroup.Builder()
            .addMessage(0x81, "IDD<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<L01X00T471D<<6408125F2702281")
            .addMessage(0x86, 0x01)
            .build()

        val signature = IdbSignature(
            "1122334455667788990011223344556677889900112233445566778899001122334455667788990011223344556677889900112233445566".hexToByteArray()
        )

        val payload = IdbPayload(header, messageGroup, null, signature)
        val originalSeal = IdbSeal(isSigned = true, isZipped = false, barcodePayload = payload)

        // Parse back
        val parsedSeal = IdbSeal.fromString(originalSeal.rawString) as IdbSeal

        // Verify
        assertEquals(originalSeal.issuingCountry, parsedSeal.issuingCountry)
        assertEquals(originalSeal.isSigned, parsedSeal.isSigned)
        assertEquals(originalSeal.isZipped, parsedSeal.isZipped)
        assertEquals(
            originalSeal.getMessage(0x81)?.value?.rawBytes?.toHexString(),
            parsedSeal.getMessage(0x81)?.value?.rawBytes?.toHexString()
        )
    }

    // ========== Comparison: VDS vs IDB API patterns ==========

    @Test
    fun testApiConsistency_FeatureAccess() {
        // VDS: Create message group and access messages
        val vdsMessageGroup = VdsMessageGroup.Builder("RESIDENCE_PERMIT")
            .addMessage("MRZ", "ATD<<TEST<<<<<<<<<<<<<<<<<<<<<<<<<<1234567890USA7001011M2501011<<<<<<00")
            .build()

        val vdsFeature = vdsMessageGroup.getMessage("MRZ")
        assertNotNull(vdsFeature)
        assertEquals("MRZ", vdsFeature.name)
        assertEquals(0x02, vdsFeature.tag)
        assertNotNull(vdsFeature.value)
        assertNotNull(vdsFeature.coding)

        // IDB: Create message group and access messages (same pattern!)
        val idbMessageGroup = IdbMessageGroup.Builder()
            .addMessage("MRZ_TD2", "IDD<<TEST<<<<<<<<<<<<<<<<<<<<<<<<<<1234567890USA7001011M2501011")
            .build()

        val idbFeature = idbMessageGroup.getMessage("MRZ_TD2")
        assertNotNull(idbFeature)
        assertEquals("MRZ_TD2", idbFeature.name)
        assertEquals(0x81, idbFeature.tag)
        assertNotNull(idbFeature.value)
        assertNotNull(idbFeature.coding)

        // Both have consistent API:
        // - getMessage(name) / getMessage(tag)
        // - messageList property
        // - Feature/IdbFeature have: tag, name, coding, value
    }

    @Test
    fun testApiConsistency_FeatureList() {
        val vdsMessageGroup = VdsMessageGroup.Builder("RESIDENCE_PERMIT")
            .addMessage("MRZ", "ATD<<TEST<<<<<<<<<<<<<<<<<<<<<<<<<<1234567890USA7001011M2501011<<<<<<00")
            .addMessage("PASSPORT_NUMBER", "ABC123")
            .build()

        val idbMessageGroup = IdbMessageGroup.Builder()
            .addMessage("MRZ_TD2", "IDD<<TEST<<<<<<<<<<<<<<<<<<<<<<<<<<1234567890USA7001011M2501011")
            .addMessage("AZR", "ABC123456DEF")
            .build()

        // Both use messageList property
        assertEquals(2, vdsMessageGroup.messageList.size)
        assertEquals(2, idbMessageGroup.messageList.size)

        // Both messages have same structure
        vdsMessageGroup.messageList.forEach { feature ->
            assertNotNull(feature.tag)
            assertNotNull(feature.name)
            assertNotNull(feature.coding)
            assertNotNull(feature.value)
        }

        idbMessageGroup.messageList.forEach { feature ->
            assertNotNull(feature.tag)
            assertNotNull(feature.name)
            assertNotNull(feature.coding)
            assertNotNull(feature.value)
        }
    }
}
