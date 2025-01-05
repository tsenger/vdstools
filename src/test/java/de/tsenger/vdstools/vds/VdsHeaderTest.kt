package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.vds.VdsHeader.Companion.fromBuffer
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import okio.Buffer
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.Arrays
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.io.FileInputStream
import java.security.*


class VdsHeaderTest {
    @Test
    fun testGetDocumentRef() {
        val header = VdsHeader.Builder("ALIENS_LAW").build()
        Assert.assertEquals(header.documentRef.toLong(), 0x01fe)
    }

    @Test
    fun testGetEncoded_V3() {
        // RESIDENCE_PERMIT 0xfb06
        val header = VdsHeader.Builder("RESIDENCE_PERMIT")
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .setIssuingDate(LocalDate.parse("2024-09-27"))
            .setSigDate(LocalDate.parse("2024-09-27"))
            .build()
        val headerBytes = header.encoded
        println(
            """
                Header bytes:
                ${Hex.toHexString(headerBytes)}
                """.trimIndent()
        )
        Assert.assertEquals("dc036abc6d32c8a72cb18d7ad88d7ad8fb06", Hex.toHexString(headerBytes))
    }

    @Test
    fun testGetEncoded_V2() {
        // RESIDENCE_PERMIT 0xfb06
        val header = VdsHeader.Builder("RESIDENCE_PERMIT")
            .setRawVersion(2)
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .setIssuingDate(LocalDate.parse("2024-09-27"))
            .setSigDate(LocalDate.parse("2024-09-27"))
            .build()
        val headerBytes = header.encoded
        println(
            """
                Header bytes:
                ${Hex.toHexString(headerBytes)}
                """.trimIndent()
        )
        Assert.assertEquals("dc026abc6d32c8a51a1f8d7ad88d7ad8fb06", Hex.toHexString(headerBytes))
    }

    @Test
    @Throws(KeyStoreException::class)
    fun testBuildHeader_2parameter() {
        val ldNow = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val encodedDate: ByteArray = DataEncoder.encodeDate(ldNow)

        val vdsHeader = VdsHeader.Builder("RESIDENCE_PERMIT")
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .build()
        val headerBytes = vdsHeader.encoded
        val expectedHeaderBytes = Arrays.concatenate(
            Hex.decode("dc036abc6d32c8a72cb1"), encodedDate, encodedDate,
            Hex.decode("fb06")
        )
        Assert.assertTrue(Arrays.areEqual(expectedHeaderBytes, headerBytes))
    }

    @Test
    @Throws(KeyStoreException::class)
    fun testBuildHeader_3parameter() {
        val ldNow = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val encodedDate: ByteArray = DataEncoder.encodeDate(ldNow)

        val vdsHeader = VdsHeader.Builder("RESIDENCE_PERMIT")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .setIssuingCountry("XYZ")
            .build()
        val headerBytes = vdsHeader.encoded

        val expectedHeaderBytes = Arrays.concatenate(
            Hex.decode("dc03ed586d32c8a72cb1"), encodedDate, encodedDate,
            Hex.decode("fb06")
        )
        Assert.assertTrue(Arrays.areEqual(expectedHeaderBytes, headerBytes))
    }

    @Test
    @Throws(KeyStoreException::class)
    fun testBuildHeader_4parameter() {
        val ldate = LocalDate.parse("2016-08-16")
        val issuingDate: ByteArray = DataEncoder.encodeDate(ldate)

        val ldNow = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val signDate: ByteArray = DataEncoder.encodeDate(ldNow)


        val vdsHeader = VdsHeader.Builder("RESIDENCE_PERMIT")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .setIssuingCountry("XYZ")
            .setRawVersion(3)
            .setIssuingDate(ldate)
            .build()

        val headerBytes = vdsHeader.encoded

        val expectedHeaderBytes = Arrays.concatenate(
            Hex.decode("dc03ed586d32c8a72cb1"), issuingDate, signDate,
            Hex.decode("fb06")
        )
        Assert.assertTrue(Arrays.areEqual(expectedHeaderBytes, headerBytes))
    }

    @Test
    @Throws(KeyStoreException::class)
    fun testBuildHeader_4parameterV2() {
        val ldate = LocalDate.parse("2016-08-16")
        val issuingDate: ByteArray = DataEncoder.encodeDate(ldate)

        val ldNow = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val signDate: ByteArray = DataEncoder.encodeDate(ldNow)

        val vdsHeader = VdsHeader.Builder("TEMP_PASSPORT")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .setIssuingCountry("XYZ")
            .setRawVersion(2)
            .setIssuingDate(ldate)
            .build()
        val headerBytes = vdsHeader.encoded
        val expectedHeaderBytes = Arrays.concatenate(
            Hex.decode("dc02ed586d32c8a51a1f"), issuingDate, signDate,
            Hex.decode("f60d")
        )
        Assert.assertTrue(Arrays.areEqual(expectedHeaderBytes, headerBytes))
    }

    @Test
    fun testParseByteArray_V3() {
        val buffer = Buffer().write(Hex.decode("dc036abc6d32c8a72cb18d7ad88d7ad8fb06"))
        val header = fromBuffer(buffer)
        Assert.assertEquals("RESIDENCE_PERMIT", header.vdsType)
        Assert.assertEquals("D  ", header.issuingCountry)
        Assert.assertEquals("DETS", header.signerIdentifier)
        Assert.assertEquals("32", header.certificateReference)
        Assert.assertEquals("2024-09-27", header.issuingDate.toString())
    }

    @Test
    fun testParseByteArray_V2() {
        val buffer = Buffer().write(Hex.decode("DC02D9C56D32C8A519FC0F71346F1D67FC04"))
        val header = fromBuffer(buffer)
        Assert.assertEquals("SOCIAL_INSURANCE_CARD", header.vdsType)
        Assert.assertEquals("UTO", header.issuingCountry)
        Assert.assertEquals("DETS", header.signerIdentifier)
        Assert.assertEquals("00027", header.certificateReference)
        Assert.assertEquals("2020-01-01", header.issuingDate.toString())
    }

    companion object {
        //@formatter:off
        var keyStorePassword: String = "vdstools"
        var keyStoreFile: String = "src/test/resources/vdstools_testcerts.bks"
        var keystore: KeyStore? = null
        
        @JvmStatic
        @BeforeClass
        fun loadKeyStore():Unit {
            Security.addProvider(BouncyCastleProvider())
            keystore = KeyStore.getInstance("BKS", "BC")
            val fis = FileInputStream(keyStoreFile)
            keystore?.load(fis, keyStorePassword.toCharArray())
            fis.close()
        }
 }}
