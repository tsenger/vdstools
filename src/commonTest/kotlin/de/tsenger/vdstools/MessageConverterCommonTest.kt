package de.tsenger.vdstools


import de.tsenger.vdstools.asn1.DerTlv
import okio.FileNotFoundException
import kotlin.test.*

@OptIn(ExperimentalStdlibApi::class)
class MessageConverterCommonTest {
//    @Test
//    fun testMessageConverter() {
//        MessageConverter()
//    }

    @Test
    fun testMessageConverterString() {
        val jsonString = readTextResource("SealCodings.json")
        MessageConverter(jsonString)
    }

    @Test
    fun testMessageConverterString_notFound() {
        assertFailsWith<FileNotFoundException> { readTextResource("Codings.json") }

    }


    @Test
    fun testGetMessage_String() {
        val jsonString = readTextResource("SealCodings.json")
        val messageConverter = MessageConverter(jsonString)
        val message = messageConverter.getMessageName(
            "FICTION_CERT",
            DerTlv.fromByteArray(
                "0306d79519a65306".hexToByteArray()
            )!!
        )
        assertEquals("PASSPORT_NUMBER", message)
    }


    @Test
    fun testEncodeMessage_String() {
        val jsonString = readTextResource("SealCodings.json")
        val messageConverter = MessageConverter(jsonString)
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06"
        val derTlv = messageConverter.encodeMessage("RESIDENCE_PERMIT", "MRZ", mrz)
        assertEquals(
            "02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b",
            derTlv.encoded.toHexString()
        )
    }

    @Test
    fun testGetAvailableVdsTypes() {
        val jsonString = readTextResource("SealCodings.json")
        val messageConverter = MessageConverter(jsonString)
        println(messageConverter.availableVdsTypes)
        assertTrue(messageConverter.availableVdsTypes.contains("ADDRESS_STICKER_ID"))
    }

    @Test
    fun testGetAvailableVdsMessages() {
        val jsonString = readTextResource("SealCodings.json")
        val messageConverter = MessageConverter(jsonString)
        println(messageConverter.availableVdsMessages)
        assertTrue(messageConverter.availableVdsMessages.contains("MRZ"))
    }

    @Test
    fun testGetDocumentRef_fakeSeal() {
        assertNull(DataEncoder.getDocumentRef("FAKE_SEAL"))
    }
}
