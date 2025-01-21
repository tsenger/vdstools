package de.tsenger.vdstools.vds

import org.bouncycastle.util.Arrays
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test

class VdsMessageTest {

    @Test
    fun testBuildVdsMessage() {
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06"
        val passportNumber = "UFO001979"
        val vdsMessage = VdsMessage.Builder("RESIDENCE_PERMIT")
            .addDocumentFeature("MRZ", mrz)
            .addDocumentFeature("PASSPORT_NUMBER", passportNumber)
            .build()
        Assert.assertEquals(
            "02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306",
            Hex.toHexString(vdsMessage.encoded)
        )
    }

    @Test
    fun testFromByteArray() {
        val messageBytes =
            Hex.decode("02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306")
        val message = VdsMessage.fromByteArray(messageBytes, "RESIDENCE_PERMIT")
        Assert.assertTrue(Arrays.areEqual(messageBytes, message.encoded))
    }
}
