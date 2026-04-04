package de.tsenger.vdstools.vds

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.EcdsaSigner
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
        val ecPrivKey = SignerJvmTest.keystore.getKey(
            "dets32",
            SignerJvmTest.keyStorePassword.toCharArray()
        ) as BCECPrivateKey
        val signer = EcdsaSigner(ecPrivKey.encoded, "brainpoolP224r1")

        val ldNow = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val encodedDate: ByteArray = DataEncoder.encodeDate(ldNow)

        val vdsSeal = VdsSeal.Builder("RESIDENCE_PERMIT")
            .issuingCountry("D<<")
            .signerIdentifier("DETS")
            .certificateReference("32")
            .addMessage("MRZ", "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06")
            .addMessage("PASSPORT_NUMBER", "UFO001979")
            .build(signer)

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
        val signer = EcdsaSigner(ecPrivKey.encoded, "brainpoolP224r1")

        val vdsSeal = VdsSeal.Builder("ARRIVAL_ATTESTATION")
            .issuingCountry("D<<")
            .signerIdentifier("DETS")
            .certificateReference("32")
            .issuingDate(LocalDate.parse("2024-09-27"))
            .sigDate(LocalDate.parse("2024-09-27"))
            .addMessage("MRZ", "MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06")
            .addMessage("AZR", "ABC123456DEF")
            .build(signer)

        Assert.assertNotNull(vdsSeal)
        val expectedHeaderMessage = Hex.decode(
            "dc036abc6d32c8a72cb18d7ad88d7ad8fd020230a56213535bd4caecc87ca4ccaeb4133c133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b030859e9203833736d24"
        )
        val headerMessage = Arrays.copyOfRange(vdsSeal.encoded, 0, 78)
        Assert.assertTrue(Arrays.areEqual(expectedHeaderMessage, headerMessage))
    }
}
