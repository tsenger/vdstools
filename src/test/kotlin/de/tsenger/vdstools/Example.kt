package de.tsenger.vdstools

import de.tsenger.vdstools.vds.VdsMessage
import org.bouncycastle.util.encoders.Hex
import org.junit.Test

class Example {
    @Test
    fun test() {
        val vdsMessage = VdsMessage.Builder("ICAO_VISA")
            .addDocumentFeature(
                "MRZ_MRVB",
                "VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250<<<<<<<<"
            )
            .addDocumentFeature("PASSPORT_NUMBER", "47110815P")
            .addDocumentFeature("DURATION_OF_STAY", Hex.decode("A00000")).build()
        println(Hex.toHexString(vdsMessage.encoded))
    }
}
