package de.tsenger.vdstools_mp

import de.tsenger.vdstools_mp.vds.VdsMessage
import kotlin.test.Test

@OptIn(ExperimentalStdlibApi::class)
class ExampleCommon {

    @Test
    fun test() {
        val vdsMessage = VdsMessage.Builder("ICAO_VISA")
            .addDocumentFeature(
                "MRZ_MRVB",
                "VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250<<<<<<<<"
            )
            .addDocumentFeature("PASSPORT_NUMBER", "47110815P")
            .addDocumentFeature("DURATION_OF_STAY", "A00000".hexToByteArray()).build()
        println(vdsMessage.encoded.toHexString())
    }
}
