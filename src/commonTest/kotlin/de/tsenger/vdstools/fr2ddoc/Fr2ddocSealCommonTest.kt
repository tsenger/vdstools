package de.tsenger.vdstools.fr2ddoc

import de.tsenger.vdstools.fr2ddoc.Fr2ddocRawStringsCommon.rawString1
import kotlin.test.Test

class Fr2ddocSealCommonTest {

    @Test
    fun testParseSeal() {
        Fr2ddocSeal.fromRawString(rawString1)

    }
}