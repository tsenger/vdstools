# VdsTools - lib to decode/verify and encode/sign  Visible Digital Seals
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)  [![Release](https://jitpack.io/v/tsenger/vdstools.svg)](https://jitpack.io/#tsenger/vdstools)

A library to decode/verify and encode/sign Visible Digital Seals (VDS) as specified in
- [BSI TR-03137 Part 1](https://www.bsi.bund.de/EN/Themen/Unternehmen-und-Organisationen/Standards-und-Zertifizierung/Technische-Richtlinien/TR-nach-Thema-sortiert/tr03137/tr-03137.html)
- [ICAO Doc 9303 Part 13: Visible Digital Seals](https://www.icao.int/publications/Documents/9303_p13_cons_en.pdf)
- [ICAO TR "VDS for Non-Electronic Documents"](https://www.icao.int/Security/FAL/TRIP/Documents/TR%20-%20Visible%20Digital%20Seals%20for%20Non-Electronic%20Documents%20V1.7.pdf)


VDS can be created with the help of this library or, if you want to try it out quickly, via the web [Sealgen](https://sealgen.tsenger.de) tool. 
There is also the [Sealva](https://play.google.com/store/apps/details?id=de.tsenger.sealver) Android app which scans, verifies and displays all VDS profiles defined in the above specifications.

<a href='https://play.google.com/store/apps/details?id=de.tsenger.sealver&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width='155' height='60'/></a>

## Parse and verify a VDS
Here is a quick overview how to use the VDS parser and verifier.
When you have the decoded raw string or raw bytes from your favorite datamatrix decoder, just put them to the VDS Tools Dataparser like this:

```java
import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.Verifier;
import de.tsenger.vdstools.seals.DigitalSeal;
import de.tsenger.vdstools.seals.VdsType;
import de.tsenger.vdstools.seals.Feature;

	...
	DigitalSeal digitalSeal = DataParser.parseVdsSeal(rawBytes);
	VdsType vdsType = digitalSeal.getVdsType()
	
	// Depending on the returned VDS type you can access the seals content
	String mrz = (String) seal.getFeature(Feature.MRZ);
	String azr = (String) seal.getFeature(Feature.AZR);
   
	// Get the VDS signer certificate reference
	String signerCertRef = digitalSeal.getSignerCertRef();
   
	// Provide for the matching X509 signer certificate
	// and use this to verify the VDS signature   
	Verifier verifier = new Verifier(digitalSeal, x509SignerCert);
	Verifier.Result result = verifier.verify();
	
	
```

Also have a look at [DataParserTest.java](https://github.com/tsenger/vdstools/blob/main/src/test/java/de/tsenger/vdstools/DataParserTest.java) and [VerifierTest.java](https://github.com/tsenger/vdstools/blob/main/src/test/java/de/tsenger/vdstools/VerifierTest.java) for some more examples.

## Build a new VDS
Since version 0.3.0 you can also generate VDS with this library. Here is an example on how to use the DateEncoder and Signer classes:

```java
KeyStore keystore = ...
...
String mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06";
String passportNumber = "UFO001979";
VdsMessage vdsMessage = new VdsMessage(VdsType.RESIDENCE_PERMIT);
vdsMessage.addDocumentFeature(Feature.MRZ, mrz);
vdsMessage.addDocumentFeature(Feature.PASSPORT_NUMBER, passportNumber);

// Here we use a keystore to get the certificate (for the header information)
// and the private key for signing the seals data
X509Certificate cert = (X509Certificate) keystore.getCertificate(keyAlias);
ECPrivateKey ecKey = (ECPrivateKey) keystore.getKey(certAlias, keyStorePassword.toCharArray());

// initialize the Signer
Signer signer = new Signer(ecKey); 
	
// Build the the VDS
// Here the header information will be read from the certificate content and the message.
DigitalSeal digitalSeal = DataEncoder.buildDigitalSeal(vdsMessage, cert, signer);

// The encoded bytes can now be used to build a datamatrix (or other) code - which is not part of this library
byte[] encodedBytes = digitalSeal.getEncodedBytes();

```

There are many other ways to define the content of the VDS. In the example above, a lot of data such as the signature or issuing date is generated automatically. However, it is also possible to set your own values. There are various buildDigitalSeal methods in the DataEncoder for this purpose. The VdsHeader and VdsMessage classes offer the option of setting the content in a finely granular manner.
 
Alternatively, it is also possible to generate many values automatically with as little input as possible or to use default values.

Also have a look at [DataEncoderTest.java](https://github.com/tsenger/vdstools/blob/main/src/test/java/de/tsenger/vdstools/DataEncoderTest.java) for some examples how to use the different options. 
In [DataMatrixTest.java](https://github.com/tsenger/vdstools/blob/main/src/test/java/de/tsenger/vdstools/DataMatrixTest.java) you will find an example on how to generated a datamatrix image file from the encoded bytes of the DataEncoder.

## Documentation
JavaDoc can be found here:

[https://javadoc.jitpack.io/com/github/tsenger/vdstools/latest/javadoc/index.html](https://javadoc.jitpack.io/com/github/tsenger/vdstools/latest/javadoc/index.html)

## How to include
[https://jitpack.io/#tsenger/vdstools](https://jitpack.io/#tsenger/vdstools)

[![Release](https://jitpack.io/v/tsenger/vdstools.svg)](https://jitpack.io/#tsenger/vdstools)

### Gradle

To include this library to your Gradle build add:

```groovy
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

and the dependency:

```groovy
dependencies {
	implementation 'com.github.tsenger:vdstools:0.3.1'
}
```

### Maven

To include this library to your Maven build add:

```xml
<repositories>
	<repository>
		<id>jitpack.io</id>
		<url>https://jitpack.io</url>
	</repository>
</repositories>
```

and the dependency:

```xml
<dependency>
	<groupId>com.github.tsenger</groupId>
	<artifactId>vdstools</artifactId>
	<version>0.3.1</version>
</dependency>
```
