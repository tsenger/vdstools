# VdsTools - Kotlin multiplatform library to work with Visible Digital Seals

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)  ![Maven Central Version](https://img.shields.io/maven-central/v/de.tsenger/vdstools?color=green) ![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/tsenger/vdstools/gradle.build.yml)

This a Kotlin multiplatform (JVM and iOS) library to decode/verify and encode/sign Visible Digital Seals (VDS) as
specified in

- [BSI TR-03137 Part 1](https://www.bsi.bund.de/EN/Themen/Unternehmen-und-Organisationen/Standards-und-Zertifizierung/Technische-Richtlinien/TR-nach-Thema-sortiert/tr03137/tr-03137.html)
- [ICAO Doc 9303 Part 13: Visible Digital Seals](https://www2023.icao.int/publications/Documents/9303_p13_cons_en.pdf)
- [ICAO TR "VDS for Non-Electronic Documents"](https://www2023.icao.int/Security/FAL/TRIP/Documents/TR%20-%20Visible%20Digital%20Seals%20for%20Non-Electronic%20Documents%20V1.7.pdf)

It also fully supports encoding and decoding Seals defined in the new draft
of [ICAO Datastructure for Barcode](https://www.icao.int/Security/FAL/TRIP/PublishingImages/Pages/Publications/ICAO%20TR%20-%20ICAO%20Datastructure%20for%20Barcode.pdf).
VDS and ICD barcodes can be parsed by a generic interface. An example is given in the following chapter

VDS can be created with the help of this library or, if you want to try it out quickly, via the
web [Sealgen](https://sealgen.tsenger.de) tool.
There is also the **Sealva** mobile app which scans, verifies and displays all VDS profiles defined in the above
specifications.

<a href='https://play.google.com/store/apps/details?id=de.tsenger.sealver&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width='155' height='60'/></a>

<a href="https://apps.apple.com/de/app/sealva-vds-validator/id6756822727?itscg=30200&itsct=apps_box_badge&mttnsubad=6756822727" style="display: inline-block;"><img src="https://toolbox.marketingtools.apple.com/api/v2/badges/download-on-the-app-store/black/en-us?releaseDate=1767571200" alt="Download on the App Store" style="width: 155px; height: 45px; vertical-align: middle; object-fit: contain;" /></a>

## Parse and verify a VDS / IDB

Here is a quick overview how to use the generic parser and verifier. The generic interface
handles VDS and IDB barcode via common function calls.
When you received the raw string from your favorite datamatrix decoder use VdsTools like this:

```kotlin
import de.tsenger.vdstools.Verifier
import de.tsenger.vdstools.generic.Seal
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.SignatureInfo
import de.tsenger.vdstools.generic.MessageValue

// example raw string coming as result from your used barcode scanner library
val rawString =
    "RDB1BNK6ADJL2PECXOABAHIMWDAQEE6ATAXHCCNJVXVGK5TEHZJGMV22BGPATHQJTYEZ4CM6D73Z2FE4O4Q7RLE6RVZJNXMTHKH7GJN6BGPATNOAIEA7EAAAAADDKKAQCADIKQ4FAAAAACRTHI6LQNJYDEIAAAAAAA2TQGIQAAAAAI5VHAMTIAAAAAFTJNBSHEAAAACAQAAAAMQAACBYHAAAAAAAAB5RW63DSAEAAAAAAAAIQAAAADJZGK4ZAAAAAAETSMVZWGAJMAD7ACLAA7YCAIAAAAAAGU4BSMP7U772RAAUQAAAAAAAGIAAAACAQAAAAAAAAAAAAAAAAAZAAAAAICAAAAAAAAAAAAAAACBYBAH7VYAAXIJTTKZ2MM5GGOZCGGZDDMRVMHYED4CB5UP7VEAAMAAAAAAIAAMCAIAAA75SAADQAAFGFIX2KKAZF6MRSG37ZAAAKAAAAAAADB4AAD74TY7FX6HL6CBIU4OROHXUXWYFSZTVXFI47EE2NADZRJWKVIGHF7BZDEJUHKDBB2LVVJBUG6MCWD66UJDQPTNHAIKTKEB4THMTRBKM6ORXIEW5WWVQDEYMMIFHA43M5OEHWK62OQHQQKTBLBNONJTM3INJTFMRPXM6NUTBYIQWXPHK6EMENBL25ZRIW5FXG2PZO3CLJC6WCXCLFGNZKYPSKOQ7EULA7BVUAKBQ44Q6HCLT5RDUZM4D3TT55GA7H57NQ7G7LXSG4W4NNAT344KM5LE7EMSDFOE5OFQDYF6PQYZRXR3RQSBCDGV34YNJG3VUWGUJ3DL7TJAYWW7YVI5GVGPX4IKM25DFVEAGB6OM2VFHQAGMFNJFT56I7V5XIRMFOIFJDG2SRS5GFCKY6UUYUVPBL3TG2ULE6ULYNIICKTLUJK6ALUA2SNNU7TSPBXVQVRPEJ7R7UHJWBWI6XGNKWRRBXEFB27VLV3OEVKMVQBCLRFUWXFYXFVOWMF7I763YAUQXDNTP7E42DG26XKBB4HYG4UPR3NQONHCMQKS5FOZOP6JDW75G5EPKRLSGBURBIMMLAW72X4TTXLFH5ATDGB4VCA6US4G57CFEPCE6SB6JUZJGDLWTD2L7YLDXCTYPXZYSMPITJV5ABIDRACHAYDBJZXQXWIPCWWX6TVPXTIA6MQQJNAVSETK7IYNK6NXA66V2A6UELOCCMQDDEKQYNOCLDZ2NGWSIRQFODRERJXLTKFZ2PIUHF34VJDGYKAAFN67I2WUVWD2UY3FOBWKVU5YXMYV5FRRN6DWNJWA76JYR2ONQ72CZHBHYBTN7XNRGVNVRMMT7DZLUXZPOA2HA46H6ADOTMVJTNWAG6SENPRKH4SJQZWGSFXXFGP4P6Q3J5NSRFZZBLFGHVFGLQIYB5VGSHBBE2YEXIPMP4XGND45NCNSKOJIN6LIT4ZQO2QXRWLOX6BY7QNMEHHI4A5VUX54LSUSASKQRNZUREAU2IATUK5OYDB3XGQTZBHZC3SHGSQJPCRSBJVFG72WXX6RN2R34JFPYXVPUKMEM55ZTGXLR76AJR2TPT7HKGO6RTEIDE6RBFNRKJRR4NPILHJ5XLUEMZKB4A65NSF6T2YN3Y3T4EXDIQCQ3XQ2N7ERQQKZYWTTVPVCNFAJMMNE4JXNFCY4FRH27KA5P3LWZGCF4TIPXSWR2QZESM4AOSH74XRORBXWWCTS3VO4CW6Q773GBQQWPJEA4DG43NESDACDL7IABCBTZ3ZUTZWNPRAW35KVFV3NSTBLXPY7FHG3HEUY3XDT6KR37OK3J3LEJSGIENHILIYX5D2OZKBGMU7FCU7ZIXAPJORJA7MBTGAQEN"

val seal: Seal = Seal.fromString(rawString)

//Get list with all messages in seal
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

// Since X509 certificate handling is strongly platform-dependent,
// the Verifier is given the plain publicKey (r|s) and the curve name.
val publicKeyBytes: ByteArray = ... // load from your certificate store
val verifier = Verifier(
    seal.signedBytes,
    signatureInfo!!.plainSignatureBytes,
    publicKeyBytes,
    "brainpoolP224r1"
)
val result: Verifier.Result = verifier.verify()
```

## Build a barcode

Here is an example on how to use the DataEncoder and Signer classes to build a VDS barcode:

```kotlin
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.vds.VdsHeader
import de.tsenger.vdstools.vds.VdsMessageGroup
import de.tsenger.vdstools.vds.VdsSeal
import kotlinx.datetime.LocalDate

val keystore: KeyStore = ...

// In this JVM example we use a BouncyCastle keystore to get the certificate (for the header information)
// and the private key for signing the seal's data
val cert: X509Certificate = keystore.getCertificate(keyAlias)
val ecKey: ECPrivateKey = keystore.getKey(certAlias, keyStorePassword.toCharArray())

// Initialize the Signer with the private key bytes and curve name
val signer = Signer(ecKey.encoded, curveName)

// 1. Build a VdsHeader
val header = VdsHeader.Builder("ARRIVAL_ATTESTATION")
    .setIssuingCountry("D<<")
    .setSignerIdentifier("DETS")
    .setCertificateReference("32")
    .setIssuingDate(LocalDate.parse("2024-09-27"))
    .setSigDate(LocalDate.parse("2024-09-27"))
    .build()

// 2. Build a VdsMessageGroup with messages
val mrz = "MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06"
val azr = "ABC123456DEF"
val messageGroup = VdsMessageGroup.Builder(header.vdsType)
    .addMessage("MRZ", mrz)
    .addMessage("AZR", azr)
    .build()

// 3. Build a signed VdsSeal
val vdsSeal = VdsSeal(header, messageGroup, signer)

// The encoded bytes can now be used to build a DataMatrix (or other) code
val encodedSealBytes = vdsSeal.encoded

```

Here is an example on how to use the DataEncoder and Signer classes to build an IDB barcode:

```kotlin
import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Signer
import de.tsenger.vdstools.idb.*

val keystore: KeyStore = ...

// In this JVM example we use a BouncyCastle keystore to get the certificate (for the header information)
// and the private key for signing the seal's data
val cert: X509Certificate = keystore.getCertificate(keyAlias)
val ecKey: ECPrivateKey = keystore.getKey(certAlias, keyStorePassword.toCharArray())

// Initialize the Signer with the private key bytes and curve name
val signer: Signer = Signer(ecKey.encoded, curveName)

// 1. Build an IdbHeader
val header = IdbHeader(
    countryIdentifier = "D<<",
    signatureAlgorithm = IdbSignatureAlgorithm.SHA256_WITH_ECDSA,
    certificateReference = DataEncoder.buildCertificateReference(cert.encoded),
    signatureCreationDate = "2025-02-11"
)

// 2. Build an IdbMessageGroup with messages
val messageGroup = IdbMessageGroup.Builder()
    .addMessage(0x80, faceImageBytes)           // FACE_IMAGE
    .addMessage(0x81, mrzTd2String)             // MRZ_TD2
    .addMessage(0x84, "2026-04-23")             // EXPIRY_DATE
    .addMessage(0x86, 0x01)                     // NATIONAL_DOCUMENT_IDENTIFIER
    .build()

// 3. Sign the header + messageGroup
val dataToSign = header.encoded + messageGroup.encoded
val signatureBytes = signer.sign(dataToSign)
val signature = IdbSignature(signatureBytes)

// 4. Build the IdbSeal
val payload = IdbPayload(header, messageGroup, null, signature)
val idbSeal = IdbSeal(isSigned = true, isZipped = false, barcodePayload = payload)

// The raw string can now be used to build a DataMatrix (or other) code
val encodedRawString = idbSeal.rawString

```

Also have a look at the testcases for more usage inspiration.
You will also find an example on how to generate a datamatrix image with the Zxing library in the jvmTests.

## Custom seal codings

VdsTools uses a JSON-based configuration to define seal document types and their message encodings which follows the
encoding based on BSI TR-03137.
Profile definitions according to BSI TR-03171 are currently not fully supported, as the profile definitions are not
publicly available at this time.
You can load your own custom codings at runtime to support additional document types beyond the standard ones.

### Loading Custom Codings

```kotlin
// Option 1: Load from JSON string
val customJson = """[
  {
    "documentType": "MY_CUSTOM_DOCUMENT",
    "documentRef": "ab01",
    "version": 1,
    "messages": [
      {
        "name": "OWNER_NAME",
        "tag": 1,
        "coding": "C40",
        "decodedLength": 30,
        "required": true,
        "minLength": 1,
        "maxLength": 20
      },
      {
        "name": "ISSUE_DATE",
        "tag": 2,
        "coding": "DATE",
        "required": true,
        "minLength": 3,
        "maxLength": 3
      }
    ]
  }
]"""

DataEncoder.loadCustomSealCodings(customJson)

// Option 2: Load from file (JVM only)
DataEncoder.loadCustomSealCodingsFromFile("path/to/MyCodings.json")
```

### Using Custom Document Types

Once loaded, custom document types work seamlessly with the existing API:

```kotlin
val messageGroup = VdsMessageGroup.Builder("MY_CUSTOM_DOCUMENT")
    .addMessage("OWNER_NAME", "MAX MUSTERMANN")
    .addMessage("ISSUE_DATE", "2024-06-15")
    .build()

val header = VdsHeader.Builder("MY_CUSTOM_DOCUMENT")
    .setIssuingCountry("D<<")
    .setSignerIdentifier("TEST")
    .setCertificateReference("32")
    .setIssuingDate(LocalDate.now())
    .setSigDate(LocalDate.now())
    .build()

val vdsSeal = VdsSeal(header, messageGroup, signer)
```

### Available Coding Types

| Coding        | Description                                                 |
|---------------|-------------------------------------------------------------|
| `C40`         | Compressed text encoding (uppercase, digits, special chars) |
| `MRZ`         | Machine Readable Zone (special C40 variant)                 |
| `UTF8_STRING` | Direct UTF-8 text                                           |
| `BYTE`        | Single byte value                                           |
| `BYTES`       | Variable-length byte array (images, binary data)            |
| `DATE`        | 3-byte date (ICAO format, e.g. "2024-06-15")                |
| `MASKED_DATE` | 4-byte date with uncertainty masks                          |

See `src/commonMain/resources/SealCodings.json` for the complete structure of the default codings.

## Documentation

[![javadoc](https://javadoc.io/badge2/de.tsenger/vdstools/javadoc.svg)](https://javadoc.io/doc/de.tsenger/vdstools)

Online JavaDoc can be found here:
[https://javadoc.io/doc/de.tsenger/vdstools](https://javadoc.io/doc/de.tsenger/vdstools)

## How to include

The vdstools library is available on
the [Maven Central Repository](https://central.sonatype.com/artifact/de.tsenger/vdstools)
and [GitHub Packages](https://github.com/tsenger/vdstools/packages/2279382) to be
easy to integrate in your projects.

### Gradle

To include this library to your Gradle build add this dependency:

```groovy
dependencies {
    implementation 'de.tsenger:vdstools:0.11.0'
}
```

### Maven

To include this library to your Maven build add this dependency:

```xml

<dependency>
    <groupId>de.tsenger</groupId>
    <artifactId>vdstools</artifactId>
    <version>0.11.0</version>
</dependency>
```
