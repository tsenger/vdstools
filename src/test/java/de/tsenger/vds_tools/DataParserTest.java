package de.tsenger.vds_tools;


import junit.framework.TestCase;

import org.bouncycastle.util.encoders.Hex;

import java.nio.ByteBuffer;
import java.time.LocalDate;

import de.tsenger.vds_tools.seals.ArrivalAttestation;
import de.tsenger.vds_tools.seals.DigitalSeal;
import de.tsenger.vds_tools.seals.IcaoVisa;
import de.tsenger.vds_tools.seals.VdsHeader;

public class DataParserTest extends TestCase {

    byte[] residentPermit_rawBytes = Hex.decode("DC03D9C5D9CAC8A73A990F71346ECF47\n" +
            "FB0602305CBA135875976EC066D417B5\n" +
            "9E8C6ABC133C133C133C133C3FEF3A29\n" +
            "38EE43F1593D1AE52DBB26751FE64B7C\n" +
            "133C136B0306D79519A65306FF408B7F\n" +
            "3B5F9A83FDD4F46EC7DCCC3384BB6C54\n" +
            "0AAF52603CC66D1F08B7F5E71243475D\n" +
            "0A833B51FD2A846622E847B1F3791803\n" +
            "F26D734B9BD18178FA22CFF2A31A");

    byte[] addressStickerId_rawBytes = Hex.decode("DC03D9C56D32C8A72CB16ECF476ECF47\n" +
            "F9080106CF3519AF974C02061A702085\n" +
            "19A1030E395E463E740C749FAD19D31E\n" +
            "FE32FF388F16D14E828A86E0F8E31DDF\n" +
            "13CC2CB3E0D8F5E4706562C9503D7326\n" +
            "AD6C0FA84154607D70975E5B7DFB2E36\n" +
            "197988ECE64345D37AD9B97C");

    byte[] socialInsurance_rawBytes = Hex.decode("DC02D9C56D32C8A519FC0F71346F1D67\n" +
            "FC0401083FEE456D2DE019A8020B5065\n" +
            "72736368776569C39F03054F73636172\n" +
            "04134AC3A2636F62C3A96E6964696374\n" +
            "7572697573FF401DCE81E863B01CFFE5\n" +
            "B099A5BBFCA60730EC9E090A1C82FA00\n" +
            "580EB592A9FC921D5F02CE8D1EC4E3AA\n" +
            "3CB4CEA3AFEF1C382B44ED8DA7105372\n" +
            "FC1D2E8D91A393");

    byte[] arrivalAttestationV02_rawBytes = Hex.decode("DC02D9C56D32C8A51A540F71346F1D67\n" +
            "FD020230A56213535BD4CAECC87CA4CC\n" +
            "AEB4133C133C133C133C133C3FEF3A29\n" +
            "38EE43F1593D1AE52DBB26751FE64B7C\n" +
            "133C136B030859E9203833736D24FF40\n" +
            "77B2FEC8EF9EF10C0D38A7D2A579EBB9\n" +
            "F80212EB06EDD7B1DC29889A6B735B7E\n" +
            "A1D7D78FF60D2AECB87B0247628C3211\n" +
            "9BA335B6BD87A7E07333C83ED16B091F");

    public void testDecodeHeader_ResidentPermit() {
        ByteBuffer bb = ByteBuffer.wrap(residentPermit_rawBytes);
        VdsHeader vdsHeader = DataParser.decodeHeader(bb);
        assertEquals(0x03, vdsHeader.rawVersion);
        assertEquals("UTO", vdsHeader.issuingCountry);
        assertEquals("UTTS", vdsHeader.signerIdentifier);
        assertEquals("5B", vdsHeader.certificateReference);
        assertEquals(LocalDate.parse("2020-01-01"), vdsHeader.issuingDate);
        assertEquals(LocalDate.parse("2023-07-26"), vdsHeader.sigDate);
        assertEquals(0xfb, vdsHeader.docFeatureRef & 0xff);
        assertEquals(0x06, vdsHeader.docTypeCat & 0xff);
    }

    public void testDecodeHeader_AddressStickerId() {
        ByteBuffer bb = ByteBuffer.wrap(addressStickerId_rawBytes);
        VdsHeader vdsHeader = DataParser.decodeHeader(bb);
        assertEquals(0x03, vdsHeader.rawVersion);
        assertEquals("UTO", vdsHeader.issuingCountry);
        assertEquals("DETS", vdsHeader.signerIdentifier);
        assertEquals("32", vdsHeader.certificateReference);
        assertEquals(LocalDate.parse("2023-07-26"), vdsHeader.issuingDate);
        assertEquals(LocalDate.parse("2023-07-26"), vdsHeader.sigDate);
        assertEquals(0xf9, vdsHeader.docFeatureRef & 0xff);
        assertEquals(0x08, vdsHeader.docTypeCat & 0xff);
        System.out.println(Hex.toHexString(vdsHeader.rawBytes));
    }

    public void testParseVdsSeal_SocialInsurranceCard() {
        DigitalSeal seal = DataParser.parseVdsSeal(socialInsurance_rawBytes);
        seal.getDocumentFeatures().forEach(System.out::println);
    }

    public void testParseVdsSeal_ArrivalAttestationV02() {
        DigitalSeal seal = DataParser.parseVdsSeal(arrivalAttestationV02_rawBytes);
        seal.getDocumentFeatures().forEach(System.out::println);
    }

    public void testGetFeature_ArrivalAttestationV02() {
        DigitalSeal seal = DataParser.parseVdsSeal(arrivalAttestationV02_rawBytes);
        assertEquals("ABC123456DEF", seal.getFeature(ArrivalAttestation.Feature.AZR));
    }

    public void testGetFeature_Null_ArrivalAttestationV02() {
        DigitalSeal seal = DataParser.parseVdsSeal(arrivalAttestationV02_rawBytes);
        assertEquals(null, seal.getFeature(IcaoVisa.Feature.MRZ));
    }

    public void testParseVdsSeal_AddressStickerId() {
        DigitalSeal seal = DataParser.parseVdsSeal(addressStickerId_rawBytes);
        assertEquals("ADDRESS_STICKER_ID", seal.getVdsType().toString());
        assertEquals("UTO", seal.getIssuingCountry());
        assertEquals("DETS", seal.getSignerIdentifier());
        assertEquals("32", seal.getCertSerialNumber().toString(16));
        assertEquals("2023-07-26", seal.getIssuingDate().toString());
        assertEquals("2023-07-26", seal.getSigDate().toString());
        seal.getDocumentFeatures().forEach(System.out::println);

    }

}