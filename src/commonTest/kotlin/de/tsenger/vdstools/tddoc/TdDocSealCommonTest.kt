package de.tsenger.vdstools.tddoc

import de.tsenger.vdstools.fr2ddoc.Fr2ddocRawStringsCommon.rawString1
import de.tsenger.vdstools.fr2ddoc.Fr2ddocRawStringsCommon.rawString2
import de.tsenger.vdstools.fr2ddoc.Fr2ddocRawStringsCommon.rawString3
import de.tsenger.vdstools.fr2ddoc.Fr2ddocRawStringsCommon.rawString4
import de.tsenger.vdstools.fr2ddoc.Fr2ddocRawStringsCommon.rawString5
import de.tsenger.vdstools.generic.SealParser
import de.tsenger.vdstools.generic.SealType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TdDocSealCommonTest {

    private val parser = SealParser(setOf(SealType.TDDOC))

    @Test
    fun testParseSample01_acteHuissier() {
        val seal = TdDocSeal.fromRawString(rawString1)
        assertEquals("12", seal.documentType)
        assertEquals("FR", seal.issuingCountry)
        assertEquals(8, seal.messageList.size)

        // field 90: "MAITRE/SPECIMEN/NATACHA"
        assertEquals("MAITRE/SPECIMEN/NATACHA", seal.getMessageByTag("90")?.toString())
        // field 92: "RAISON SOCIALE DE TEST"
        assertEquals("RAISON SOCIALE DE TEST", seal.getMessageByTag("92")?.toString())
        // field 94: "SAISIE CONSERVATOIRE DE CREANCES"
        assertEquals("SAISIE CONSERVATOIRE DE CREANCES", seal.getMessageByTag("94")?.toString())
        // field 96: date 2017-11-21
        assertEquals(LocalDate(2017, 11, 21).toString(), seal.getMessageByTag("96")?.toString())
        // field 91: "MME/BERTHIER/CORINNE"
        assertEquals("MME/BERTHIER/CORINNE", seal.getMessageByTag("91")?.toString())
        // field 93: "RAISON SOCIALE DU TIERS CONCERNE"
        assertEquals("RAISON SOCIALE DU TIERS CONCERNE", seal.getMessageByTag("93")?.toString())
        // field 95: "1896547853AB"
        assertEquals("1896547853AB", seal.getMessageByTag("95")?.toString())
        // field 0C: base32 decoded URL
        assertEquals("huissier-justice.fr/1896547853AB", seal.getMessageByTag("0C")?.toString())

        // Verify signature
        assertNotNull(seal.signatureInfo)
        assertEquals("FR000001", seal.signatureInfo!!.signerCertificateReference)
    }

    @Test
    fun testParseSample02_attestationCVE() {
        val seal = TdDocSeal.fromRawString(rawString2)
        assertEquals("B1", seal.documentType)
        assertEquals("FR", seal.issuingCountry)
        assertEquals(6, seal.messageList.size)

        assertEquals("18-ROSWFTHR-35", seal.getMessageByTag("BK")?.toString())
        assertEquals("CORINNE/NATACHA", seal.getMessageByName("Liste des prenoms")?.toString())
        assertEquals("BERTHIER", seal.getMessageByName("Nom patronymique")?.toString())
        assertEquals("", seal.getMessageByName("Nom d'usage")?.toString())
        assertEquals(LocalDate(1973, 7, 12).toString(), seal.getMessageByName("Date de naissance")?.toString())
        assertEquals("9654321785T", seal.getMessageByName("Numero ou code d'identification de l'etudiant")?.toString())
    }

    @Test
    fun testParseSample03_certificatCession() {
        val seal = TdDocSeal.fromRawString(rawString3)
        assertEquals("A8", seal.documentType)
        assertEquals("FR", seal.issuingCountry)

        assertEquals("83CSG75", seal.getMessageByName("Immatriculation du vehicule")?.toString())
        assertEquals("12345678901234567", seal.getMessageByName("Numero de serie du vehicule (VIN)")?.toString())
        assertEquals(
            LocalDate(1970, 1, 2).toString(),
            seal.getMessageByName("Date de premiere immatriculation du vehicule")?.toString()
        )
        assertEquals("1337", seal.getMessageByName("Kilometrage")?.toString())
        assertEquals("DU PONT", seal.getMessageByName("Nom patronymique du vendeur")?.toString())
        assertEquals("JEAN FRANCOIS", seal.getMessageByName("Prenom du vendeur")?.toString())
        assertEquals(
            LocalDateTime(2020, 3, 2, 14, 0).toString(),
            seal.getMessageByName("Date et heure de la cession")?.toString()
        )
        assertEquals(LocalDate(2020, 3, 2).toString(), seal.getMessageByName("Date de la signature du vendeur")?.toString())
        assertEquals("DURAND", seal.getMessageByName("Nom patronymique de l'acheteur")?.toString())
        assertEquals("FREDERIC", seal.getMessageByName("Prenom de l'acheteur")?.toString())
        assertEquals("42 RUE DES TESTS", seal.getMessageByName("Ligne 4 adresse du domicile de l'acheteur")?.toString())
        assertEquals("10430", seal.getMessageByName("Code postal du domicile de l'acheteur")?.toString())
        assertEquals("SAINTE COMMUNE DES TESTS", seal.getMessageByName("Commune du domicile de l'acheteur")?.toString())
        assertEquals("123456", seal.getMessageByName("N d'enregistrement")?.toString())
        assertEquals(
            LocalDateTime(2020, 3, 2, 14, 0).toString(),
            seal.getMessageByName("Date et heure d'enregistrement dans le SIV")?.toString()
        )
        assertEquals("M", seal.getMessageByName("Genre du vendeur")?.toString())
        assertEquals("M", seal.getMessageByName("Genre de l'acheteur")?.toString())
    }

    @Test
    fun testParseSample04_documentEtranger() {
        val seal = TdDocSeal.fromRawString(rawString4)
        assertEquals("13", seal.documentType)
        assertEquals("FR", seal.issuingCountry)

        assertEquals("2", seal.getMessageByName("Type de document etranger")?.toString())
        assertEquals("9201202004012359123", seal.getMessageByName("Numero de la demande de document etranger")?.toString())
        assertEquals(LocalDate(2020, 4, 1).toString(), seal.getMessageByName("Date de depot de la demande")?.toString())
        assertEquals("AUTORISE A TRAVAILLER", seal.getMessageByName("Autorisation")?.toString())
        assertEquals("7503120521", seal.getMessageByName("Numero d'etranger")?.toString())
        assertEquals("SPECIMEN", seal.getMessageByName("Nom patronymique")?.toString())
        assertEquals("NATACHA/CORINNE", seal.getMessageByName("Liste des prenoms")?.toString())
        assertEquals("F", seal.getMessageByName("Genre")?.toString())
        assertEquals(LocalDate(1973, 7, 12).toString(), seal.getMessageByName("Date de naissance")?.toString())
        assertEquals("BUENOS AIRES", seal.getMessageByName("Lieu de naissance")?.toString())
        assertEquals("AR", seal.getMessageByName("Pays de naissance")?.toString())
        assertEquals("AR", seal.getMessageByName("Nationalite")?.toString())
    }

    @Test
    fun testParseSample05_attestationDICEM() {
        val seal = TdDocSeal.fromRawString(rawString5)
        assertEquals("14", seal.documentType)
        assertEquals("FR", seal.issuingCountry)

        assertEquals("123456", seal.getMessageByName("Numero d'identification")?.toString())
        assertEquals(
            "CYCLOMOTEUR MOTOCYCLETTE TRICYCLE A MOTEUR TOUT TERRAIN",
            seal.getMessageByName("Type d'engin")?.toString()
        )
        assertEquals("12345678975123ABDC", seal.getMessageByName("Numero de serie")?.toString())
        assertEquals("MARQUE VEHICULE", seal.getMessageByName("Marque du vehicule")?.toString())
        assertEquals("ROUGE", seal.getMessageByName("Couleur dominante")?.toString())
        assertEquals("1", seal.getMessageByName("Type de proprietaire")?.toString())
        assertEquals("SPECIMEN", seal.getMessageByName("Nom patronymique")?.toString())
        assertEquals("NATACHA/CORINNE", seal.getMessageByName("Liste des prenoms")?.toString())
    }

    @Test
    fun testParseSample05_messageList() {
        val seal = TdDocSeal.fromRawString(rawString5)
        assertEquals("14", seal.documentType)
        assertEquals("FR", seal.issuingCountry)

        assertEquals(15, seal.messageList.size)
        for (message in seal.messageList) {
            println("${message.tag}(${message.name}:${message.value::class.simpleName}): ${message.value}")
        }
    }

    @Test
    fun testParseSealViaParser() {
        val seal = parser.parse(rawString1)
        assertEquals("12", seal.documentType)
        assertEquals("FR", seal.issuingCountry)
    }

    @Test
    fun testSignedBytesDoNotIncludeSignature() {
        val seal = TdDocSeal.fromRawString(rawString1)
        val signedStr = seal.signedBytes!!.decodeToString()
        assertTrue(!signedStr.contains('\u001F'), "signedBytes should not contain US separator")
        assertTrue(signedStr.startsWith("DC04"), "signedBytes should start with header")
    }

    @Test
    @OptIn(ExperimentalStdlibApi::class)
    fun testSignatureBytes() {
        val seal = TdDocSeal.fromRawString(rawString1)
        val sigHex = seal.signatureInfo!!.plainSignatureBytes.toHexString()
        assertEquals(
            "73aed1edb18987950719f546c3c99fb1d3bd6d0259a7221169ff1d6d93863170" +
                    "8d9afe45c4de1cbcea6d8ca8e7a7cd79858291dd2a74cea1d386f1e83faa006a",
            sigHex
        )
    }
}
