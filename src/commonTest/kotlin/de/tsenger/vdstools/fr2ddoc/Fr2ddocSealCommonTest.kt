package de.tsenger.vdstools.fr2ddoc

import de.tsenger.vdstools.fr2ddoc.Fr2ddocRawStringsCommon.rawString1
import de.tsenger.vdstools.fr2ddoc.Fr2ddocRawStringsCommon.rawString2
import de.tsenger.vdstools.fr2ddoc.Fr2ddocRawStringsCommon.rawString3
import de.tsenger.vdstools.fr2ddoc.Fr2ddocRawStringsCommon.rawString4
import de.tsenger.vdstools.fr2ddoc.Fr2ddocRawStringsCommon.rawString5
import de.tsenger.vdstools.generic.Seal
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Fr2ddocSealCommonTest {

    @Test
    fun testParseSample01_acteHuissier() {
        val seal = Fr2ddocSeal.fromRawString(rawString1)
        assertEquals("12", seal.documentType)
        assertEquals("FR", seal.issuingCountry)
        assertEquals(8, seal.messageList.size)

        // field 90 (0x90 = 144): "MAITRE/SPECIMEN/NATACHA"
        assertEquals("MAITRE/SPECIMEN/NATACHA", seal.getMessage(0x90)?.toString())
        // field 92 (0x92 = 146): "RAISON SOCIALE DE TEST"
        assertEquals("RAISON SOCIALE DE TEST", seal.getMessage(0x92)?.toString())
        // field 94 (0x94 = 148): "SAISIE CONSERVATOIRE DE CREANCES"
        assertEquals("SAISIE CONSERVATOIRE DE CREANCES", seal.getMessage(0x94)?.toString())
        // field 96 (0x96 = 150): date 2017-11-21
        assertEquals(LocalDate(2017, 11, 21).toString(), seal.getMessage(0x96)?.toString())
        // field 91 (0x91 = 145): "MME/BERTHIER/CORINNE"
        assertEquals("MME/BERTHIER/CORINNE", seal.getMessage(0x91)?.toString())
        // field 93 (0x93 = 147): "RAISON SOCIALE DU TIERS CONCERNE"
        assertEquals("RAISON SOCIALE DU TIERS CONCERNE", seal.getMessage(0x93)?.toString())
        // field 95 (0x95 = 149): "1896547853AB"
        assertEquals("1896547853AB", seal.getMessage(0x95)?.toString())
        // field 0C (0x0C = 12): base32 decoded URL
        assertEquals("huissier-justice.fr/1896547853AB", seal.getMessage(0x0C)?.toString())

        // Verify signature
        assertNotNull(seal.signatureInfo)
        assertEquals("FR000001", seal.signatureInfo!!.signerCertificateReference)
    }

    @Test
    fun testParseSample02_attestationCVE() {
        val seal = Fr2ddocSeal.fromRawString(rawString2)
        assertEquals("B1", seal.documentType)
        assertEquals("FR", seal.issuingCountry)
        assertEquals(6, seal.messageList.size)

        // BK (0xBK? No, BK is not hex!) - field IDs like "BK" need to be parsed as hex
        // BK = 11*16+20 = no, that's not valid hex... wait
        // Actually the tag is fieldId.toInt(16). "BK" is not valid hex!
        // Let me check: B=11, K is not a hex digit. So this will fail.
        // The plan says "parse fieldId as hex int" but BK, BB etc are not valid hex.
        // Let me verify by name instead.
        assertEquals("18-ROSWFTHR-35", seal.getMessage("Numero de l'Attestation de versement de la CVE")?.toString())
        assertEquals("CORINNE/NATACHA", seal.getMessage("Liste des prenoms")?.toString())
        assertEquals("BERTHIER", seal.getMessage("Nom patronymique")?.toString())
        assertEquals("", seal.getMessage("Nom d'usage")?.toString())
        assertEquals(LocalDate(1973, 7, 12).toString(), seal.getMessage("Date de naissance")?.toString())
        assertEquals("9654321785T", seal.getMessage("Numero ou code d'identification de l'etudiant")?.toString())
    }

    @Test
    fun testParseSample03_certificatCession() {
        val seal = Fr2ddocSeal.fromRawString(rawString3)
        assertEquals("A8", seal.documentType)
        assertEquals("FR", seal.issuingCountry)

        assertEquals("83CSG75", seal.getMessage("Immatriculation du vehicule")?.toString())
        assertEquals("12345678901234567", seal.getMessage("Numero de serie du vehicule (VIN)")?.toString())
        assertEquals(
            LocalDate(1970, 1, 2).toString(),
            seal.getMessage("Date de premiere immatriculation du vehicule")?.toString()
        )
        assertEquals("1337", seal.getMessage("Kilometrage")?.toString())
        assertEquals("DU PONT", seal.getMessage("Nom patronymique du vendeur")?.toString())
        assertEquals("JEAN FRANCOIS", seal.getMessage("Prenom du vendeur")?.toString())
        assertEquals(
            LocalDateTime(2020, 3, 2, 14, 0).toString(),
            seal.getMessage("Date et heure de la cession")?.toString()
        )
        assertEquals(LocalDate(2020, 3, 2).toString(), seal.getMessage("Date de la signature du vendeur")?.toString())
        assertEquals("DURAND", seal.getMessage("Nom patronymique de l'acheteur")?.toString())
        assertEquals("FREDERIC", seal.getMessage("Prenom de l'acheteur")?.toString())
        assertEquals("42 RUE DES TESTS", seal.getMessage("Ligne 4 adresse du domicile de l'acheteur")?.toString())
        assertEquals("10430", seal.getMessage("Code postal du domicile de l'acheteur")?.toString())
        assertEquals("SAINTE COMMUNE DES TESTS", seal.getMessage("Commune du domicile de l'acheteur")?.toString())
        assertEquals("123456", seal.getMessage("N d'enregistrement")?.toString())
        assertEquals(
            LocalDateTime(2020, 3, 2, 14, 0).toString(),
            seal.getMessage("Date et heure d'enregistrement dans le SIV")?.toString()
        )
        assertEquals("M", seal.getMessage("Genre du vendeur")?.toString())
        assertEquals("M", seal.getMessage("Genre de l'acheteur")?.toString())
    }

    @Test
    fun testParseSample04_documentEtranger() {
        val seal = Fr2ddocSeal.fromRawString(rawString4)
        assertEquals("13", seal.documentType)
        assertEquals("FR", seal.issuingCountry)

        assertEquals("2", seal.getMessage("Type de document etranger")?.toString())
        assertEquals("9201202004012359123", seal.getMessage("Numero de la demande de document etranger")?.toString())
        assertEquals(LocalDate(2020, 4, 1).toString(), seal.getMessage("Date de depot de la demande")?.toString())
        assertEquals("AUTORISE A TRAVAILLER", seal.getMessage("Autorisation")?.toString())
        assertEquals("7503120521", seal.getMessage("Numero d'etranger")?.toString())
        assertEquals("SPECIMEN", seal.getMessage("Nom patronymique")?.toString())
        assertEquals("NATACHA/CORINNE", seal.getMessage("Liste des prenoms")?.toString())
        assertEquals("F", seal.getMessage("Genre")?.toString())
        assertEquals(LocalDate(1973, 7, 12).toString(), seal.getMessage("Date de naissance")?.toString())
        assertEquals("BUENOS AIRES", seal.getMessage("Lieu de naissance")?.toString())
        assertEquals("AR", seal.getMessage("Pays de naissance")?.toString())
        assertEquals("AR", seal.getMessage("Nationalite")?.toString())
    }

    @Test
    fun testParseSample05_attestationDICEM() {
        val seal = Fr2ddocSeal.fromRawString(rawString5)
        assertEquals("14", seal.documentType)
        assertEquals("FR", seal.issuingCountry)

        assertEquals("123456", seal.getMessage("Numero d'identification")?.toString())
        assertEquals(
            "CYCLOMOTEUR MOTOCYCLETTE TRICYCLE A MOTEUR TOUT TERRAIN",
            seal.getMessage("Type d'engin")?.toString()
        )
        assertEquals("12345678975123ABDC", seal.getMessage("Numero de serie")?.toString())
        assertEquals("MARQUE VEHICULE", seal.getMessage("Marque du vehicule")?.toString())
        assertEquals("ROUGE", seal.getMessage("Couleur dominante")?.toString())
        assertEquals("1", seal.getMessage("Type de proprietaire")?.toString())
        assertEquals("SPECIMEN", seal.getMessage("Nom patronymique")?.toString())
        assertEquals("NATACHA/CORINNE", seal.getMessage("Liste des prenoms")?.toString())
    }

    @Test
    fun testParseSealFromGenericSeal() {
        val seal = Seal.fromString(rawString1)
        assertEquals("12", seal.documentType)
        assertEquals("FR", seal.issuingCountry)
    }

    @Test
    fun testSignedBytesDoNotIncludeSignature() {
        val seal = Fr2ddocSeal.fromRawString(rawString1)
        val signedStr = seal.signedBytes!!.decodeToString()
        // signedBytes should be everything before US separator
        assertTrue(!signedStr.contains('\u001F'), "signedBytes should not contain US separator")
        assertTrue(signedStr.startsWith("DC04"), "signedBytes should start with header")
    }

    @Test
    @OptIn(ExperimentalStdlibApi::class)
    fun testSignatureBytes() {
        val seal = Fr2ddocSeal.fromRawString(rawString1)
        val sigHex = seal.signatureInfo!!.plainSignatureBytes.toHexString()
        assertEquals(
            "73aed1edb18987950719f546c3c99fb1d3bd6d0259a7221169ff1d6d93863170" +
                    "8d9afe45c4de1cbcea6d8ca8e7a7cd79858291dd2a74cea1d386f1e83faa006a",
            sigHex
        )
    }
}
