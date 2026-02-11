package de.tsenger.vdstools.generic

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.idb.IcbRawStringsCommon
import de.tsenger.vdstools.idb.IdbSeal
import de.tsenger.vdstools.vds.VdsRawBytesCommon
import de.tsenger.vdstools.vds.VdsSeal
import kotlin.test.*

@OptIn(ExperimentalStdlibApi::class)
class SealCommonTest {

    @Test
    fun testFromStringIdb() {
        assertTrue(Seal.fromString(IcbRawStringsCommon.TemporaryPassport) is IdbSeal)
    }

    @Test
    fun testFromStringIdb_fail() {
        assertFalse(Seal.fromString(IcbRawStringsCommon.TemporaryPassport) is VdsSeal)
    }


    @Test
    fun testIdbGetMessageTag() {
        val seal = Seal.fromString(IcbRawStringsCommon.TemporaryPassport)
        val mrz = seal.getMessage(8)?.value.toString()
        assertEquals(
            "PPD<<FOLKS<<TALLULAH<<<<<<<<<<<<<<<<<<<<<<<<\n3113883489D<<9709155F1601013<<<<<<<<<<<<<<04", mrz
        )
    }

    @Test
    fun testIdbGetMessageName() {
        val seal = Seal.fromString(IcbRawStringsCommon.TemporaryPassport)
        val mrz = seal.getMessage("MRZ_TD3")?.value.toString()
        assertEquals(
            "PPD<<FOLKS<<TALLULAH<<<<<<<<<<<<<<<<<<<<<<<<\n3113883489D<<9709155F1601013<<<<<<<<<<<<<<04", mrz
        )
    }

    @Test
    fun testIdbRawString() {
        val seal = Seal.fromString(IcbRawStringsCommon.TemporaryPassport)
        assertEquals(IcbRawStringsCommon.TemporaryPassport, seal.rawString)
    }

    @Test
    fun testIdbGetPlainSignature() {
        val seal = Seal.fromString(IcbRawStringsCommon.TemporaryPassport)
        println(seal.signatureInfo?.plainSignatureBytes?.toHexString())
        val signature = seal.signatureInfo?.plainSignatureBytes
        assertNotNull(signature)
        assertEquals(64, signature.size)
        assertContentEquals(
            "5d31b07d744257e59bc43316cc6420d61464e5a0381897e99299813bfa7c857943edd6393ecd0bb74809f3a280c08156057000e93a1116eb1bf3336bbeb65c29".hexToByteArray(),
            signature
        )
    }

    @Test
    fun testIdbMessageList() {
        val seal = Seal.fromString(IcbRawStringsCommon.TemporaryPassport)
        assertEquals(4, seal.messageList.size)
        assertTrue(seal.messageList.any { it.name == "MRZ_TD3" })
    }

    @Test
    fun testIdbIssuingCountry() {
        val seal = Seal.fromString(IcbRawStringsCommon.TemporaryPassport)
        assertEquals("D", seal.issuingCountry)
    }

    @Test
    fun testIdbSignerInfo() {
        val seal = Seal.fromString(IcbRawStringsCommon.TemporaryPassport)
        val signatureInfo = seal.signatureInfo
        assertNotNull(signatureInfo)
        assertEquals("SHA256_WITH_ECDSA", signatureInfo.signatureAlgorithm)
        assertEquals("a57a790577", signatureInfo.signerCertificateReference)
        assertEquals(64, signatureInfo.plainSignatureBytes.size)
        assertNull(signatureInfo.signerCertificateBytes)
    }

    @Test
    fun testIdbSignedBytes() {
        val seal = Seal.fromString(IcbRawStringsCommon.TemporaryPassport)
        assertEquals(1089, seal.signedBytes?.size)
    }

    @Test
    fun testIdbDocumentType() {
        val seal = Seal.fromString(IcbRawStringsCommon.TemporaryPassport)
        assertEquals("TEMPORARY_PASSPORT", seal.documentType)
    }

    @Test
    fun testFromStringVds() {
        val rawString = DataEncoder.encodeBase256(VdsRawBytesCommon.emergenyTravelDoc)
        assertTrue(Seal.fromString(rawString) is VdsSeal)
    }

    @Test
    fun testVdsGetMessageTag() {
        val rawString = DataEncoder.encodeBase256(VdsRawBytesCommon.emergenyTravelDoc)
        val seal = Seal.fromString(rawString)
        val mrz = seal.getMessage(2)?.value.toString()
        assertEquals(
            "I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06", mrz
        )
    }

    @Test
    fun testVdsGetMessageName() {
        val rawString = DataEncoder.encodeBase256(VdsRawBytesCommon.emergenyTravelDoc)
        val seal = Seal.fromString(rawString)
        val mrz = seal.getMessage("MRZ")?.value.toString()
        assertEquals(
            "I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06", mrz
        )
    }

    @Test
    fun testVdsRawString() {
        val rawString = DataEncoder.encodeBase256(VdsRawBytesCommon.emergenyTravelDoc)
        val seal = Seal.fromString(rawString)
        assertEquals(rawString, seal.rawString)
    }

    @Test
    fun testVdsGetPlainSignature() {
        val rawString = DataEncoder.encodeBase256(VdsRawBytesCommon.emergenyTravelDoc)
        val seal = Seal.fromString(rawString)
        val signature = seal.signatureInfo?.plainSignatureBytes
        assertNotNull(signature)
        assertEquals(64, signature.size)
        assertContentEquals(
            "22f8bd19eccba4ef24f204787796dd914fec61f605b153b22a6ef307d3869938a4e7e908f0a63b8379880b395c7fdbac720d7f2836d08e1da62611614a00120b".hexToByteArray(),
            signature
        )
    }

    @Test
    fun testVdsMessageGroupList() {
        val rawString = DataEncoder.encodeBase256(VdsRawBytesCommon.emergenyTravelDoc)
        val seal = Seal.fromString(rawString)
        assertEquals(1, seal.messageList.size)
        assertTrue(seal.messageList.any { it.name == "MRZ" })
    }

    @Test
    fun testVdsIssuingCountry() {
        val rawString = DataEncoder.encodeBase256(VdsRawBytesCommon.emergenyTravelDoc)
        val seal = Seal.fromString(rawString)
        assertEquals("UTO", seal.issuingCountry)
    }

    @Test
    fun testVdsSignerInfo() {
        val rawString = DataEncoder.encodeBase256(VdsRawBytesCommon.emergenyTravelDoc)
        val seal = Seal.fromString(rawString)
        val signatureInfo = seal.signatureInfo
        assertNotNull(signatureInfo)
        assertEquals("SHA256_WITH_ECDSA", signatureInfo.signatureAlgorithm)
        assertEquals("UTTS5B", signatureInfo.signerCertificateReference)
        assertEquals(64, signatureInfo.plainSignatureBytes.size)
        assertNull(signatureInfo.signerCertificateBytes)
    }

    @Test
    fun testVdsSignedBytes() {
        val rawString = DataEncoder.encodeBase256(VdsRawBytesCommon.emergenyTravelDoc)
        val seal = Seal.fromString(rawString)
        assertEquals(68, seal.signedBytes?.size)
    }

    @Test
    fun testVdsDocumentType() {
        val rawString = DataEncoder.encodeBase256(VdsRawBytesCommon.emergenyTravelDoc)
        val seal = Seal.fromString(rawString)
        assertEquals("ICAO_EMERGENCY_TRAVEL_DOCUMENT", seal.documentType)
    }

    @Test
    fun testRawString() {
        val rawString =
            "RDB1BNK6ADJL2PECXOABAHIMWDAQEE6ATAXHCCNJVXVGK5TEHZJGMV22BGPATHQJTYEZ4CM6D73Z2FE4O4Q7RLE6RVZJNXMTHKH7GJN6BGPATNOAIEA7EAAAAADDKKAQCADIKQ4FAAAAACRTHI6LQNJYDEIAAAAAAA2TQGIQAAAAAI5VHAMTIAAAAAFTJNBSHEAAAACAQAAAAMQAACBYHAAAAAAAAB5RW63DSAEAAAAAAAAIQAAAADJZGK4ZAAAAAAETSMVZWGAJMAD7ACLAA7YCAIAAAAAAGU4BSMP7U772RAAUQAAAAAAAGIAAAACAQAAAAAAAAAAAAAAAAAZAAAAAICAAAAAAAAAAAAAAACBYBAH7VYAAXIJTTKZ2MM5GGOZCGGZDDMRVMHYED4CB5UP7VEAAMAAAAAAIAAMCAIAAA75SAADQAAFGFIX2KKAZF6MRSG37ZAAAKAAAAAAADB4AAD74TY7FX6HL6CBIU4OROHXUXWYFSZTVXFI47EE2NADZRJWKVIGHF7BZDEJUHKDBB2LVVJBUG6MCWD66UJDQPTNHAIKTKEB4THMTRBKM6ORXIEW5WWVQDEYMMIFHA43M5OEHWK62OQHQQKTBLBNONJTM3INJTFMRPXM6NUTBYIQWXPHK6EMENBL25ZRIW5FXG2PZO3CLJC6WCXCLFGNZKYPSKOQ7EULA7BVUAKBQ44Q6HCLT5RDUZM4D3TT55GA7H57NQ7G7LXSG4W4NNAT344KM5LE7EMSDFOE5OFQDYF6PQYZRXR3RQSBCDGV34YNJG3VUWGUJ3DL7TJAYWW7YVI5GVGPX4IKM25DFVEAGB6OM2VFHQAGMFNJFT56I7V5XIRMFOIFJDG2SRS5GFCKY6UUYUVPBL3TG2ULE6ULYNIICKTLUJK6ALUA2SNNU7TSPBXVQVRPEJ7R7UHJWBWI6XGNKWRRBXEFB27VLV3OEVKMVQBCLRFUWXFYXFVOWMF7I763YAUQXDNTP7E42DG26XKBB4HYG4UPR3NQONHCMQKS5FOZOP6JDW75G5EPKRLSGBURBIMMLAW72X4TTXLFH5ATDGB4VCA6US4G57CFEPCE6SB6JUZJGDLWTD2L7YLDXCTYPXZYSMPITJV5ABIDRACHAYDBJZXQXWIPCWWX6TVPXTIA6MQQJNAVSETK7IYNK6NXA66V2A6UELOCCMQDDEKQYNOCLDZ2NGWSIRQFODRERJXLTKFZ2PIUHF34VJDGYKAAFN67I2WUVWD2UY3FOBWKVU5YXMYV5FRRN6DWNJWA76JYR2ONQ72CZHBHYBTN7XNRGVNVRMMT7DZLUXZPOA2HA46H6ADOTMVJTNWAG6SENPRKH4SJQZWGSFXXFGP4P6Q3J5NSRFZZBLFGHVFGLQIYB5VGSHBBE2YEXIPMP4XGND45NCNSKOJIN6LIT4ZQO2QXRWLOX6BY7QNMEHHI4A5VUX54LSUSASKQRNZUREAU2IATUK5OYDB3XGQTZBHZC3SHGSQJPCRSBJVFG72WXX6RN2R34JFPYXVPUKMEM55ZTGXLR76AJR2TPT7HKGO6RTEIDE6RBFNRKJRR4NPILHJ5XLUEMZKB4A65NSF6T2YN3Y3T4EXDIQCQ3XQ2N7ERQQKZYWTTVPVCNFAJMMNE4JXNFCY4FRH27KA5P3LWZGCF4TIPXSWR2QZESM4AOSH74XRORBXWWCTS3VO4CW6Q773GBQQWPJEA4DG43NESDACDL7IABCBTZ3ZUTZWNPRAW35KVFV3NSTBLXPY7FHG3HEUY3XDT6KR37OK3J3LEJSGIENHILIYX5D2OZKBGMU7FCU7ZIXAPJORJA7MBTGAQEN"
        val seal = Seal.fromString(rawString)
        println(seal.documentType)
        assertEquals("ARRIVAL_ATTESTATION_IDB", seal.documentType)
        println(seal.messageList.joinToString("\n") { message -> "${message.name}: ${message.value}" })
        assertEquals(4, seal.messageList.size)
        assertEquals("ABC123456DEF", seal.getMessage("AZR").toString())
        assertEquals(13, seal.getMessage("NATIONAL_DOCUMENT_IDENTIFIER")?.value?.decoded)
        assertEquals(
            "AUD<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
            seal.getMessage(0x81).toString()
        )
    }

    @Test
    fun testReadmeExample() {
        val rawString =
            "RDB1BNK6ADJL2PECXOABAHIMWDAQEE6ATAXHCCNJVXVGK5TEHZJGMV22BGPATHQJTYEZ4CM6D73Z2FE4O4Q7RLE6RVZJNXMTHKH7GJN6BGPATNOAIEA7EAAAAADDKKAQCADIKQ4FAAAAACRTHI6LQNJYDEIAAAAAAA2TQGIQAAAAAI5VHAMTIAAAAAFTJNBSHEAAAACAQAAAAMQAACBYHAAAAAAAAB5RW63DSAEAAAAAAAAIQAAAADJZGK4ZAAAAAAETSMVZWGAJMAD7ACLAA7YCAIAAAAAAGU4BSMP7U772RAAUQAAAAAAAGIAAAACAQAAAAAAAAAAAAAAAAAZAAAAAICAAAAAAAAAAAAAAACBYBAH7VYAAXIJTTKZ2MM5GGOZCGGZDDMRVMHYED4CB5UP7VEAAMAAAAAAIAAMCAIAAA75SAADQAAFGFIX2KKAZF6MRSG37ZAAAKAAAAAAADB4AAD74TY7FX6HL6CBIU4OROHXUXWYFSZTVXFI47EE2NADZRJWKVIGHF7BZDEJUHKDBB2LVVJBUG6MCWD66UJDQPTNHAIKTKEB4THMTRBKM6ORXIEW5WWVQDEYMMIFHA43M5OEHWK62OQHQQKTBLBNONJTM3INJTFMRPXM6NUTBYIQWXPHK6EMENBL25ZRIW5FXG2PZO3CLJC6WCXCLFGNZKYPSKOQ7EULA7BVUAKBQ44Q6HCLT5RDUZM4D3TT55GA7H57NQ7G7LXSG4W4NNAT344KM5LE7EMSDFOE5OFQDYF6PQYZRXR3RQSBCDGV34YNJG3VUWGUJ3DL7TJAYWW7YVI5GVGPX4IKM25DFVEAGB6OM2VFHQAGMFNJFT56I7V5XIRMFOIFJDG2SRS5GFCKY6UUYUVPBL3TG2ULE6ULYNIICKTLUJK6ALUA2SNNU7TSPBXVQVRPEJ7R7UHJWBWI6XGNKWRRBXEFB27VLV3OEVKMVQBCLRFUWXFYXFVOWMF7I763YAUQXDNTP7E42DG26XKBB4HYG4UPR3NQONHCMQKS5FOZOP6JDW75G5EPKRLSGBURBIMMLAW72X4TTXLFH5ATDGB4VCA6US4G57CFEPCE6SB6JUZJGDLWTD2L7YLDXCTYPXZYSMPITJV5ABIDRACHAYDBJZXQXWIPCWWX6TVPXTIA6MQQJNAVSETK7IYNK6NXA66V2A6UELOCCMQDDEKQYNOCLDZ2NGWSIRQFODRERJXLTKFZ2PIUHF34VJDGYKAAFN67I2WUVWD2UY3FOBWKVU5YXMYV5FRRN6DWNJWA76JYR2ONQ72CZHBHYBTN7XNRGVNVRMMT7DZLUXZPOA2HA46H6ADOTMVJTNWAG6SENPRKH4SJQZWGSFXXFGP4P6Q3J5NSRFZZBLFGHVFGLQIYB5VGSHBBE2YEXIPMP4XGND45NCNSKOJIN6LIT4ZQO2QXRWLOX6BY7QNMEHHI4A5VUX54LSUSASKQRNZUREAU2IATUK5OYDB3XGQTZBHZC3SHGSQJPCRSBJVFG72WXX6RN2R34JFPYXVPUKMEM55ZTGXLR76AJR2TPT7HKGO6RTEIDE6RBFNRKJRR4NPILHJ5XLUEMZKB4A65NSF6T2YN3Y3T4EXDIQCQ3XQ2N7ERQQKZYWTTVPVCNFAJMMNE4JXNFCY4FRH27KA5P3LWZGCF4TIPXSWR2QZESM4AOSH74XRORBXWWCTS3VO4CW6Q773GBQQWPJEA4DG43NESDACDL7IABCBTZ3ZUTZWNPRAW35KVFV3NSTBLXPY7FHG3HEUY3XDT6KR37OK3J3LEJSGIENHILIYX5D2OZKBGMU7FCU7ZIXAPJORJA7MBTGAQEN"

        val seal: Seal = Seal.fromString(rawString)

        //Get list with all message in seal
        val messageList = seal.messageList
        for (message in messageList) {
            println("${message.name} (${message.coding}) -> ${message.value}")
        }

        // Access message data by name - value.toString() returns the decoded value
        val mrz: String? = seal.getMessage("MRZ_TD2")?.toString()

        // Or use type-safe access via sealed class
        val messageValue = seal.getMessage("MRZ_TD2")?.value
        if (messageValue is MessageValue.MrzValue) {
            println("MRZ: ${messageValue.mrz}")
        } else {
            // rawBytes is always an option
            println(messageValue?.rawBytes?.toHexString())
        }

        // SignatureInfo contains all signature relevant data
        val signatureInfo: SignatureInfo? = seal.signatureInfo
        println("Signing date: ${signatureInfo?.signingDate}")

        // Get the signer certificate reference
        val signerCertRef: String? = signatureInfo?.signerCertificateReference
        println("Signer certificate reference: $signerCertRef")
    }


    @Test
    fun testBaseDokumentTypeNullVds() {
        val rawString = DataEncoder.encodeBase256(VdsRawBytesCommon.addressStickerId)
        val seal = Seal.fromString(rawString)
        assertNotNull(seal)
        assertEquals("ADDRESS_STICKER_ID", seal.documentType)
        assertNull(seal.baseDocumentType)
    }

    @Test
    fun testBaseDokumentTypeNotNull() {
        val rawString = DataEncoder.encodeBase256(VdsRawBytesCommon.meldebescheinigung)
        val seal = Seal.fromString(rawString)
        assertNotNull(seal)
        assertEquals("MELDEBESCHEINIGUNG", seal.documentType)
        assertEquals("ADMINISTRATIVE_DOCUMENTS", seal.baseDocumentType)
    }

    @Test
    fun testBaseDokumentTypeNullIdb() {
        val seal = Seal.fromString(IcbRawStringsCommon.ProvisionalResidenceDocument)
        assertNotNull(seal)
        assertEquals("PROVISIONAL_RESIDENCE_DOCUMENT", seal.documentType)
        assertNull(seal.baseDocumentType)
    }


}