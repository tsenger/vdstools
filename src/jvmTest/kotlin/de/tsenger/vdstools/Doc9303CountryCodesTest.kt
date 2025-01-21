package de.tsenger.vdstools

import de.tsenger.vdstools.Doc9303CountryCodes.convertToIcaoOrIso3
import org.junit.Assert
import org.junit.Test

class Doc9303CountryCodesTest {
    @Test
    fun testConvertToIcaoOrIso3_DE() {
        Assert.assertEquals("D<<", convertToIcaoOrIso3("DE"))
    }

    @Test
    fun testConvertToIcaoOrIso3_EU() {
        Assert.assertEquals("EUE", convertToIcaoOrIso3("EU"))
    }

    @Test
    fun testConvertToIcaoOrIso3_US() {
        Assert.assertEquals("USA", convertToIcaoOrIso3("US"))
    }

    @Test
    fun testConvertToIcaoOrIso3_FR() {
        Assert.assertEquals("FRA", convertToIcaoOrIso3("FR"))
    }
}
