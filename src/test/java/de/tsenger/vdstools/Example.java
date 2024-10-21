package de.tsenger.vdstools;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import de.tsenger.vdstools.vds.Feature;
import de.tsenger.vdstools.vds.VdsMessage;
import de.tsenger.vdstools.vds.VdsType;

public class Example {

	@Test
	public void test() {
		VdsMessage vdsMessage = new VdsMessage(VdsType.ICAO_VISA);
		vdsMessage.addDocumentFeature(Feature.MRZ,
				"VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250<<<<<<<<");
		vdsMessage.addDocumentFeature(Feature.PASSPORT_NUMBER, "47110815P");
		vdsMessage.addDocumentFeature(Feature.DURATION_OF_STAY_RAWBYTES, Hex.decode("A00000"));
		System.out.println(Hex.toHexString(vdsMessage.getEncoded()));

		VdsMessage vdsMessage2 = VdsMessage.fromByteArray(vdsMessage.getEncoded(), VdsType.ICAO_VISA);
		System.out.println(vdsMessage2.getVdsType());
	}

}
