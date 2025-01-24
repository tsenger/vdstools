# VdsTools - Kotlin multiplatform library to work with Visible Digital Seals

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)  ![Maven Central Version](https://img.shields.io/maven-central/v/de.tsenger/vdstools?color=green) ![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/tsenger/vdstools/maven.yml)

This a Kotlin multiplatform (JVM and iOS) library to decode/verify and encode/sign Visible Digital Seals (VDS) as
specified in

- [BSI TR-03137 Part 1](https://www.bsi.bund.de/EN/Themen/Unternehmen-und-Organisationen/Standards-und-Zertifizierung/Technische-Richtlinien/TR-nach-Thema-sortiert/tr03137/tr-03137.html)
- [ICAO Doc 9303 Part 13: Visible Digital Seals](https://www.icao.int/publications/Documents/9303_p13_cons_en.pdf)
- [ICAO TR "VDS for Non-Electronic Documents"](https://www.icao.int/Security/FAL/TRIP/Documents/TR%20-%20Visible%20Digital%20Seals%20for%20Non-Electronic%20Documents%20V1.7.pdf)

It also supports encoding and decoding Seals defined in the new draft
of [ICAO Datastructure for Barcode](https://www.icao.int/Security/FAL/TRIP/PublishingImages/Pages/Publications/ICAO%20TR%20-%20ICAO%20Datastructure%20for%20Barcode.pdf).
The IDB encoder/decoders are at early stadium of and still differs from the VDS parser/encoder but they are already
useable. You will find them in the
package de.tsenger.vdstools.idb.

VDS can be created with the help of this library or, if you want to try it out quickly, via the
web [Sealgen](https://sealgen.tsenger.de) tool.
There is also the [Sealva](https://play.google.com/store/apps/details?id=de.tsenger.sealver) Android app which scans,
verifies and displays all VDS profiles defined in the above specifications.

<a href='https://play.google.com/store/apps/details?id=de.tsenger.sealver&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width='155' height='60'/></a>

## Parse and verify a VDS

Here is a quick overview how to use the VDS parser and verifier.
When you have the decoded raw string or raw bytes from your favorite datamatrix decoder, just put them to the VDS Tools
Dataparser like this:

```kotlin
import de.tsenger.vdstools.Verifier
import de.tsenger.vdstools.vds.DigitalSeal
import de.tsenger.vdstools.vds.Feature

val digitalSeal: DigitalSeal? = DigitalSeal.fromByteArray(VdsRawBytesCommon.fictionCert)
assertNotNull(digitalSeal)
val vdsType: String = digitalSeal.vdsType

val mrz: String? = digitalSeal.getFeature("MRZ")?.valueStr
val azr: String? = digitalSeal.getFeature("AZR")?.valueStr
val imgBytes: ByteArray? = digitalSeal.getFeature("FACE_IMAGE")?.valueBytes
val imgBytesHexString: String? = digitalSeal.getFeature("FACE_IMAGE")?.valueStr


// get all available Features in a List<Feature>
val featureList: List<Feature> = digitalSeal.featureList

for (feature in featureList) {
    println("${feature.name}, ${feature.coding},  ${feature.valueStr}")
}

// Get the VDS signer certificate reference
val signerCertRef: String = digitalSeal.signerCertRef


// Since X509 certificate handling is strongly platform-dependent, 
// the Verfifier is given the raw PublicKey in DER format and the curve name.
val publicKeyBytes: ByteArray = byteArrayOf()
val verifier: Verifier = Verifier(digitalSeal, publicKeyBytes, "brainpoolP224r1")
val result: Verifier.Result = verifier.verify()


```

## Build a new VDS

Since version 0.3.0 you can also generate VDS with this library. Here is an example on how to use the DateEncoder and
Signer classes:

```kotlin
val keystore: KeyStore = ...


// In this JVM example we use a BouncyCastle keystore to get the certificate (for the header information)
// and the private key for signing the seals data
val cert: X509Certificate = keystore.getCertificate(keyAlias)
val ecKey: ECPrivateKey = keystore.getKey(certAlias, keyStorePassword.toCharArray())

// initialize the Signer
val signer: Signer = Signer(ecKey.encoded, curveName)

// 1. Build a VdsHeader
val header = VdsHeader.Builder("ARRIVAL_ATTESTATION")
    .setIssuingCountry("D<<")
    .setSignerIdentifier("DETS")
    .setCertificateReference("32")
    .setIssuingDate(LocalDate.parse("2024-09-27"))
    .setSigDate(LocalDate.parse("2024-09-27"))
    .build()

// 2. Build a VdsMessage
val mrz = "MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06"
val azr = "ABC123456DEF"
val vdsMessage = VdsMessage.Builder(header.vdsType)
    .addDocumentFeature("MRZ", mrz)
    .addDocumentFeature("AZR", azr)
    .build()

// 3. Build a signed DigitalSeal
val digitalSeal = DigitalSeal(header, vdsMessage, signer)

// The encoded bytes can now be used to build a datamatrix (or other) code - which is not part of this library
val encodedSealBytes = digitalSeal.encoded

```

There are many other ways to define the content of the VDS. There are various ways to encode a DigitalSeal by for this
purpose.
The VdsHeader and VdsMessage classes offer the option of setting the content in a finely granular manner.

Alternatively, it is also possible to generate many values automatically with as little input as possible or to use
default values.

Also have a look at the DigitalSeal testcases for more usage inspiration.
You will also find an example on how to generate a datamatrix image with the Zxing library in the jvmTests.

## Documentation

[![javadoc](https://javadoc.io/badge2/de.tsenger/vdstools/javadoc.svg)](https://javadoc.io/doc/de.tsenger/vdstools)

Online JavaDoc can be found here:
[https://javadoc.io/doc/de.tsenger/vdstools](https://javadoc.io/doc/de.tsenger/vdstools)

## How to include

The vdstools library is available on
the [Maven Central Repository](https://central.sonatype.com/artifact/de.tsenger/vdstools) and can be easily integrated
in
your projects.

### Gradle

To include this library to your Gradle build add this dependency:

```groovy
dependencies {
    implementation 'de.tsenger:vdstools:0.8.4'
}
```

### Maven

To include this library to your Maven build add this dependency:

```xml

<dependency>
    <groupId>de.tsenger</groupId>
    <artifactId>vdstools</artifactId>
    <version>0.8.4</version>
</dependency>
```
