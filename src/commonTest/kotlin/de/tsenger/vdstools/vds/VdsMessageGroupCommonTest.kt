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
            .addMessage("MRZ", mrz)
            .addMessage("PASSPORT_NUMBER", passportNumber)
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
            .addMessage(0x02, mrz)
            .addMessage(0x03, passportNumber)
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
            .addMessage("MRZ", mrz)
            .addMessage(0x03, passportNumber)
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
                .addMessage(0xFF, "some value")
                .build()
        }
    }

    // MRZ_MRVA: line1 (44) + first 28 chars of line2; full 88-char input must be truncated to 72
    @Test
    fun testMrzMrvaEncodingTruncatesToLine1Plus28() {
        val fullMrz = "VIS<<HOLDER<<GIVEN<NAME<<<<<<<<<<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250OPTIONALDAT06"
        val group = VdsMessageGroup.Builder("VISA")
            .addMessage("MRZ_MRVA", fullMrz)
            .build()
        val mrzMsg = group.getMessageByName("MRZ_MRVA")!!
        // Encoded bytes must be 48 (C40 encoding of 72 chars = 24 triples × 2 bytes)
        assertEquals(48, mrzMsg.value.rawBytes.size)
        // Decoded display: line1 (44 chars) + newline + line2 (28 chars, no optional data)
        assertEquals(
            "VIS<<HOLDER<<GIVEN<NAME<<<<<<<<<<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250",
            mrzMsg.value.toString()
        )
    }

    // MRZ_MRVB: line1 (36) + first 28 chars of line2; full 72-char input must be truncated to 64
    @Test
    fun testMrzMrvbEncodingTruncatesToLine1Plus28() {
        val fullMrz = "VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250OPTDAT06"
        val group = VdsMessageGroup.Builder("VISA")
            .addMessage("MRZ_MRVB", fullMrz)
            .build()
        val mrzMsg = group.getMessageByName("MRZ_MRVB")!!
        // Encoded bytes must be 44 (C40 encoding of 64 chars: 21 triples × 2 bytes + unlatch pair)
        assertEquals(44, mrzMsg.value.rawBytes.size)
        // Decoded display: line1 (36 chars) + newline + line2 (28 chars, no optional data)
        assertEquals(
            "VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250",
            mrzMsg.value.toString()
        )
    }

    // Input without newline separator must yield the same result as input with newline
    @Test
    fun testMrzMrvbEncodingStripsNewlineBeforeTruncation() {
        val mrzWithNewline    = "VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250OPTDAT06"
        val mrzWithoutNewline = "VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<1234567XY7GBR5203116M2005250OPTDAT06"
        val groupWith = VdsMessageGroup.Builder("VISA").addMessage("MRZ_MRVB", mrzWithNewline).build()
        val groupWithout = VdsMessageGroup.Builder("VISA").addMessage("MRZ_MRVB", mrzWithoutNewline).build()
        assertContentEquals(groupWith.encoded, groupWithout.encoded)
    }

    @Test
    fun testFromByteArray() {
        val messageBytes =
            "02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306".hexToByteArray()
        val message = VdsMessageGroup.fromByteArray(messageBytes, "RESIDENCE_PERMIT")
        assertContentEquals(messageBytes, message.encoded)
    }
}
