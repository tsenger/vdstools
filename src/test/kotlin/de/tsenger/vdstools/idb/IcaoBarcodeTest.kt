package de.tsenger.vdstools.idb


import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import vdstools.idb.IcaoBarcode
import vdstools.idb.IdbHeader
import vdstools.idb.IdbPayload
import java.io.FileInputStream
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import kotlin.test.fail

class IcaoBarcodeTest {
    @Test
    fun testIsNotSignedIsNotZipped() {
        val icb = IcaoBarcode('A', IdbPayload(IdbHeader("UTO"), null, null, null))
        Assert.assertFalse(icb.isSigned)
        Assert.assertFalse(icb.isZipped)
    }

    @Test
    fun testIsSignedIsNotZipped() {
        val icb = IcaoBarcode('B', IdbPayload(IdbHeader("UTO"), null, null, null))
        Assert.assertTrue(icb.isSigned)
        Assert.assertFalse(icb.isZipped)
    }

    @Test
    fun testIsNotSignedIsZipped() {
        val icb = IcaoBarcode('C', IdbPayload(IdbHeader("UTO"), null, null, null))
        Assert.assertFalse(icb.isSigned)
        Assert.assertTrue(icb.isZipped)
    }

    @Test
    fun testIsSignedIsZipped() {
        val icb = IcaoBarcode('D', IdbPayload(IdbHeader("UTO"), null, null, null))
        Assert.assertTrue(icb.isSigned)
        Assert.assertTrue(icb.isZipped)
    }

    @Test
    @Throws(CertificateException::class, IOException::class)
    fun testConstructor_signed_zipped() {
        val payload = IdbPayload.fromByteArray(
            Hex.decode(
                "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbb"
                        + "b332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7"
            ),
            true
        )
        val icb = IcaoBarcode(true, true, payload)
        println(icb.encoded)
        Assert.assertEquals(
            "NDB1DPDNACWQAUX7WVPABAUCAGAQBACNV3CDBCICBBMFRWKZ3JNNWW64LTOV3XS635P37HASLXOZTF5LCVFHUQ7NWEO4NWVOEUZNZZ5JSVFMYIOTKGTQRP5LDIOUU2XQYP4UCMKKD3BCXTL2G2REAJT3DFD5FEPDSP7ZKYE",
            icb.encoded
        )
    }

    @Test
    @Throws(CertificateException::class, IOException::class)
    fun testConstructor_signed_notZipped() {
        val payload = IdbPayload.fromByteArray(
            Hex.decode(
                "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbb"
                        + "b332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7"
            ),
            true
        )
        val icb = IcaoBarcode(true, false, payload)
        println(icb.encoded)
        Assert.assertEquals(
            "NDB1BNK6ACBIEAMBACAE3LWEGCEQECCYLDMVTWS23NN5YXG5LXPF5X27X6OBEXO5TGL2WFKKPJB63MI5Y3NK4JJS3TT2TFKKZQQ5GUNHBC72WGQ5JJVPBQ7ZIEYUUHWCFPGXUNVCIATHWGKH2KI6H",
            icb.encoded
        )
    }

    @Test
    @Throws(CertificateException::class, IOException::class)
    fun testConstructor_notSigned_zipped() {
        val payload = IdbPayload.fromByteArray(
            Hex.decode("6abc61120510b0b1b2b3b4b5b6b7b8b9babbbcbdbebf"),
            false
        )
        val icb = IcaoBarcode(false, true, payload)
        println(icb.encoded)
        Assert.assertEquals("NDB1CPDNACFQA5H7WVPDBCICRBMFRWKZ3JNNWW64LTOV3XS635P4DDIGSO", icb.encoded)
    }

    @Test
    @Throws(CertificateException::class, IOException::class)
    fun testConstructor_notSigned_notZipped() {
        val payload = IdbPayload.fromByteArray(
            Hex.decode("6abc61120510b0b1b2b3b4b5b6b7b8b9babbbcbdbebf"),
            false
        )
        val icb = IcaoBarcode(false, false, payload)
        println(icb.encoded)
        Assert.assertEquals("NDB1ANK6GCEQFCCYLDMVTWS23NN5YXG5LXPF5X27Q", icb.encoded)
    }

    @Test
    @Throws(CertificateException::class, IOException::class)
    fun testGetEncoded() {
        val payload = IdbPayload.fromByteArray(
            Hex.decode(
                "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbb"
                        + "b332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7"
            ),
            true
        )
        val icb = IcaoBarcode('D', payload)
        println(icb.encoded)
        Assert.assertEquals(
            "NDB1DPDNACWQAUX7WVPABAUCAGAQBACNV3CDBCICBBMFRWKZ3JNNWW64LTOV3XS635P37HASLXOZTF5LCVFHUQ7NWEO4NWVOEUZNZZ5JSVFMYIOTKGTQRP5LDIOUU2XQYP4UCMKKD3BCXTL2G2REAJT3DFD5FEPDSP7ZKYE",
            icb.encoded
        )
    }

    @Test
    @Throws(CertificateException::class, IOException::class)
    fun testFromString_signed_zipped() {
        val result = IcaoBarcode.fromString(
            "NDB1DPDNACWQAUX7WVPABAUCAGAQBACNV3CDBCICBBMFRWKZ3JNNWW64LTOV3XS635P37HASLXOZTF5LCVFHUQ7NWEO4NWVOEUZNZZ5JSVFMYIOTKGTQRP5LDIOUU2XQYP4UCMKKD3BCXTL2G2REAJT3DFD5FEPDSP7ZKYE"
        )
        result.onSuccess {
            Assert.assertEquals(
                "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7",
                Hex.toHexString(it.payLoad.encoded)
            )
        }
        result.onFailure { fail() }

        //		System.out.println(Hex.toHexString(barcode.getPayLoad().getEncoded()));
    }

    @Test
    @Throws(CertificateException::class, IOException::class)
    fun testFromString_signed_notZipped() {
        val result = IcaoBarcode.fromString(
            "NDB1BNK6ACBIEAMBACAE3LWEGCEQECCYLDMVTWS23NN5YXG5LXPF5X27X6OBEXO5TGL2WFKKPJB63MI5Y3NK4JJS3TT2TFKKZQQ5GUNHBC72WGQ5JJVPBQ7ZIEYUUHWCFPGXUNVCIATHWGKH2KI6H"
        )
        result.onSuccess {
            Assert.assertEquals(
                "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7",
                Hex.toHexString(it.payLoad.encoded)
            )
        }
        result.onFailure { fail() }
        //		System.out.println(Hex.toHexString(barcode.getPayLoad().getEncoded()));
    }

    @Test
    @Throws(CertificateException::class, IOException::class)
    fun testFromString_notSigned_zipped() {
        val result = IcaoBarcode.fromString("NDB1CPDNACFQA5H7WVPDBCICRBMFRWKZ3JNNWW64LTOV3XS635P4DDIGSO")
        result.onSuccess {
            Assert.assertEquals(
                "6abc61120510b0b1b2b3b4b5b6b7b8b9babbbcbdbebf",
                Hex.toHexString(it.payLoad.encoded)
            )
        }
        result.onFailure { fail() }
        //		System.out.println(Hex.toHexString(barcode.getPayLoad().getEncoded()));
    }

    @Test
    @Throws(CertificateException::class, IOException::class)
    fun testFromString_notSigned_notZipped() {
        val result = IcaoBarcode.fromString("NDB1ANK6GCEQFCCYLDMVTWS23NN5YXG5LXPF5X27Q")
        result.onSuccess {
            Assert.assertEquals(
                "6abc61120510b0b1b2b3b4b5b6b7b8b9babbbcbdbebf",
                Hex.toHexString(it.payLoad.encoded)
            )
        }
        result.onFailure { fail() }
        //		System.out.println(Hex.toHexString(barcode.getPayLoad().getEncoded()));
    }

    @Test
    fun testFromString_invalid_BarcodeIdentifier() {
        val result = IcaoBarcode.fromString("ADB1ANK6GCEQFCCYLDMVTWS23NN5YXG5LXPF5X27Q")
        result.onSuccess { fail() }
    }

    companion object {
        var keyStorePassword: String = "vdstools"
        var keyStoreFile: String = "src/test/resources/vdstools_testcerts.bks"
        var keystore: KeyStore? = null

        @JvmStatic
        @BeforeClass
        @Throws(
            NoSuchAlgorithmException::class,
            CertificateException::class,
            IOException::class,
            KeyStoreException::class,
            NoSuchProviderException::class
        )
        fun loadKeyStore() {
            Security.addProvider(BouncyCastleProvider())
            keystore = KeyStore.getInstance("BKS", "BC")
            val fis = FileInputStream(keyStoreFile)
            keystore?.load(fis, keyStorePassword.toCharArray())
            fis.close()
        }
    }
}
