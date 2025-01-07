package de.tsenger.vdstools;

import static org.junit.Assert.assertEquals;
import static de.tsenger.vdstools.Doc9303CountryCodes.convertToIcaoOrIso3;

import org.junit.Test;

public class Doc9303CountryCodesTest {

	@Test
	public void testConvertToIcaoOrIso3_DE() {
		assertEquals("D<<", convertToIcaoOrIso3("DE"));
	}
	
	@Test
	public void testConvertToIcaoOrIso3_EU() {
		assertEquals("EUE", convertToIcaoOrIso3("EU"));
	}
	
	@Test
	public void testConvertToIcaoOrIso3_US() {
		assertEquals("USA", convertToIcaoOrIso3("US"));
	}
	
	@Test
	public void testConvertToIcaoOrIso3_FR() {
		assertEquals("FRA", convertToIcaoOrIso3("FR"));
	}

}
