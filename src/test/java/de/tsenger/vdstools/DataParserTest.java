package de.tsenger.vdstools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import de.tsenger.vdstools.vds.Feature;
import de.tsenger.vdstools.vds.VdsHeader;
import de.tsenger.vdstools.vds.seals.DigitalSeal;

public class DataParserTest {

	//@formatter:off
    static byte[] residentPermit_rawBytes = Hex.decode(
            "DC03D9C5D9CAC8A73A990F71346ECF47\n"
            + "FB0602305CBA135875976EC066D417B5\n" 
            + "9E8C6ABC133C133C133C133C3FEF3A29\n"
            + "38EE43F1593D1AE52DBB26751FE64B7C\n" 
            + "133C136B0306D79519A65306FF408B7F\n"
            + "3B5F9A83FDD4F46EC7DCCC3384BB6C54\n" 
            + "0AAF52603CC66D1F08B7F5E71243475D\n"
            + "0A833B51FD2A846622E847B1F3791803\n" 
            + "F26D734B9BD18178FA22CFF2A31A");

    static byte[] addressStickerId_rawBytes = Hex.decode(
            "DC03D9C56D32C8A72CB16ECF476ECF47\n"
            + "F9080106CF3519AF974C02061A702085\n" 
            + "19A1030E395E463E740C749FAD19D31E\n"
            + "FE32FF388F16D14E828A86E0F8E31DDF\n" 
            + "13CC2CB3E0D8F5E4706562C9503D7326\n"
            + "AD6C0FA84154607D70975E5B7DFB2E36\n" 
            + "197988ECE64345D37AD9B97C");

    static byte[] socialInsurance_rawBytes = Hex.decode(
            "DC02D9C56D32C8A519FC0F71346F1D67\n" 
            + "FC0401083FEE456D2DE019A8020B5065\n"
            + "72736368776569C39F03054F73636172\n" 
            + "04134AC3A2636F62C3A96E6964696374\n"
            + "7572697573FF401DCE81E863B01CFFE5\n" 
            + "B099A5BBFCA60730EC9E090A1C82FA00\n"
            + "580EB592A9FC921D5F02CE8D1EC4E3AA\n" 
            + "3CB4CEA3AFEF1C382B44ED8DA7105372\n" 
            + "FC1D2E8D91A393");

    static byte[] arrivalAttestationV02_rawBytes = Hex.decode(
            "DC02D9C56D32C8A51A540F71346F1D67\n"
            + "FD020230A56213535BD4CAECC87CA4CC\n" 
            + "AEB4133C133C133C133C133C3FEF3A29\n"
            + "38EE43F1593D1AE52DBB26751FE64B7C\n" 
            + "133C136B030859E9203833736D24FF40\n"
            + "77B2FEC8EF9EF10C0D38A7D2A579EBB9\n" 
            + "F80212EB06EDD7B1DC29889A6B735B7E\n"
            + "A1D7D78FF60D2AECB87B0247628C3211\n" 
            + "9BA335B6BD87A7E07333C83ED16B091F");

    static byte[] arrivalAttestation_rawBytes = Hex.decode(
            "dc026abc6d32c8a519fc0f71341145f4" +
                "fd020230a5621353d9a275735bd4134b" +
                "c549133c133c133c133c133ca32519a5" +
                "19a4344a5e681ae7204b20d532cf4b7c" +
                "133c133f030820d5201019a51aeaff40" +
                "4a1f218ca4392647ecff6c8abf9e796a" +
                "78eebe0b1ac8cc25c4ee17eed961d118" +
                "9091358d7d616f1a517abc747f6c4490" +
                "ff159d4dcf50248b00b1e32e9e7805e7");


    static byte[] visa_224bitSig_rawBytes = Hex.decode(
            "DC03D9C56D32C8A72CB10F71347D0017\n" 
            + "5D01022CDD52134A74DA1347C6FED95C\n"
            + "B89F9FCE133C133C133C133C20383373\n" 
            + "4AAF47F0C32F1A1E20EB2625393AFE31\n"
            + "0403A00000050633BE1FED20C6FF389F\n" 
            + "D029C66FB2E4BF361CDBFFD8F5931B62\n"
            + "59F645B077702C617F453D0B898A55E6\n" 
            + "E7870974FFE7B3AC416ACDE6B03B3C3A\n" 
            + "8CB5A22B456816");
    
    static byte[] supplementSheet_rawBytes = Hex.decode(
            "DC03D9C5D9CAC8A73A990F71347D4E37\n"
            + "FA0604305CBA135875976EC066D417B5\n"
            + "9E8C6ABC133C133C133C133C3FEF3A29\n"
            + "38EE43F1593D1AE52DBB26751FE64B7C\n"
            + "133C136B0506B77519A519AAFF4008F9\n"
            + "E9B4B79BE5703048A4879A4F420C433C\n"
            + "375295A355FB0D29DCBED211CF6F5F57\n"
            + "38BA2B74E2FE5F1D2D2021E054BFFD0E\n"
            + "4CE17D98E5BCED26A85C91C68B2F");
    
    static byte[] addressStickerPassport_rawBytes = Hex.decode(
            "DC03D9C5D9CAC8A73A990F71347D4E37\n"
            + "F80A0106B77A38E596CE02061A203A4D\n"
            + "1FE1030426532081FF4027436CE719F9\n"
            + "13CCD3EBFAEEAE175171450DB6CA1B62\n"
            + "FF188748834D2DC5299A5F418BE8D4DC\n"
            + "052E0536CB6DE711B4CC645651C6B0EA\n"
            + "FE5713E96290DC149169");
    
    static byte[] emergenyTravelDoc_rawBytes = Hex.decode(
            "DC03D9C5D9CAC8A73A990F71347D4E37\n"
            + "5E0302308A0D62B9D917A4CCA93CA4D0\n"
            + "EDFC133C133C133C133C133C3FEF3A29\n"
            + "38EE43F1593D1AE52DBB26751FE64B7C\n"
            + "133C136BFF4022F8BD19ECCBA4EF24F2\n"
            + "04787796DD914FEC61F605B153B22A6E\n"
            + "F307D3869938A4E7E908F0A63B837988\n"
            + "0B395C7FDBAC720D7F2836D08E1DA626\n"
            + "11614A00120B");
    
    static byte[] tempPassport_rawBytes = Hex.decode(
    		"dc03d9c5d9cac8a73a99a807b88bcd28\n"
    		+ "f60d0182037bff4fff51002900000000\n"
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
    		+ "d9023cb9da1353d9a275735bd4134bc5\n"
    		+ "4957fc133c133c133c133c133c133c13\n"
    		+ "4719a519a519a56abc4c1d4bcf3eff45\n"
    		+ "8d2c93133c133c133c133c133cfe39ff\n"
    		+ "400b2460883f3126b614cb67a26e2d28\n"
    		+ "086b7ace0267a1b8bea9e19b028d0360\n"
    		+ "aaa395edb1669e76a8f150a732a9386f\n"
    		+ "535605c34092b79ae3fe9a7f87c3a4a7\n"
    		+ "a6");
    
    static byte[] tempPerso_rawBytes = Hex.decode(
    		"dc03d9c5d9cac8a73a99a807b88bcd28\n"
    		+ "f70b0182037bff4fff51002900000000\n"
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
    		+ "d902308eba1353d9a275735bd4134bc5\n"
    		+ "4957fc133c133c133c133c6ae519a519\n"
    		+ "a521ec14a81ae62714273d205e25fc13\n"
    		+ "3c133dff403551e0815c340245bedf29\n"
    		+ "b6454f197eee23e830c1e6311507f4f1\n"
    		+ "3135a368ee4c91d90f40a2adf5e2a521\n"
    		+ "a87e536d0f1c21f57eacc20bc8c6e818\n"
    		+ "cd3cdb0e38");

    static byte[] fictionCert_rawBytes = Hex.decode(
    		"dc03d9c5d9cac8a73a990f71378bcd28\n"
    		+ "f50c0182037bff4fff51002900000000\n"
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
    		+ "d90230abca1353d9a275735bd413499f\n"
    		+ "2db792c494133c133c133c9ce519a519\n"
    		+ "a54a0bc3a81ae62724273d205e2aaf3f\n"
    		+ "4e19ce0306e95545b819f6040820d520\n"
    		+ "1019a51aeaff409f321b3076160a2b1e\n"
    		+ "237da1f3a7646528af6449701c536561\n"
    		+ "8d870abe6a16ae35d4ac6c8446738b9d\n"
    		+ "00551581d5124cef956cf81e5ac32d37\n"
    		+ "69a60d687bb2cd");

  //@formatter:on    

	@Test
	public void testDecodeMaskedDate1() {
		String decodedDate = DataParser.decodeMaskedDate(Hex.decode("c3002e7c"));
		assertEquals("19xx-xx-01", decodedDate);
	}

	@Test
	public void testDecodeMaskedDate2() {
		String decodedDate = DataParser.decodeMaskedDate(Hex.decode("313d10da"));
		assertEquals("201x-04-xx", decodedDate);
	}

	@Test
	public void testDecodeMaskedDate3() {
		String decodedDate = DataParser.decodeMaskedDate(Hex.decode("f000076c"));
		assertEquals("1900-xx-xx", decodedDate);
	}

	@Test
	public void testDecodeMaskedDate4() {
		String decodedDate = DataParser.decodeMaskedDate(Hex.decode("00bbddbf"));
		assertEquals("1999-12-31", decodedDate);
	}

	@Test
	public void testDecodeMaskedDate5() {
		String decodedDate = DataParser.decodeMaskedDate(Hex.decode("ff000000"));
		assertEquals("xxxx-xx-xx", decodedDate);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDecodeMaskedDate6_invalidFormat() {
		String decodedDate = DataParser.decodeMaskedDate(Hex.decode("ff0000"));
		assertNull(decodedDate);
	}

	@Test
	public void testDecodeDateTime1() {
		LocalDateTime localDateTime = DataParser.decodeDateTime(Hex.decode("0aecc4c7fb80"));
		System.out.println(localDateTime);
		assertTrue(LocalDateTime.parse("2030-12-01T00:00:00").isEqual(localDateTime));
	}

	@Test
	public void testDecodeDateTime2() {
		LocalDateTime localDateTime = DataParser.decodeDateTime(Hex.decode("02f527bf25b2"));
		System.out.println(localDateTime);
		assertTrue(LocalDateTime.parse("1957-03-25T08:15:22").isEqual(localDateTime));
	}

	@Test
	public void testDecodeDateTime3() {
		LocalDateTime localDateTime = DataParser.decodeDateTime(Hex.decode("00eb28c03640"));
		System.out.println(localDateTime);
		assertTrue(LocalDateTime.parse("0001-01-01T00:00:00").isEqual(localDateTime));
	}

	@Test
	public void testDecodeDateTime4() {
		LocalDateTime localDateTime = DataParser.decodeDateTime(Hex.decode("0b34792d9777"));
		System.out.println(localDateTime);
		assertTrue(LocalDateTime.parse("9999-12-31T23:59:59").isEqual(localDateTime));
	}

	@Test
	public void testDecodeHeader() {
		ByteBuffer bb = ByteBuffer.wrap(residentPermit_rawBytes);
		VdsHeader vdsHeader = DataParser.decodeHeader(bb);
		assertEquals(0x03, vdsHeader.rawVersion);
		assertEquals("UTO", vdsHeader.issuingCountry);
		assertEquals("UTTS", vdsHeader.signerIdentifier);
		assertEquals("5B", vdsHeader.certificateReference);
		assertEquals(LocalDate.parse("2020-01-01"), vdsHeader.issuingDate);
		assertEquals(LocalDate.parse("2023-07-26"), vdsHeader.sigDate);
		assertEquals(0xfb, vdsHeader.docFeatureRef & 0xff);
		assertEquals(0x06, vdsHeader.docTypeCat & 0xff);
	}

	@Test
	public void testParseSocialInsurranceCard() throws IOException {
		DigitalSeal seal = DataParser.parseVdsSeal(socialInsurance_rawBytes);
		assertEquals("65170839J003", seal.getFeature(Feature.SOCIAL_INSURANCE_NUMBER));
		assertEquals("Perschweiß", seal.getFeature(Feature.SURNAME));
		assertEquals("Oscar", seal.getFeature(Feature.FIRST_NAME));
		assertEquals("Jâcobénidicturius", seal.getFeature(Feature.BIRTH_NAME));
	}

	@Test
	public void testParseArrivalAttestationV02() throws IOException {
		DigitalSeal seal = DataParser.parseVdsSeal(arrivalAttestationV02_rawBytes);
		assertEquals("MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06",
				seal.getFeature(Feature.MRZ));
		assertEquals("ABC123456DEF", seal.getFeature(Feature.AZR));
		assertEquals(null, seal.getFeature(Feature.FIRST_NAME));
	}

	@Test
	public void testParseResidentPermit() throws IOException {
		DigitalSeal seal = DataParser.parseVdsSeal(residentPermit_rawBytes);
		assertEquals("ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06",
				seal.getFeature(Feature.MRZ));
		assertEquals("UFO001979", seal.getFeature(Feature.PASSPORT_NUMBER));
	}

	@Test
	public void testParseSupplementSheet() throws IOException {
		DigitalSeal seal = DataParser.parseVdsSeal(supplementSheet_rawBytes);
		assertEquals("ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06",
				seal.getFeature(Feature.MRZ));
		assertEquals("PA0000005", seal.getFeature(Feature.SHEET_NUMBER));
	}

	@Test
	public void testEmergencyTravelDoc() throws IOException {
		DigitalSeal seal = DataParser.parseVdsSeal(emergenyTravelDoc_rawBytes);
		assertEquals("I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06",
				seal.getFeature(Feature.MRZ));
	}

	@Test
	public void testParseAddressStickerId() throws IOException {
		DigitalSeal seal = DataParser.parseVdsSeal(addressStickerId_rawBytes);
		assertEquals("T2000AK47", seal.getFeature(Feature.DOCUMENT_NUMBER));
		assertEquals("05314000", seal.getFeature(Feature.AGS));
		assertEquals("53175HEINEMANNSTR11", seal.getFeature(Feature.ADDRESS));
	}

	@Test
	public void testParseAddressStickerPassport() throws IOException {
		DigitalSeal seal = DataParser.parseVdsSeal(addressStickerPassport_rawBytes);
		assertEquals("PA5500K11", seal.getFeature(Feature.DOCUMENT_NUMBER));
		assertEquals("03359010", seal.getFeature(Feature.AGS));
		assertEquals("21614", seal.getFeature(Feature.POSTAL_CODE));
	}

	@Test
	public void testParseVisa() throws IOException {
		DigitalSeal seal = DataParser.parseVdsSeal(visa_224bitSig_rawBytes);
		assertEquals("VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<1234567XY7GBR5203116M2005250",
				seal.getFeature(Feature.MRZ_MRVB));
		assertEquals("47110815P", seal.getFeature(Feature.PASSPORT_NUMBER));
		assertEquals("a00000", Hex.toHexString((byte[]) seal.getFeature(Feature.DURATION_OF_STAY)));
	}

	@Test
	public void testParseFictionCert() throws IOException {
		DigitalSeal seal = DataParser.parseVdsSeal(fictionCert_rawBytes);
		assertEquals("NFD<<MUSTERMANN<<CLEOPATRE<<<<<<<<<<L000000007TUR8308126F2701312T2611011",
				seal.getFeature(Feature.MRZ));
		assertEquals("X98723021", seal.getFeature(Feature.PASSPORT_NUMBER));
		assertEquals("160113000085", seal.getFeature(Feature.AZR));
	}

	@Test
	public void testParseTempPerso() throws IOException {
		DigitalSeal seal = DataParser.parseVdsSeal(tempPerso_rawBytes);
		assertEquals("ITD<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<D000000001D<<8308126<2701312<<<<<<<0",
				seal.getFeature(Feature.MRZ));
	}

	@Test
	public void testParseTempPassport() throws IOException {
		DigitalSeal seal = DataParser.parseVdsSeal(tempPassport_rawBytes);
		assertEquals("PPD<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<<<<<<<<<A000000000D<<8308126<2710316<<<<<<<<<<<<<<<8",
				seal.getFeature(Feature.MRZ));
	}

	@Test
	public void testFeatureMap() throws IOException {
		DigitalSeal seal = DataParser.parseVdsSeal(visa_224bitSig_rawBytes);
		seal.getFeatureMap()
				.forEach((key, value) -> System.out.println(String.format("Key: %s, Value: %s", key, value)));
	}

	@Test
	public void testGetEncodedBytes_rp() throws IOException {
		DigitalSeal seal = DataParser.parseVdsSeal(residentPermit_rawBytes);
		assertTrue(Arrays.areEqual(residentPermit_rawBytes, seal.getEncodedBytes()));
	}

	@Test
	public void testGetEncodedBytes_aa() throws IOException {
		DigitalSeal seal = DataParser.parseVdsSeal(arrivalAttestation_rawBytes);
		System.out.println(Hex.toHexString(arrivalAttestation_rawBytes));
		System.out.println(Hex.toHexString(seal.getEncodedBytes()));
		assertTrue(Arrays.areEqual(arrivalAttestation_rawBytes, seal.getEncodedBytes()));
	}

	@Test
	public void testGetEncodedBytes_aav2() throws IOException {
		DigitalSeal seal = DataParser.parseVdsSeal(arrivalAttestationV02_rawBytes);
		System.out.println(Hex.toHexString(arrivalAttestationV02_rawBytes));
		System.out.println(Hex.toHexString(seal.getEncodedBytes()));
		assertTrue(Arrays.areEqual(arrivalAttestationV02_rawBytes, seal.getEncodedBytes()));
	}

	@Test
	public void testGetEncodedBytes_fc() throws IOException {
		DigitalSeal seal = DataParser.parseVdsSeal(fictionCert_rawBytes);
		assertTrue(Arrays.areEqual(fictionCert_rawBytes, seal.getEncodedBytes()));
	}

	@Test
	public void testUnzip() throws IOException {
		byte[] compressedBytes = Hex.decode(
				"78da014e00b1ff61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c743d4280b");
		byte[] decompressedBytes = DataParser.unzip(compressedBytes);
		System.out.println("Decompressed: " + Hex.toHexString(decompressedBytes));
		assertEquals(
				"61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7",
				Hex.toHexString(decompressedBytes));
	}
}