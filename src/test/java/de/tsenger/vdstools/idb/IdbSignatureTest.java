package de.tsenger.vdstools.idb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

public class IdbSignatureTest {

	byte[] idbSignatureBytes = Hex.decode(
			"7f40a1bb9cc00b2ff63050bdfafe845396588ee3035fe2267d51549f55a6688aa9873e1c5a171c8983a1630e85642df982b510cf8eb28b0ce022cf0057582a7f0086");

	@Test
	public void testGetDerSignatureBytes() throws IOException {
		IdbSignature signature = IdbSignature.fromByteArray(idbSignatureBytes);
		assertEquals(
				"3045022100a1bb9cc00b2ff63050bdfafe845396588ee3035fe2267d51549f55a6688aa98702203e1c5a171c8983a1630e85642df982b510cf8eb28b0ce022cf0057582a7f0086",
				Hex.toHexString(signature.getDerSignatureBytes()));

	}

	@Test
	public void testGetPlainSignatureBytes() throws IOException {
		IdbSignature signature = IdbSignature.fromByteArray(idbSignatureBytes);
		assertEquals(
				"a1bb9cc00b2ff63050bdfafe845396588ee3035fe2267d51549f55a6688aa9873e1c5a171c8983a1630e85642df982b510cf8eb28b0ce022cf0057582a7f0086",
				Hex.toHexString(signature.getPlainSignatureBytes()));
	}

	@Test
	public void testGetEncoded() throws IOException {
		IdbSignature signature = new IdbSignature(Hex.decode(
				"a1bb9cc00b2ff63050bdfafe845396588ee3035fe2267d51549f55a6688aa9873e1c5a171c8983a1630e85642df982b510cf8eb28b0ce022cf0057582a7f0086"));
		assertTrue(Arrays.areEqual(idbSignatureBytes, signature.getEncoded()));
	}

	@Test
	public void testFromByteArray() throws IOException {
		IdbSignature signature = IdbSignature.fromByteArray(idbSignatureBytes);
		assertEquals(
				"a1bb9cc00b2ff63050bdfafe845396588ee3035fe2267d51549f55a6688aa9873e1c5a171c8983a1630e85642df982b510cf8eb28b0ce022cf0057582a7f0086",
				Hex.toHexString(signature.getPlainSignatureBytes()));
	}

}
