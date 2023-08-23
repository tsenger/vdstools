# VdsTools - lib to parse and verify Visible Digital Seals

A Java library to parse and verify visible digital seals (VDS) as specified in
- [BSI TR-03137 Part 1](https://www.bsi.bund.de/EN/Themen/Unternehmen-und-Organisationen/Standards-und-Zertifizierung/Technische-Richtlinien/TR-nach-Thema-sortiert/tr03137/tr-03137.html)
- [ICAO Doc 9303 Part 13: Visible Digital Seals](https://www.icao.int/publications/Documents/9303_p13_cons_en.pdf)
- [ICAO TR "VDS for Non-Electronic Documents"](https://www.icao.int/Security/FAL/TRIP/Documents/TR%20-%20Visible%20Digital%20Seals%20for%20Non-Electronic%20Documents%20V1.7.pdf)



Test VDS can be generated with help of the [Sealgen](https://sealgen.tsenger.de) tool. 
There is also the [Sealva](https://play.google.com/store/apps/details?id=de.tsenger.sealver) Android app which scans, verifies and displays all VDS profiles defined in the above specifications.

<a href='https://play.google.com/store/apps/details?id=de.tsenger.sealver&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width='155' height='60'/></a>

## How to use

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

Also have a look at [DataParserTest.java](https://github.com/tsenger/vdstools/blob/main/src/test/java/de/tsenger/vds_tools/DataParserTest.java) and [VerifierTest.java](https://github.com/tsenger/vdstools/blob/main/src/test/java/de/tsenger/vds_tools/VerifierTest.java) for some more examples.

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
	implementation 'com.github.tsenger:vdstools:0.2.1'
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
	<version>0.2.1</version>
</dependency>
```
