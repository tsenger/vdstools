package de.tsenger.vdstools.vds;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VdsTypeTest {

	@Test
	public void testGetValue() {
		assertEquals(0xf908, VdsType.ADDRESS_STICKER_ID.getValue());
	}

	@Test
	public void testValueOf() {
		assertEquals(VdsType.ADDRESS_STICKER_ID, VdsType.valueOf(0xf908));
	}

	@Test
	public void testValueOf_unknown() {
		assertEquals(null, VdsType.valueOf(0x6666));
	}

}
