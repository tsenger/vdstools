package de.tsenger.vdstools.vds

import kotlin.test.Test
import kotlin.test.assertContentEquals


@OptIn(ExperimentalStdlibApi::class)
class VdsMessageGroupIosTest {

    @Test
    fun testBuildVdsMessageGroup() {
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06"
        val passportNumber = "UFO001979"
        val vdsMessage = VdsMessageGroup.Builder("RESIDENCE_PERMIT")
            .addFeature("MRZ", mrz)
            .addFeature("PASSPORT_NUMBER", passportNumber)
            .build()
        assertContentEquals(
            "02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306".hexToByteArray(),
            vdsMessage.encoded
        )
    }


    @Test
    fun testFromByteArray() {
        val messageBytes =
            ("02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306").hexToByteArray()
        val message = VdsMessageGroup.fromByteArray(messageBytes, "RESIDENCE_PERMIT")
        assertContentEquals(messageBytes, message.encoded)
    }
}
