package de.tsenger.vdstools


import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.io.FileInputStream
import java.security.KeyStore
import java.security.Security
import java.time.LocalDateTime

class DataEncoderTest {
    //	@Test
    //	public void testEncodeDate_Now() {
    //		LocalDate ldNow = LocalDate.now();
    //		System.out.println("LocalDate.now(): " + ldNow);
    //		byte[] encodedDate = DataEncoder.encodeDate(ldNow);
    //		System.out.println("encodedDate: " + Hex.toHexString(encodedDate));
    //		assertEquals(ldNow, DataParser.decodeDate(encodedDate));
    //	}

    @Test
    fun testEncodeDateString() {
        val encodedDate = DataEncoder.encodeDate("1979-10-09")
        println("encodedDate: " + Hex.toHexString(encodedDate))
        Assert.assertEquals("99fdcb", Hex.toHexString(encodedDate))
    }

    @Test
    fun testEncodeMaskedDate1() {
        val encodedDate = DataEncoder.encodeMaskedDate("19xx-xx-01")
        Assert.assertEquals("c3002e7c", Hex.toHexString(encodedDate))
    }

    @Test
    fun testEncodeMaskedDate2() {
        val encodedDate = DataEncoder.encodeMaskedDate("201x-04-XX")
        Assert.assertEquals("313d10da", Hex.toHexString(encodedDate))
    }

    @Test
    fun testEncodeMaskedDate3() {
        val encodedDate = DataEncoder.encodeMaskedDate("1900-xx-xx")
        Assert.assertEquals("f000076c", Hex.toHexString(encodedDate))
    }

    @Test
    fun testEncodeMaskedDate4() {
        val encodedDate = DataEncoder.encodeMaskedDate("1999-12-31")
        Assert.assertEquals("00bbddbf", Hex.toHexString(encodedDate))
    }

    @Test
    fun testEncodeMaskedDate5() {
        val encodedDate = DataEncoder.encodeMaskedDate("xxxx-xx-xx")
        Assert.assertEquals("ff000000", Hex.toHexString(encodedDate))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testEncodeMaskedDate6_invalidFormat() {
        val encodedDate = DataEncoder.encodeMaskedDate("19-03-2010")
        Assert.assertEquals("ff000000", Hex.toHexString(encodedDate))
    }

    @Test
    fun testEncodeDateTime1() {
        val dateTime = LocalDateTime.parse("1957-03-25T08:15:22")
        val dateTimeBytes = DataEncoder.encodeDateTime(dateTime)
        Assert.assertEquals("02f527bf25b2", Hex.toHexString(dateTimeBytes))
    }

    @Test
    fun testEncodeDateTime2() {
        val dateTime = LocalDateTime.parse("2030-12-01T00:00:00")
        val dateTimeBytes = DataEncoder.encodeDateTime(dateTime)
        Assert.assertEquals("0aecc4c7fb80", Hex.toHexString(dateTimeBytes))
    }

    @Test
    fun testEncodeDateTime3() {
        val dateTime = LocalDateTime.parse("0001-01-01T00:00:00")
        val dateTimeBytes = DataEncoder.encodeDateTime(dateTime)
        Assert.assertEquals("00eb28c03640", Hex.toHexString(dateTimeBytes))
    }

    @Test
    fun testEncodeDateTime4() {
        val dateTime = LocalDateTime.parse("9999-12-31T23:59:59")
        val dateTimeBytes = DataEncoder.encodeDateTime(dateTime)
        Assert.assertEquals("0b34792d9777", Hex.toHexString(dateTimeBytes))
    }

    @Test
    fun testRegex() {
        val dateString = "1979-10-09"
        val formattedDate = dateString.replace("(.{4})-(.{2})-(.{2})".toRegex(), "$2$3$1")
        Assert.assertEquals("10091979", formattedDate)
    }

    @Test
    fun testEncodeDate_String() {
        val encodedDate = DataEncoder.encodeDate("2024-09-27")
        println("encodedDate: " + Hex.toHexString(encodedDate))

        Assert.assertEquals("8d7ad8", Hex.toHexString(encodedDate))
    }

    //	@Test
    //	public void testGetSignerCertRef() throws InvalidNameException, KeyStoreException {
    //		X509Certificate cert = (X509Certificate) keystore.getCertificate("dets32");
    //		String signerCertRef[] = DataEncoder.getSignerCertRef(cert);
    //
    //		assertEquals("DETS", signerCertRef[0]);
    //		assertEquals("32", signerCertRef[1]);
    //	}

    @Test
    fun testZip() {
        val bytesToCompress = Hex.decode(
            "61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7"
        )
        val compressedBytes = DataEncoder.zip(bytesToCompress)
        println("Compressed: " + Hex.toHexString(compressedBytes))
        Assert.assertEquals(
            "78da014e00b1ff61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c743d4280b",
            Hex.toHexString(compressedBytes)
        )
    }
    //	@Test
    //	public void testGetCertificateReference() throws KeyStoreException {
    //		X509Certificate cert = (X509Certificate) keystore.getCertificate("dets32");
    //		byte[] certRef = DataEncoder.buildCertificateReference(cert);
    //		System.out.println(Hex.toHexString(certRef));
    //		assertEquals("998b56e575", Hex.toHexString(certRef));
    //	}

    companion object {
        var keyStorePassword: String = "vdstools"
        var keyStoreFile: String = "src/test/resources/vdstools_testcerts.bks"
        var keystore: KeyStore? = null

        @JvmStatic
        @BeforeClass
        fun loadKeyStore() {
            Security.addProvider(BouncyCastleProvider())
            keystore = KeyStore.getInstance("BKS", "BC")
            val fis = FileInputStream(keyStoreFile)
            keystore?.load(fis, keyStorePassword.toCharArray())
            fis.close()
        }
    }
}
