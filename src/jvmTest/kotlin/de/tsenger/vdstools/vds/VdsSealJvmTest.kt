package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.SignerJvmTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.util.Arrays
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


class VdsSealJvmTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun testBuildDigitalSeal() {
        val mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06"
        val passportNumber = "UFO001979"
        val vdsMessage = VdsMessageGroup.Builder("RESIDENCE_PERMIT")
            .addMessage("MRZ", mrz)
            .addMessage("PASSPORT_NUMBER", passportNumber)
            .build()

        val ecPrivKey = SignerJvmTest.keystore.getKey(
            "dets32",
            SignerJvmTest.keyStorePassword.toCharArray()
        ) as BCECPrivateKey
        val signer = Signer(ecPrivKey.encoded, "brainpoolP224r1")

        val ldNow = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val encodedDate: ByteArray = DataEncoder.encodeDate(ldNow)

        val vdsHeader = VdsHeader.Builder(vdsMessage.vdsType)
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .build()
        val vdsSeal = VdsSeal(vdsHeader, vdsMessage, signer)
        Assert.assertNotNull(vdsSeal)
        val expectedHeaderMessage = Arrays.concatenate(
            Hex.decode("dc036abc6d32c8a72cb1"), encodedDate, encodedDate,
            Hex.decode(
                "fb0602305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306"
            )
        )
        val headerMessage = Arrays.copyOfRange(vdsSeal.encoded, 0, 76)
        Assert.assertTrue(Arrays.areEqual(expectedHeaderMessage, headerMessage))
    }

    @Test
    fun testBuildDigitalSeal2() {
        val ecPrivKey = SignerJvmTest.keystore.getKey(
            "dets32",
            SignerJvmTest.keyStorePassword.toCharArray()
        ) as BCECPrivateKey
        val signer = Signer(ecPrivKey.encoded, "brainpoolP224r1")
        val header = VdsHeader.Builder("ARRIVAL_ATTESTATION")
            .setIssuingCountry("D<<")
            .setSignerIdentifier("DETS")
            .setCertificateReference("32")
            .setIssuingDate(LocalDate.parse("2024-09-27"))
            .setSigDate(LocalDate.parse("2024-09-27"))
            .build()
        val mrz = "MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06"
        val azr = "ABC123456DEF"
        val vdsMessage = VdsMessageGroup.Builder(header.vdsType)
            .addMessage("MRZ", mrz)
            .addMessage("AZR", azr)
            .build()
        val vdsSeal = VdsSeal(header, vdsMessage, signer)

        Assert.assertNotNull(vdsSeal)
        val expectedHeaderMessage = Hex.decode(
            "dc036abc6d32c8a72cb18d7ad88d7ad8fd020230a56213535bd4caecc87ca4ccaeb4133c133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b030859e9203833736d24"
        )
        val headerMessage = Arrays.copyOfRange(vdsSeal.encoded, 0, 78)
        Assert.assertTrue(Arrays.areEqual(expectedHeaderMessage, headerMessage))
    }
}
