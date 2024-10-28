package de.tsenger.vdstools.vds;

import static org.junit.Assert.assertEquals;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

public class VdsMessageTest {

	//@formatter:off
	@Test
	public void testBuildVdsMessage() {
		String mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<" + "6525845096USA7008038M2201018<<<<<<06";
		String passportNumber = "UFO001979";
		VdsMessage vdsMessage = new VdsMessage.Builder("RESIDENCE_PERMIT")
				.addDocumentFeature("MRZ", mrz)
				.addDocumentFeature("PASSPORT_NUMBER", passportNumber)
				.build();
		assertEquals(
				"02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306",
				Hex.toHexString(vdsMessage.getEncoded()));
	}

}
