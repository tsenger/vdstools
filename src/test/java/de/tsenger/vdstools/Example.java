package de.tsenger.vdstools;

import java.io.ByteArrayOutputStream;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import de.tsenger.vdstools.vds.VdsMessage;

public class Example {

	@Test
	public void test() {
		VdsMessage vdsMessage = new VdsMessage("ICAO_VISA");
		vdsMessage.addDocumentFeature("MRZ_MRVB",
				"VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250<<<<<<<<");
		vdsMessage.addDocumentFeature("PASSPORT_NUMBER", "47110815P");
		vdsMessage.addDocumentFeature("DURATION_OF_STAY", Hex.decode("A00000"));
		System.out.println(Hex.toHexString(vdsMessage.getEncoded()));

	}

	@Test
	public void testBaos() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(2);
		System.out.println(baos.toByteArray().length);
	}

}
