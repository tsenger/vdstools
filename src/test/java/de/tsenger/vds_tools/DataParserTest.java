package de.tsenger.vds_tools;

import java.nio.ByteBuffer;
import java.time.LocalDate;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.seals.DigitalSeal;
import de.tsenger.vdstools.seals.Feature;
import de.tsenger.vdstools.seals.VdsHeader;
import junit.framework.TestCase;

public class DataParserTest extends TestCase {

  //@formatter:off
    static byte[] residentPermit_rawBytes = Hex.decode(
            "DC03D9C5D9CAC8A73A990F71346ECF47\n"
            + "FB0602305CBA135875976EC066D417B5\n" 
            + "9E8C6ABC133C133C133C133C3FEF3A29\n"
            + "38EE43F1593D1AE52DBB26751FE64B7C\n" 
            + "133C136B0306D79519A65306FF408B7F\n"
            + "3B5F9A83FDD4F46EC7DCCC3384BB6C54\n" 
            + "0AAF52603CC66D1F08B7F5E71243475D\n"
            + "0A833B51FD2A846622E847B1F3791803\n" 
            + "F26D734B9BD18178FA22CFF2A31A");

    static byte[] addressStickerId_rawBytes = Hex.decode(
            "DC03D9C56D32C8A72CB16ECF476ECF47\n"
            + "F9080106CF3519AF974C02061A702085\n" 
            + "19A1030E395E463E740C749FAD19D31E\n"
            + "FE32FF388F16D14E828A86E0F8E31DDF\n" 
            + "13CC2CB3E0D8F5E4706562C9503D7326\n"
            + "AD6C0FA84154607D70975E5B7DFB2E36\n" 
            + "197988ECE64345D37AD9B97C");

    static byte[] socialInsurance_rawBytes = Hex.decode(
            "DC02D9C56D32C8A519FC0F71346F1D67\n" 
            + "FC0401083FEE456D2DE019A8020B5065\n"
            + "72736368776569C39F03054F73636172\n" 
            + "04134AC3A2636F62C3A96E6964696374\n"
            + "7572697573FF401DCE81E863B01CFFE5\n" 
            + "B099A5BBFCA60730EC9E090A1C82FA00\n"
            + "580EB592A9FC921D5F02CE8D1EC4E3AA\n" 
            + "3CB4CEA3AFEF1C382B44ED8DA7105372\n" 
            + "FC1D2E8D91A393");

    static byte[] arrivalAttestationV02_rawBytes = Hex.decode(
            "DC02D9C56D32C8A51A540F71346F1D67\n"
            + "FD020230A56213535BD4CAECC87CA4CC\n" 
            + "AEB4133C133C133C133C133C3FEF3A29\n"
            + "38EE43F1593D1AE52DBB26751FE64B7C\n" 
            + "133C136B030859E9203833736D24FF40\n"
            + "77B2FEC8EF9EF10C0D38A7D2A579EBB9\n" 
            + "F80212EB06EDD7B1DC29889A6B735B7E\n"
            + "A1D7D78FF60D2AECB87B0247628C3211\n" 
            + "9BA335B6BD87A7E07333C83ED16B091F");

    static byte[] arrivalAttestation_rawBytes = Hex.decode(
            "dc026abc6d32c8a519fc0f71341145f4" +
                "fd020230a5621353d9a275735bd4134b" +
                "c549133c133c133c133c133ca32519a5" +
                "19a4344a5e681ae7204b20d532cf4b7c" +
                "133c133f030820d5201019a51aeaff40" +
                "4a1f218ca4392647ecff6c8abf9e796a" +
                "78eebe0b1ac8cc25c4ee17eed961d118" +
                "9091358d7d616f1a517abc747f6c4490" +
                "ff159d4dcf50248b00b1e32e9e7805e7");


    static byte[] visa_224bitSig_rawBytes = Hex.decode(
            "DC03D9C56D32C8A72CB10F71347D0017\n" 
            + "5D01022CDD52134A74DA1347C6FED95C\n"
            + "B89F9FCE133C133C133C133C20383373\n" 
            + "4AAF47F0C32F1A1E20EB2625393AFE31\n"
            + "0403A00000050633BE1FED20C6FF389F\n" 
            + "D029C66FB2E4BF361CDBFFD8F5931B62\n"
            + "59F645B077702C617F453D0B898A55E6\n" 
            + "E7870974FFE7B3AC416ACDE6B03B3C3A\n" 
            + "8CB5A22B456816");
    
    static byte[] supplementSheet_rawBytes = Hex.decode(
            "DC03D9C5D9CAC8A73A990F71347D4E37\n"
            + "FA0604305CBA135875976EC066D417B5\n"
            + "9E8C6ABC133C133C133C133C3FEF3A29\n"
            + "38EE43F1593D1AE52DBB26751FE64B7C\n"
            + "133C136B0506B77519A519AAFF4008F9\n"
            + "E9B4B79BE5703048A4879A4F420C433C\n"
            + "375295A355FB0D29DCBED211CF6F5F57\n"
            + "38BA2B74E2FE5F1D2D2021E054BFFD0E\n"
            + "4CE17D98E5BCED26A85C91C68B2F");
    
    static byte[] addressStickerPassport_rawBytes = Hex.decode(
            "DC03D9C5D9CAC8A73A990F71347D4E37\n"
            + "F80A0106B77A38E596CE02061A203A4D\n"
            + "1FE1030426532081FF4027436CE719F9\n"
            + "13CCD3EBFAEEAE175171450DB6CA1B62\n"
            + "FF188748834D2DC5299A5F418BE8D4DC\n"
            + "052E0536CB6DE711B4CC645651C6B0EA\n"
            + "FE5713E96290DC149169");
    
    static byte[] emergenyTravelDoc_rawBytes = Hex.decode(
            "DC03D9C5D9CAC8A73A990F71347D4E37\n"
            + "5E0302308A0D62B9D917A4CCA93CA4D0\n"
            + "EDFC133C133C133C133C133C3FEF3A29\n"
            + "38EE43F1593D1AE52DBB26751FE64B7C\n"
            + "133C136BFF4022F8BD19ECCBA4EF24F2\n"
            + "04787796DD914FEC61F605B153B22A6E\n"
            + "F307D3869938A4E7E908F0A63B837988\n"
            + "0B395C7FDBAC720D7F2836D08E1DA626\n"
            + "11614A00120B");


  //@formatter:on    

    @Test
    public void testDecodeHeader() {
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

    @Test
    public void testParseSocialInsurranceCard() {
        DigitalSeal seal = DataParser.parseVdsSeal(socialInsurance_rawBytes);
        assertEquals("65170839J003", seal.getFeature(Feature.SOCIAL_INSURANCE_NUMBER));
        assertEquals("Perschweiß", seal.getFeature(Feature.SURNAME));
        assertEquals("Oscar", seal.getFeature(Feature.FIRST_NAME));
        assertEquals("Jâcobénidicturius", seal.getFeature(Feature.BIRTH_NAME));
    }

    @Test
    public void testParseArrivalAttestationV02() {
        DigitalSeal seal = DataParser.parseVdsSeal(arrivalAttestationV02_rawBytes);
        assertEquals("MED<<MANNSENS<<MANNY<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
                seal.getFeature(Feature.MRZ));
        assertEquals("ABC123456DEF", seal.getFeature(Feature.AZR));
        assertEquals(null, seal.getFeature(Feature.FIRST_NAME));
    }
    
    @Test
    public void testParseResidentPermit() {
        DigitalSeal seal = DataParser.parseVdsSeal(residentPermit_rawBytes);
        assertEquals("ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
                seal.getFeature(Feature.MRZ));
        assertEquals("UFO001979", seal.getFeature(Feature.PASSPORT_NUMBER));
    }
    
    @Test
    public void testParseSupplementSheet() {
        DigitalSeal seal = DataParser.parseVdsSeal(supplementSheet_rawBytes);
        assertEquals("ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
                seal.getFeature(Feature.MRZ));
        assertEquals("PA0000005", seal.getFeature(Feature.SHEET_NUMBER));
    }
    
    @Test
    public void testEmergencyTravelDoc() {
        DigitalSeal seal = DataParser.parseVdsSeal(emergenyTravelDoc_rawBytes);
        assertEquals("I<GBRSUPAMANN<<MARY<<<<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06",
                seal.getFeature(Feature.MRZ));
    }

    @Test
    public void testParseAddressStickerId() {
        DigitalSeal seal = DataParser.parseVdsSeal(addressStickerId_rawBytes);
        assertEquals("T2000AK47", seal.getFeature(Feature.DOCUMENT_NUMBER));
        assertEquals("05314000", seal.getFeature(Feature.AGS));
        assertEquals("53175HEINEMANNSTR11", seal.getFeature(Feature.RAW_ADDRESS));
        assertEquals("53175", seal.getFeature(Feature.POSTAL_CODE));
        assertEquals("HEINEMANNSTR", seal.getFeature(Feature.STREET));
        assertEquals("11", seal.getFeature(Feature.STREET_NR));
    }
    
    @Test
    public void testParseAddressStickerPassport() {
        DigitalSeal seal = DataParser.parseVdsSeal(addressStickerPassport_rawBytes);
        assertEquals("PA5500K11", seal.getFeature(Feature.DOCUMENT_NUMBER));
        assertEquals("03359010", seal.getFeature(Feature.AGS));
        assertEquals("21614", seal.getFeature(Feature.POSTAL_CODE));
    }

    @Test
    public void testParseVisa() {
        DigitalSeal seal = DataParser.parseVdsSeal(visa_224bitSig_rawBytes);
        assertEquals("VCD<<DENT<<ARTHUR<PHILIP<<<<<<<<<<<<\n1234567XY7GBR5203116M2005250<<<<<<<<",
                seal.getFeature(Feature.MRZ));
        assertEquals("47110815P", seal.getFeature(Feature.PASSPORT_NUMBER));
        assertEquals(0, seal.getFeature(Feature.DURATION_OF_STAY_YEARS));
        assertEquals(0, seal.getFeature(Feature.DURATION_OF_STAY_MONTHS));
        assertEquals(160, seal.getFeature(Feature.DURATION_OF_STAY_DAYS));
    }

    @Test
    public void testFeatureMap() {
        DigitalSeal seal = DataParser.parseVdsSeal(visa_224bitSig_rawBytes);
        seal.getFeatureMap()
                .forEach((key, value) -> System.out.println(String.format("Key: %s, Value: %s", key, value)));
    }

}