package de.tsenger.vdstools.vds

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalStdlibApi::class)
class VdsMessageGroupCommonTest {

    @Test
    fun testBuildVdsMessageGroup() {
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06"
        val passportNumber = "UFO001979"
        val vdsMessage = VdsMessageGroup.Builder("RESIDENCE_PERMIT")
            .addFeature("MRZ", mrz)
            .addFeature("PASSPORT_NUMBER", passportNumber)
            .build()
        assertEquals(
            "02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306",
            vdsMessage.encoded.toHexString()
        )
    }

    @Test
    fun testBuildVdsMessageGroupWithTag() {
        // MRZ has tag 2, PASSPORT_NUMBER has tag 3 for RESIDENCE_PERMIT
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06"
        val passportNumber = "UFO001979"
        val vdsMessage = VdsMessageGroup.Builder("RESIDENCE_PERMIT")
            .addFeature(0x02, mrz)
            .addFeature(0x03, passportNumber)
            .build()
        assertEquals(
            "02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306",
            vdsMessage.encoded.toHexString()
        )
    }

    @Test
    fun testBuildVdsMessageGroupMixedNameAndTag() {
        // Use name for MRZ and tag for PASSPORT_NUMBER
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06"
        val passportNumber = "UFO001979"
        val vdsMessage = VdsMessageGroup.Builder("RESIDENCE_PERMIT")
            .addFeature("MRZ", mrz)
            .addFeature(0x03, passportNumber)
            .build()
        assertEquals(
            "02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306",
            vdsMessage.encoded.toHexString()
        )
    }

    @Test
    fun testBuildVdsMessageGroupWithInvalidTag() {
        assertFailsWith<IllegalArgumentException> {
            VdsMessageGroup.Builder("RESIDENCE_PERMIT")
                .addFeature(0xFF, "some value")
                .build()
        }
    }

    @Test
    fun testFromByteArray() {
        val messageBytes =
            "02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306".hexToByteArray()
        val message = VdsMessageGroup.fromByteArray(messageBytes, "RESIDENCE_PERMIT")
        assertContentEquals(messageBytes, message.encoded)
    }
}
