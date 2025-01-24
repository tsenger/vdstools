package de.tsenger.vdstools.idb

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.Arrays
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.io.FileInputStream
import java.io.IOException
import java.security.*
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

class IdbSignerCertificateJvmTest {
    var encodedIdbSignerCertificate: ByteArray = Hex
        .decode(
            ("7e8201bc308201b83082013ea00302010202015b300a06082a8648ce3d040302303e"
                    + "310b30090603550406130255543110300e060355040a13077473656e6765723110300e060355040b13077365616c67656e310b3009"
                    + "060355040313025453301e170d3230303631303037313530305a170d3330303631303037313530305a303e310b3009060355040613"
                    + "0255543110300e060355040a13077473656e6765723110300e060355040b13077365616c67656e310b300906035504031302545330"
                    + "5a301406072a8648ce3d020106092b24030302080101070342000408132a7243b3ccc29c271097081c96a729eefb8eb93630e53649"
                    + "8e9b7ce1ced25d68a789d93bef39c04715c5ad3915d281c0754ecc08508bf66687efc630df88a32c302a30090603551d1304023000"
                    + "301d0603551d0e04160414adc6bafc76d49aa2d92fface93d71033832c6e96300a06082a8648ce3d040302036800306502310087f8"
                    + "5c8aa332659ed7ec30b8b61653353158f5ee6841c45c3b98fd1f14f0366203c934136c7444398f7fed359300203402307a95090526"
                    + "35c0faceeb83b00ad56d345a48e9af9b7e27c1301b5c47c347a91e464223551174dfba9f85beda2350f452")
        )

    @Test
    @Throws(KeyStoreException::class)
    fun testConstructor() {
        val signCert =
            IdbSignerCertificate((keystore.getCertificate("dets32") as X509Certificate).encoded)
        Assert.assertNotNull(signCert)
    }

    //	@Test(expected = IllegalArgumentException.class)
    //	public void testConstructor_null() throws KeyStoreException {
    //		new IdbSignerCertificate(null);
    //	}
    @Test
    @Throws(CertificateException::class, IOException::class)
    fun testFromByteArray() {
        val signCert = IdbSignerCertificate.fromByteArray(encodedIdbSignerCertificate)
        Assert.assertNotNull(signCert)
    }

    @Test
    @Throws(
        KeyStoreException::class,
        CertificateEncodingException::class,
        IOException::class
    )
    fun testGetEncoded() {
        val signCert =
            IdbSignerCertificate((keystore.getCertificate("utts5b") as X509Certificate).encoded)
        Assert.assertTrue(Arrays.areEqual(encodedIdbSignerCertificate, signCert.encoded))
        //		System.out.println(Hex.toHexString(signCert.getEncoded()));
    }

    @Test
    @Throws(CertificateException::class, IOException::class)
    fun testGetX509Certificate() {
        val signCert = IdbSignerCertificate.fromByteArray(encodedIdbSignerCertificate)
        Assert.assertNotNull(signCert.certBytes)
    }

    companion object {
        var keyStorePassword: String = "vdstools"
        var keyStoreFile: String = "src/commonTest/resources/vdstools_testcerts.bks"
        lateinit var keystore: KeyStore

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
            keystore.load(fis, keyStorePassword.toCharArray())
            fis.close()
        }
    }
}
