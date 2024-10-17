package de.tsenger.vdstools.idb;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IcaoBarcodeFormatTest {

	@Test
	public void testIsNotSignedIsNotZipped() {
		IcaoBarcodeFormat icb = new IcaoBarcodeFormat('A', new BarcodePayload());
		assertFalse(icb.isSigned());
		assertFalse(icb.isZipped());
	}

	@Test
	public void testIsSignedIsNotZipped() {
		IcaoBarcodeFormat icb = new IcaoBarcodeFormat('B', new BarcodePayload());
		assertTrue(icb.isSigned());
		assertFalse(icb.isZipped());
	}

	@Test
	public void testIsNotSignedIsZipped() {
		IcaoBarcodeFormat icb = new IcaoBarcodeFormat('C', new BarcodePayload());
		assertFalse(icb.isSigned());
		assertTrue(icb.isZipped());
	}

	@Test
	public void testIsSignedIsZipped() {
		IcaoBarcodeFormat icb = new IcaoBarcodeFormat('D', new BarcodePayload());
		assertTrue(icb.isSigned());
		assertTrue(icb.isZipped());
	}

}
