package de.tsenger.vdstools.vds.tr03171

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.readTextResource

/** TR-03171 XML test profiles from `src/commonTest/resources`. */
object TestProfiles {
    /** Profile name of `test_profile_v08_meldebestaetigung.xml` (UUID 9a4223406d374ef99e2cf95e31a23846). */
    const val MELDEBESTAETIGUNG = "Meldebestätigung"

    fun loadMeldebestaetigung() {
        DataEncoder.loadVdsProfileDefinitionFromXml(readTextResource("test_profile_v08_meldebestaetigung.xml"))
    }
}
