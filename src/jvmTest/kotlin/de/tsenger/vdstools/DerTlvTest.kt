package de.tsenger.vdstools

import de.tsenger.vdstools.asn1.DerTlv
import org.bouncycastle.util.Arrays
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test
import java.io.IOException

class DerTlvTest {
    //@formatter:on    
    @Test
    @Throws(IOException::class)
    fun testGetEncoded1() {
        val derTlv = DerTlv(0x01.toByte(), Hex.decode("aabbccddeeff010203"))
        Assert.assertEquals("0109aabbccddeeff010203", Hex.toHexString(derTlv.encoded))
    }

    @Test
    @Throws(IOException::class)
    fun testGetEncoded2() {
        val value = Arrays.copyOfRange(der_rawBytes, 4, der_rawBytes.size)
        val derTlv = DerTlv(0x01.toByte(), value)
        Assert.assertTrue(Arrays.areEqual(der_rawBytes, derTlv.encoded))
    }

    @Test
    @Throws(IOException::class)
    fun testfromByteArray1() {
        val derTlv = DerTlv.fromByteArray(der_rawBytes)
        Assert.assertEquals(0x01, derTlv!!.tag.toLong())
        Assert.assertEquals(0x037b, derTlv.value.size.toLong())
    }

    @Test
    @Throws(IOException::class)
    fun testfromByteArray2() {
        val derTlv = DerTlv.fromByteArray(Hex.decode("0809aabbccddeeff010203"))
        Assert.assertEquals(0x08, derTlv!!.tag.toLong())
        Assert.assertEquals(0x09, derTlv.value.size.toLong())
    }

    companion object {
        //@formatter:off
        var der_rawBytes: ByteArray = Hex.decode(
        ("""
                 0182037bff4fff51002900000000
                 019d0000021300000000000000000000
                 019d0000021300000000000000000001
                 070101ff52000c000200010005040400
                 00ff5c002342772076f076f076c06f00
                 6f006ee0675067506768500550055047
                 57d357d35762ff640025000143726561
                 746564206279204f70656e4a50454720
                 76657273696f6e20322e352e30ff9000
                 0a0000000002f20001ff93c7d1a28014
                 a254717b603970487a48a192f03b40a4
                 17e10ec30c718545a1f116ee9443f7d8
                 9d6a946da5814118980326c118aecaef
                 2bec5c0c348e3dd4ecd02661eaeaf74b
                 ff037c2b4a29bb81b06da6f0b1f09311
                 bf98a370e2407d6b7784e2436377f14a
                 8db4a1de53ac0296e78052918d2f789f
                 fe046068a7d735f06c2d5cb1d3aaa45b
                 5ec1cc05a2f210f200c1f155a1f23283
                 e1a25e527c1053d089bc3c3f43aca8b3
                 79fb80f0a5713bbf97d23113b99de696
                 1929d50f0735f79e56f0296f56bf5059
                 fae888e7c37a5ad7f2b0800e0ab82287
                 cbbab5e66cb6099416cebab039b6b0dd
                 c2e8ae31de209feaa25210d1f1229d63
                 a2a85e15da0c0f39ac65650eb650808e
                 b1ef335d100b98906b4fec4cf8e8c347
                 b6c3555f0d8f4d6ff0aa973897e3fe43
                 4bfbcea6c3122331ae5e0d2d8e75f069
                 a6e4c58a47c9c78ada52ad4bc3e2d350
                 76d20eacd3d1109e9cf96527d927de23
                 03804413a39194ee10d7c21ca7260c67
                 4a37848370fbe46189f71766ea1db6b0
                 25602a9d89015a280fcc8b26b268269e
                 719e5339c583136cf6032fc65992498b
                 a30b02d650782ce479d43fc2bef3eafc
                 9446daf58fbea6d840b0edcab7c23cdf
                 4338dbf00d62cd0448fcdd368716051a
                 e01ba87b9019b7ee8a14f267f22fa8be
                 e2556e9ee0722dc7bb2b95a873ecc196
                 d7526a1093cb15ad470c83bfe2c591c6
                 c799506cca1a03739d9b667cd41ed339
                 9a137c7551ecf034acb1c2ff5635368a
                 13238dd3b56a05d557fcc1bc53e46737
                 63e2389cbdc080f7e2edff70ab87a830
                 f4eb30f17a56f492a8b893c0d072be50
                 f5b854bbf6b90621aa7f6f232dbdc0ec
                 c5e9221b9e259bf114bcd88463caceac
                 e0180aff26eeb33bcc23f665769f4c52
                 d178a74cdb299e4d60632d917a3c7b17
                 205131af8fc679e84d012d99e6288e20
                 54ab9c466220d7f6c33888164da7ecc3
                 2c29dbd45613d2d39e0eca258f3f4382
                 3be8e56baf5400f1cd20eb2ae0d9450b
                 8c1b78662b5d98a02cd65e75a49c5b25
                 46db04bbd97792abbe58c5c16fba80ff
                 d9
                 """.trimIndent()))
 }}