package de.tsenger.vdstools

import de.tsenger.vdstools.Doc9303CountryCodes.convertToIcaoOrIso3
import kotlin.test.Test
import kotlin.test.assertEquals


class Doc9303CountryCodesCommonTest {
    @Test
    fun testConvertToIcaoOrIso3_DE() {
        assertEquals("D<<", convertToIcaoOrIso3("DE"))
    }

    @Test
    fun testConvertToIcaoOrIso3_EU() {
        assertEquals("EUE", convertToIcaoOrIso3("EU"))
    }

    @Test
    fun testConvertToIcaoOrIso3_US() {
        assertEquals("USA", convertToIcaoOrIso3("US"))
    }

    @Test
    fun testConvertToIcaoOrIso3_FR() {
        assertEquals("FRA", convertToIcaoOrIso3("FR"))
    }
}
