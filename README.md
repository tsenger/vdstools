# VdsTools - lib to parse and verify Visible Digital Seals


A Java library to parse and verify visible digital seals (VDS) as specified in
- [BSI TR-03137 Part1](https://www.bsi.bund.de/EN/Themen/Unternehmen-und-Organisationen/Standards-und-Zertifizierung/Technische-Richtlinien/TR-nach-Thema-sortiert/tr03137/tr-03137.html)
- [ICAO TR "VDS for Non-Electronic Documents"](https://www.icao.int/Security/FAL/TRIP/Documents/TR%20-%20Visible%20Digital%20Seals%20for%20Non-Electronic%20Documents%20V1.7.pdf)

See [DataParserTest.java](https://github.com/tsenger/vdstools/blob/main/src/test/java/de/tsenger/vds_tools/DataParserTest.java) and [VerifierTest.java](https://github.com/tsenger/vdstools/blob/main/src/test/java/de/tsenger/vds_tools/VerifierTest.java) for a quick overview how to use the VDS parser and verifier.

Test VDS can be generated with help of the [Sealgen](https://sealgen.tsenger.de) tool. 
There is also the [Sealva](https://play.google.com/store/apps/details?id=de.tsenger.sealver) Android app which scans, verifies and displays all VDS profiles defined in the above specifications.

<a href='https://play.google.com/store/apps/details?id=de.tsenger.sealver&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width='155' height='60'/></a>

## Usage
[https://jitpack.io/#tsenger/vdstools](https://jitpack.io/#tsenger/vdstools)

[![Release](https://jitpack.io/v/tsenger/vdstools.svg)](https://jitpack.io/#tsenger/vdstools)

To use this library in your Maven build add:

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
        <version>0.1</version>
    </dependency>
```
## Documentation
JavaDoc can be found here:

[https://javadoc.jitpack.io/com/github/tsenger/vdstools/latest/javadoc/index.html](https://javadoc.jitpack.io/com/github/tsenger/vdstools/latest/javadoc/index.html)