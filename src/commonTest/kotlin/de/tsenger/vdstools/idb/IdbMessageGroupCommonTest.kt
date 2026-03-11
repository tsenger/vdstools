package de.tsenger.vdstools.idb

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.idb.IdbMessageGroup.Companion.fromByteArray
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalStdlibApi::class)
class IdbMessageGroupCommonTest {
    @Test
    fun testConstructorEmpty() {
        val messageGroup = IdbMessageGroup(emptyList())
        assertNotNull(messageGroup)
    }

    @Test
    fun testConstructorWithDerTlv() {
        val derTlv = DerTlv(0x09, "a0a1a2a3a4a5a6a7a8a9aa".hexToByteArray())
        val messageGroup = IdbMessageGroup(listOf(derTlv))
        assertNotNull(messageGroup)
        assertEquals(1, messageGroup.messageList.size.toLong())
    }


    @Test
    fun testMessageList() {
        val messageGroup = IdbMessageGroup.Builder()
            .addMessage("CAN", "654321")
            .addMessage(
                "MRZ_TD1",
                "I<URYEWCVECOXY8<<<<<<<<<<<<<<<7206122M2811062URY<<<<<<<<<<<8BUCKLEY<<WINIFRED<<<<<<<<<<<<<"
            )
            .build()
        assertEquals(2, messageGroup.messageList.size.toLong())
        val messageList = messageGroup.messageList
        assertEquals("CAN", messageList[0].name)
        assertEquals("MRZ_TD1", messageList[1].name)
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncoded() {
        val derTlv1 = DerTlv(0x09, "a0a1a2a3a4a5a6a7a8a9aa".hexToByteArray())
        val derTlv2 = DerTlv(0x07, "b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray())
        val messageGroup = IdbMessageGroup(listOf(derTlv1, derTlv2))
        assertEquals(
            "611f090ba0a1a2a3a4a5a6a7a8a9aa0710b0b1b2b3b4b5b6b7b8b9babbbcbdbebf", messageGroup.encoded.toHexString()
        )
    }

    @Test
    @Throws(IOException::class)
    fun testFromByteArray() {
        val messageGroup =
            fromByteArray("611f090ba0a1a2a3a4a5a6a7a8a9aa0710b0b1b2b3b4b5b6b7b8b9babbbcbdbebf".hexToByteArray())
        val messageList = messageGroup.messageList
        assertEquals("CAN", messageList[0].name)
        assertEquals("MRZ_TD1", messageList[1].name)
    }

    @Test
    fun testNestedSubMessageParsing() {
        // Build a PROOF_OF_VACCINATION with nested VACCINATION_EVENT > VACCINATION_DETAILS > DATE_OF_VACCINATION
        val messageGroup = IdbMessageGroup.Builder()
            .addMessage("PROOF_OF_VACCINATION") {
                addMessage("UVCI", "01DE1234V1")
                addMessage("NAME_OF_HOLDER", "DOE JOHN")
                addMessage("SEX", "M")
                addMessage("VACCINATION_EVENT") {
                    addMessage("VACCINE_OR_PROPHYLAXIS", "J07BX0")
                    addMessage("DISEASE_OR_AGENT", "RA0100")
                    addMessage("VACCINE_BRAND", "COVAX")
                    addMessage("VACCINATION_DETAILS") {
                        addMessage("DATE_OF_VACCINATION", "2021-03-15")
                        addMessage("DOSE_NUMBER", "1")
                        addMessage("COUNTRY_OF_VACCINATION", "DE")
                        addMessage("ADMINISTERING_CENTRE", "HOSP1")
                        addMessage("VACCINE_BATCH_NUMBER", "B12345")
                    }
                }
            }
            .build()

        val pov = messageGroup.getMessage("PROOF_OF_VACCINATION")
        assertNotNull(pov)

        // Navigate into sub-messages
        val event = pov.getMessageByName("VACCINATION_EVENT")
        assertNotNull(event, "VACCINATION_EVENT should be found as sub-message")

        val details = event.getMessageByName("VACCINATION_DETAILS")
        assertNotNull(details, "VACCINATION_DETAILS should be found as sub-message of VACCINATION_EVENT")

        val dateOfVax = details.getMessageByName("DATE_OF_VACCINATION")
        assertNotNull(dateOfVax, "DATE_OF_VACCINATION should be found as sub-message of VACCINATION_DETAILS")
        assertEquals("2021-03-15", dateOfVax.value.toString())
    }

    @Test
    fun testSubMessageTagBasedAccess() {
        val messageGroup = IdbMessageGroup.Builder()
            .addMessage("PROOF_OF_VACCINATION") {
                addMessage("UVCI", "01DE1234V1")
                addMessage("NAME_OF_HOLDER", "DOE JOHN")
                addMessage("SEX", "M")
                addMessage("VACCINATION_EVENT") {
                    addMessage("VACCINE_OR_PROPHYLAXIS", "J07BX0")
                    addMessage("DISEASE_OR_AGENT", "RA0100")
                    addMessage("VACCINE_BRAND", "COVAX")
                    addMessage("VACCINATION_DETAILS") {
                        addMessage("DATE_OF_VACCINATION", "2021-03-15")
                        addMessage("DOSE_NUMBER", "1")
                        addMessage("COUNTRY_OF_VACCINATION", "DE")
                        addMessage("ADMINISTERING_CENTRE", "HOSP1")
                        addMessage("VACCINE_BATCH_NUMBER", "B12345")
                    }
                }
            }
            .build()

        val pov = messageGroup.getMessage("PROOF_OF_VACCINATION")
        assertNotNull(pov)

        // Tag-based access: VACCINATION_EVENT has tag 9
        val eventByInt = pov.getMessageByTag(9)
        assertNotNull(eventByInt)
        assertEquals("VACCINATION_EVENT", eventByInt.name)

        // Hex string access
        val eventByHex = pov.getMessageByTag("09")
        assertNotNull(eventByHex)
        assertEquals("VACCINATION_EVENT", eventByHex.name)
    }

    @Test
    fun testSubMessageIteration() {
        val messageGroup = IdbMessageGroup.Builder()
            .addMessage("PROOF_OF_VACCINATION") {
                addMessage("UVCI", "01DE1234V1")
                addMessage("NAME_OF_HOLDER", "DOE JOHN")
                addMessage("SEX", "M")
                addMessage("VACCINATION_EVENT") {
                    addMessage("VACCINE_OR_PROPHYLAXIS", "J07BX0")
                    addMessage("DISEASE_OR_AGENT", "RA0100")
                    addMessage("VACCINE_BRAND", "COVAX")
                    addMessage("VACCINATION_DETAILS") {
                        addMessage("DATE_OF_VACCINATION", "2021-03-15")
                        addMessage("DOSE_NUMBER", "1")
                        addMessage("COUNTRY_OF_VACCINATION", "DE")
                        addMessage("ADMINISTERING_CENTRE", "HOSP1")
                        addMessage("VACCINE_BATCH_NUMBER", "B12345")
                    }
                }
            }
            .build()

        val event = messageGroup.getMessage("PROOF_OF_VACCINATION")
            ?.getMessageByName("VACCINATION_EVENT")
        assertNotNull(event)

        // VACCINATION_EVENT should have sub-messages
        assertTrue(event.messages.isNotEmpty(), "VACCINATION_EVENT should have sub-messages")

        val subNames = event.messages.map { it.name }
        assertTrue("VACCINE_OR_PROPHYLAXIS" in subNames)
        assertTrue("VACCINATION_DETAILS" in subNames)
    }

    @Test
    fun testBuilderRoundtrip() {
        // Build → encode → decode → verify navigation
        val original = IdbMessageGroup.Builder()
            .addMessage("PROOF_OF_VACCINATION") {
                addMessage("UVCI", "01DE1234V1")
                addMessage("NAME_OF_HOLDER", "DOE JOHN")
                addMessage("SEX", "M")
                addMessage("VACCINATION_EVENT") {
                    addMessage("VACCINE_OR_PROPHYLAXIS", "J07BX0")
                    addMessage("DISEASE_OR_AGENT", "RA0100")
                    addMessage("VACCINE_BRAND", "COVAX")
                    addMessage("VACCINATION_DETAILS") {
                        addMessage("DATE_OF_VACCINATION", "2021-03-15")
                        addMessage("DOSE_NUMBER", "1")
                        addMessage("COUNTRY_OF_VACCINATION", "DE")
                        addMessage("ADMINISTERING_CENTRE", "HOSP1")
                        addMessage("VACCINE_BATCH_NUMBER", "B12345")
                    }
                }
            }
            .build()

        val encoded = original.encoded
        val decoded = fromByteArray(encoded)

        val pov = decoded.getMessage("PROOF_OF_VACCINATION")
        assertNotNull(pov)

        val event = pov.getMessageByName("VACCINATION_EVENT")
        assertNotNull(event)

        val details = event.getMessageByName("VACCINATION_DETAILS")
        assertNotNull(details)

        val dateOfVax = details.getMessageByName("DATE_OF_VACCINATION")
        assertNotNull(dateOfVax)
        assertEquals("2021-03-15", dateOfVax.value.toString())

        val country = details.getMessageByName("COUNTRY_OF_VACCINATION")
        assertNotNull(country)
        assertEquals("DE", country.value.toString())
    }

    @Test
    fun testVdsMessagesHaveEmptySubMessages() {
        // Non-container messages should have empty messages list
        val messageGroup = IdbMessageGroup.Builder()
            .addMessage("CAN", "654321")
            .build()

        val can = messageGroup.getMessage("CAN")
        assertNotNull(can)
        assertTrue(can.messages.isEmpty(), "Non-container messages should have empty messages list")
    }
}
