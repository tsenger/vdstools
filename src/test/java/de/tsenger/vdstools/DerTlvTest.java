package de.tsenger.vdstools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

public class DerTlvTest {

	//@formatter:off
    

    static byte[] der_rawBytes = Hex.decode(
    		"0182037bff4fff51002900000000\n"
    		+ "019d0000021300000000000000000000\n"
    		+ "019d0000021300000000000000000001\n"
    		+ "070101ff52000c000200010005040400\n"
    		+ "00ff5c002342772076f076f076c06f00\n"
    		+ "6f006ee0675067506768500550055047\n"
    		+ "57d357d35762ff640025000143726561\n"
    		+ "746564206279204f70656e4a50454720\n"
    		+ "76657273696f6e20322e352e30ff9000\n"
    		+ "0a0000000002f20001ff93c7d1a28014\n"
    		+ "a254717b603970487a48a192f03b40a4\n"
    		+ "17e10ec30c718545a1f116ee9443f7d8\n"
    		+ "9d6a946da5814118980326c118aecaef\n"
    		+ "2bec5c0c348e3dd4ecd02661eaeaf74b\n"
    		+ "ff037c2b4a29bb81b06da6f0b1f09311\n"
    		+ "bf98a370e2407d6b7784e2436377f14a\n"
    		+ "8db4a1de53ac0296e78052918d2f789f\n"
    		+ "fe046068a7d735f06c2d5cb1d3aaa45b\n"
    		+ "5ec1cc05a2f210f200c1f155a1f23283\n"
    		+ "e1a25e527c1053d089bc3c3f43aca8b3\n"
    		+ "79fb80f0a5713bbf97d23113b99de696\n"
    		+ "1929d50f0735f79e56f0296f56bf5059\n"
    		+ "fae888e7c37a5ad7f2b0800e0ab82287\n"
    		+ "cbbab5e66cb6099416cebab039b6b0dd\n"
    		+ "c2e8ae31de209feaa25210d1f1229d63\n"
    		+ "a2a85e15da0c0f39ac65650eb650808e\n"
    		+ "b1ef335d100b98906b4fec4cf8e8c347\n"
    		+ "b6c3555f0d8f4d6ff0aa973897e3fe43\n"
    		+ "4bfbcea6c3122331ae5e0d2d8e75f069\n"
    		+ "a6e4c58a47c9c78ada52ad4bc3e2d350\n"
    		+ "76d20eacd3d1109e9cf96527d927de23\n"
    		+ "03804413a39194ee10d7c21ca7260c67\n"
    		+ "4a37848370fbe46189f71766ea1db6b0\n"
    		+ "25602a9d89015a280fcc8b26b268269e\n"
    		+ "719e5339c583136cf6032fc65992498b\n"
    		+ "a30b02d650782ce479d43fc2bef3eafc\n"
    		+ "9446daf58fbea6d840b0edcab7c23cdf\n"
    		+ "4338dbf00d62cd0448fcdd368716051a\n"
    		+ "e01ba87b9019b7ee8a14f267f22fa8be\n"
    		+ "e2556e9ee0722dc7bb2b95a873ecc196\n"
    		+ "d7526a1093cb15ad470c83bfe2c591c6\n"
    		+ "c799506cca1a03739d9b667cd41ed339\n"
    		+ "9a137c7551ecf034acb1c2ff5635368a\n"
    		+ "13238dd3b56a05d557fcc1bc53e46737\n"
    		+ "63e2389cbdc080f7e2edff70ab87a830\n"
    		+ "f4eb30f17a56f492a8b893c0d072be50\n"
    		+ "f5b854bbf6b90621aa7f6f232dbdc0ec\n"
    		+ "c5e9221b9e259bf114bcd88463caceac\n"
    		+ "e0180aff26eeb33bcc23f665769f4c52\n"
    		+ "d178a74cdb299e4d60632d917a3c7b17\n"
    		+ "205131af8fc679e84d012d99e6288e20\n"
    		+ "54ab9c466220d7f6c33888164da7ecc3\n"
    		+ "2c29dbd45613d2d39e0eca258f3f4382\n"
    		+ "3be8e56baf5400f1cd20eb2ae0d9450b\n"
    		+ "8c1b78662b5d98a02cd65e75a49c5b25\n"
    		+ "46db04bbd97792abbe58c5c16fba80ff\n"
    		+ "d9");

  //@formatter:on    

	@Test
	public void testGetEncoded1() throws IOException {
		DerTlv derTlv = new DerTlv((byte) 0x01, Hex.decode("aabbccddeeff010203"));
		assertEquals("0109aabbccddeeff010203", Hex.toHexString(derTlv.getEncoded()));
	}

	@Test
	public void testGetEncoded2() throws IOException {
		byte[] value = Arrays.copyOfRange(der_rawBytes, 4, der_rawBytes.length);
		DerTlv derTlv = new DerTlv((byte) 0x01, value);
		assertTrue(Arrays.areEqual(der_rawBytes, derTlv.getEncoded()));
	}

	@Test
	public void testfromByteArray1() throws IOException {
		DerTlv derTlv = DerTlv.fromByteArray(der_rawBytes);
		assertEquals(0x01, derTlv.getTag());
		assertEquals(0x037b, derTlv.getValue().length);
	}

	@Test
	public void testfromByteArray2() throws IOException {
		DerTlv derTlv = DerTlv.fromByteArray(Hex.decode("0809aabbccddeeff010203"));
		assertEquals(0x08, derTlv.getTag());
		assertEquals(0x09, derTlv.getValue().length);
	}

}