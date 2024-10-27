package de.tsenger.vdstools.vds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import de.tsenger.vdstools.DataEncoder;

public class VdsTypeTest {

	@Test
	public void testGetValue() {
		assertEquals(0xf908, DataEncoder.getFeatureEncoder().getDocumentRef("ADDRESS_STICKER_ID"));
	}

	@Test
	public void testValueOf() {
		assertEquals("ADDRESS_STICKER_ID", DataEncoder.getFeatureEncoder().getVdsType(0xf908));
	}

	@Test
	public void testValueOf_unknown() {
		assertNull(DataEncoder.getFeatureEncoder().getVdsType(0x6666));
	}

}
