package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.generic.FormatFinding
import de.tsenger.vdstools.generic.SealParser
import kotlin.test.*

/**
 * Lengths of IDB message types are checked when read, like for VDS, only if the message type is
 * verified against a `specification` (bundled: the national types of BSI TR-03137-1 V3.0 §11.2.2).
 */
class IdbMessageLengthCommonTest {

    @BeforeTest
    fun setUp() {
        DataEncoder.resetToDefaults()
    }

    @AfterTest
    fun tearDown() {
        DataEncoder.resetToDefaults()
    }

    private fun seal(block: IdbSeal.Builder.() -> Unit): IdbSeal {
        val built = IdbSeal.Builder().countryIdentifier("D<<").apply(block).build()
        return SealParser().parse(built.rawString) as IdbSeal
    }

    private fun lengthFindings(seal: IdbSeal) = seal.formatFindings.filterIsInstance<FormatFinding.InvalidLength>()

    @Test
    fun bundledNationalTypes_carryTheLengthsOfTheSpecification() {
        val face = DataEncoder.idbMessageTypes.getMessageTypeDto("FACE_IMAGE")!!
        assertEquals("BSI TR-03137-1 V3.0 §11.2.2", face.specification)
        assertEquals(900, face.minBytes)
        assertEquals(1000, face.maxBytes)
        val expected = mapOf(0x81 to 48, 0x82 to 6, 0x83 to 8, 0x84 to 4, 0x85 to 6, 0x86 to 1)
        for ((tag, length) in expected) {
            val dto = DataEncoder.idbMessageTypes.getMessageTypeDto(tag, "D")!!
            assertEquals(length to length, dto.minBytes to dto.maxBytes, dto.name)
            assertNotNull(dto.specification, dto.name)
        }
    }

    @Test
    fun faceImageWithinTheLimits_hasNoFinding() {
        assertEquals(emptyList(), lengthFindings(seal { addMessage("FACE_IMAGE", ByteArray(950)) }))
    }

    @Test
    fun faceImageTooLarge_isInvalidLength() {
        assertEquals(
            listOf(FormatFinding.InvalidLength(listOf(0x80), "FACE_IMAGE", 1200, 900, 1000)),
            lengthFindings(seal { addMessage("FACE_IMAGE", ByteArray(1200)) })
        )
    }

    @Test
    fun faceImageTooSmall_isInvalidLength() {
        assertEquals(
            listOf(FormatFinding.InvalidLength(listOf(0x80), "FACE_IMAGE", 10, 900, 1000)),
            lengthFindings(seal { addMessage("FACE_IMAGE", ByteArray(10)) })
        )
    }

    @Test
    fun c40Lengths_areEncodedBytesNotCharacters() {
        // MRZ-TD2: 72 characters -> 48 bytes C40; AZR: 12 characters -> 8 bytes C40
        val mrz = "AU" + "D".repeat(34) + "1234567890D<<7001011M2501011<<<<<<<6"
        assertEquals(72, mrz.length)
        val seal = seal {
            addMessage("MRZ_TD2", mrz)
            addMessage("AZR", "ABC123456DEF")
        }
        assertEquals(listOf(48, 8), seal.messageList.map { it.value.rawBytes.size })
        assertEquals(emptyList(), lengthFindings(seal))
    }

    @Test
    fun c40ValueTooShort_isInvalidLength() {
        // 9 characters -> 6 bytes, the AZR needs 8
        assertEquals(
            listOf(FormatFinding.InvalidLength(listOf(0x83), "AZR", 6, 8, 8)),
            lengthFindings(seal { addMessage("AZR", "ABC123456") })
        )
    }

    @Test
    fun typeWithoutSpecification_isNotChecked() {
        DataEncoder.addCustomIdbMessageTypes(
            """[{"name": "MY_BYTES", "tag": 135, "coding": "BYTES", "country": "D", "minBytes": 5, "maxBytes": 5}]"""
        )
        assertEquals(emptyList(), lengthFindings(seal { addMessage("MY_BYTES", ByteArray(1)) }))
    }
}
