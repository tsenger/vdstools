package de.tsenger.vdstools


import de.tsenger.vdstools.asn1.DerTlv
import okio.FileNotFoundException
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test

class VdsSealCodingRegistryJvmTest {

    @Test
    fun testVdsSealCodingRegistryString() {
        val jsonString = readTextResource("SealCodings.json")
        VdsSealCodingRegistry(jsonString)
    }

    @Test(expected = FileNotFoundException::class)
    fun testVdsSealCodingRegistryString_notFound() {
        val jsonString = readTextResource("Codings.json")
        VdsSealCodingRegistry(jsonString)
    }

    @Test
    fun testGetMessage_String() {
        val jsonString = readTextResource("SealCodings.json")
        val registry = VdsSealCodingRegistry(jsonString)
        val message = registry.getMessageName(
            "FICTION_CERT",
            DerTlv.fromByteArray(Hex.decode("0306d79519a65306"))!!
        )
        Assert.assertEquals("PASSPORT_NUMBER", message)
    }


    @Test
    fun testEncodeMessage_String() {
        val jsonString = readTextResource("SealCodings.json")
        val registry = VdsSealCodingRegistry(jsonString)
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06"
        val derTlv = registry.encodeMessage("RESIDENCE_PERMIT", "MRZ", mrz)
        Assert.assertEquals(
            "02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b",
            Hex.toHexString(derTlv.encoded)
        )
    }

    @Test
    fun testGetAvailableVdsTypes() {
        val jsonString = readTextResource("SealCodings.json")
        val registry = VdsSealCodingRegistry(jsonString)
        println(registry.availableVdsTypes)
        Assert.assertTrue(registry.availableVdsTypes.contains("ADDRESS_STICKER_ID"))
    }

    @Test
    fun testGetAvailableVdsMessages() {
        val jsonString = readTextResource("SealCodings.json")
        val registry = VdsSealCodingRegistry(jsonString)
        println(registry.availableVdsMessages)
        Assert.assertTrue(registry.availableVdsMessages.contains("MRZ"))
    }

    @Test
    fun testGetDocumentRef_fakeSeal() {
        Assert.assertNull(DataEncoder.getDocumentRef("FAKE_SEAL"))
    }
}