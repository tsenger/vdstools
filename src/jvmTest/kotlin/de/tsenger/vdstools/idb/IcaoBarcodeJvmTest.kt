package de.tsenger.vdstools.idb


import de.tsenger.vdstools.DataEncoder.buildCertificateReference
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.vds.VdsMessage
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import java.io.FileInputStream
import java.io.IOException
import java.security.KeyStore
import java.security.Security
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

@OptIn(ExperimentalStdlibApi::class)
class IcaoBarcodeJvmTest {

    companion object {
        var keyStorePassword: String = "vdstools"
        var keyStoreFile: String = "src/commonTest/resources/vdstools_testcerts.bks"
        lateinit var keystore: KeyStore

        @JvmStatic
        @BeforeClass
        fun loadKeyStore() {
            Security.addProvider(BouncyCastleProvider())
            keystore = KeyStore.getInstance("BKS", "BC")
            val fis = FileInputStream(keyStoreFile)
            keystore.load(fis, keyStorePassword.toCharArray())
            fis.close()
        }
    }

    @Test
    fun testIsNotSignedIsNotZipped() {
        val icb = IcaoBarcode('A', IdbPayload(IdbHeader("UTO"), IdbMessageGroup(emptyList()), null, null))
        assertFalse(icb.isSigned)
        assertFalse(icb.isZipped)
    }

    @Test
    fun testIsSignedIsNotZipped() {
        val icb = IcaoBarcode('B', IdbPayload(IdbHeader("UTO"), IdbMessageGroup(emptyList()), null, null))
        assertTrue(icb.isSigned)
        assertFalse(icb.isZipped)
    }

    @Test
    fun testIsNotSignedIsZipped() {
        val icb = IcaoBarcode('C', IdbPayload(IdbHeader("UTO"), IdbMessageGroup(emptyList()), null, null))
        assertFalse(icb.isSigned)
        assertTrue(icb.isZipped)
    }

    @Test
    fun testIsSignedIsZipped() {
        val icb = IcaoBarcode('D', IdbPayload(IdbHeader("UTO"), IdbMessageGroup(emptyList()), null, null))
        assertTrue(icb.isSigned)
        assertTrue(icb.isZipped)
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
        println(icb.rawString)
        assertEquals(
            "NDB1DPDNACWQAUX7WVPABAUCAGAQBACNV3CDBCICBBMFRWKZ3JNNWW64LTOV3XS635P37HASLXOZTF5LCVFHUQ7NWEO4NWVOEUZNZZ5JSVFMYIOTKGTQRP5LDIOUU2XQYP4UCMKKD3BCXTL2G2REAJT3DFD5FEPDSP7ZKYE",
            icb.rawString
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
        println(icb.rawString)
        assertEquals(
            "NDB1BNK6ACBIEAMBACAE3LWEGCEQECCYLDMVTWS23NN5YXG5LXPF5X27X6OBEXO5TGL2WFKKPJB63MI5Y3NK4JJS3TT2TFKKZQQ5GUNHBC72WGQ5JJVPBQ7ZIEYUUHWCFPGXUNVCIATHWGKH2KI6H",
            icb.rawString
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
        println(icb.rawString)
        assertEquals("NDB1CPDNACFQA5H7WVPDBCICRBMFRWKZ3JNNWW64LTOV3XS635P4DDIGSO", icb.rawString)
    }

    @Test
    @Throws(CertificateException::class, IOException::class)
    fun testConstructor_notSigned_notZipped() {
        val payload = IdbPayload.fromByteArray(
            Hex.decode("6abc61120510b0b1b2b3b4b5b6b7b8b9babbbcbdbebf"),
            false
        )
        val icb = IcaoBarcode(false, false, payload)
        println(icb.rawString)
        assertEquals("NDB1ANK6GCEQFCCYLDMVTWS23NN5YXG5LXPF5X27Q", icb.rawString)
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
        println(icb.rawString)
        assertEquals(
            "NDB1DPDNACWQAUX7WVPABAUCAGAQBACNV3CDBCICBBMFRWKZ3JNNWW64LTOV3XS635P37HASLXOZTF5LCVFHUQ7NWEO4NWVOEUZNZZ5JSVFMYIOTKGTQRP5LDIOUU2XQYP4UCMKKD3BCXTL2G2REAJT3DFD5FEPDSP7ZKYE",
            icb.rawString
        )
    }


    @Test
    fun testFromString_signed_zipped() {
        val icb = IcaoBarcode.fromString(
            "NDB1DPDNACWQAUX7WVPABAUCAGAQBACNV3CDBCICBBMFRWKZ3JNNWW64LTOV3XS635P37HASLXOZTF5LCVFHUQ7NWEO4NWVOEUZNZZ5JSVFMYIOTKGTQRP5LDIOUU2XQYP4UCMKKD3BCXTL2G2REAJT3DFD5FEPDSP7ZKYE"
        ) as IcaoBarcode
        assertNotNull(icb)
        assertEquals(
            "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7",
            icb.payLoad.encoded.toHexString()
        )

    }

    @Test
    fun testFromString_signed_notZipped() {
        val icb = IcaoBarcode.fromString(
            "NDB1BNK6ACBIEAMBACAE3LWEGCEQECCYLDMVTWS23NN5YXG5LXPF5X27X6OBEXO5TGL2WFKKPJB63MI5Y3NK4JJS3TT2TFKKZQQ5GUNHBC72WGQ5JJVPBQ7ZIEYUUHWCFPGXUNVCIATHWGKH2KI6H"
        ) as IcaoBarcode
        assertNotNull(icb)
        assertEquals(
            "6abc010504030201009b5d8861120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7",
            icb.payLoad.encoded.toHexString()
        )
    }

    @Test

    fun testFromString_notSigned_zipped() {
        val icb = IcaoBarcode.fromString("NDB1CPDNACFQA5H7WVPDBCICRBMFRWKZ3JNNWW64LTOV3XS635P4DDIGSO") as IcaoBarcode
        assertNotNull(icb)
        assertEquals(
            "6abc61120510b0b1b2b3b4b5b6b7b8b9babbbcbdbebf",
            icb.payLoad.encoded.toHexString()
        )

    }

    @Test
    fun testFromString_notSigned_notZipped() {
        val icb = IcaoBarcode.fromString("NDB1ANK6GCEQFCCYLDMVTWS23NN5YXG5LXPF5X27Q") as IcaoBarcode
        assertNotNull(icb)
        assertEquals(
            "6abc61120510b0b1b2b3b4b5b6b7b8b9babbbcbdbebf",
            icb.payLoad.encoded.toHexString()
        )
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun testFromString_invalid_BarcodeIdentifier() {

        val icb = IcaoBarcode.fromString("ADB1ANK6GCEQFCCYLDMVTWS23NN5YXG5LXPF5X27Q")
        assertNull(icb)
    }

    @Test
    fun testBuildIdbSeal() {

        val signerCertRef = "utts5b"

        // Build Header
        val cert = keystore.getCertificate(signerCertRef) as X509Certificate
        val certRef = buildCertificateReference(cert.encoded)
        val header = IdbHeader(
            "D<<",
            IdbSignatureAlgorithm.SHA256_WITH_ECDSA,
            certRef,
            "2025-01-31"
        )

        // Build Emergency Travel Document VdsMessage
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06"
        val vdsMessage = VdsMessage.Builder("ICAO_EMERGENCY_TRAVEL_DOCUMENT")
            .addDocumentFeature("MRZ", mrz)
            .build()

        // Add ETD to an IdbMessageGroup
        val messageGroup = IdbMessageGroup.Builder().addMessage("EMERGENCY_TRAVEL_DOCUMENT", vdsMessage.encoded).build()

        // Generate Signature
        val ecPrivKey = keystore.getKey(signerCertRef, keyStorePassword.toCharArray()) as BCECPrivateKey
        val signer = Signer(ecPrivKey.encoded, "brainpoolP256r1")
        val signature = IdbSignature(signer.sign(header.encoded + messageGroup.encoded))

        val icb = IcaoBarcode('B', IdbPayload(header, messageGroup, null, signature))
        assertTrue(
            icb.rawString.startsWith("NDB1BNK6ADJL2PECXOAAUAUMWCNACGIBDAXF2CNMHLF3OYBTNIF5VT2GGVPATHQJTYEZ4CM6D73Z2FE4O4Q7RLE6RVZJNXMTHKH7G")
        )


    }


}
