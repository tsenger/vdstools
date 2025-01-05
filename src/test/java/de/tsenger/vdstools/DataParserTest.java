package de.tsenger.vdstools;

import de.tsenger.vdstools.vds.VdsHeader;
import de.tsenger.vdstools.vds.VdsRawBytes;
import okio.Buffer;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

public class DataParserTest {

    @Test
    public void testDecodeMaskedDate1() {
        String decodedDate = DataParser.decodeMaskedDate(Hex.decode("c3002e7c"));
        assertEquals("19xx-xx-01", decodedDate);
    }

    @Test
    public void testDecodeMaskedDate2() {
        String decodedDate = DataParser.decodeMaskedDate(Hex.decode("313d10da"));
        assertEquals("201x-04-xx", decodedDate);
    }

    @Test
    public void testDecodeMaskedDate3() {
        String decodedDate = DataParser.decodeMaskedDate(Hex.decode("f000076c"));
        assertEquals("1900-xx-xx", decodedDate);
    }

    @Test
    public void testDecodeMaskedDate4() {
        String decodedDate = DataParser.decodeMaskedDate(Hex.decode("00bbddbf"));
        assertEquals("1999-12-31", decodedDate);
    }

    @Test
    public void testDecodeMaskedDate5() {
        String decodedDate = DataParser.decodeMaskedDate(Hex.decode("ff000000"));
        assertEquals("xxxx-xx-xx", decodedDate);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeMaskedDate6_invalidFormat() {
        String decodedDate = DataParser.decodeMaskedDate(Hex.decode("ff0000"));
        assertNull(decodedDate);
    }

    @Test
    public void testDecodeDateTime1() {
        LocalDateTime localDateTime = DataParser.decodeDateTime(Hex.decode("0aecc4c7fb80"));
        System.out.println(localDateTime);
        assertTrue(LocalDateTime.parse("2030-12-01T00:00:00").isEqual(localDateTime));
    }

    @Test
    public void testDecodeDateTime2() {
        LocalDateTime localDateTime = DataParser.decodeDateTime(Hex.decode("02f527bf25b2"));
        System.out.println(localDateTime);
        assertTrue(LocalDateTime.parse("1957-03-25T08:15:22").isEqual(localDateTime));
    }

    @Test
    public void testDecodeDateTime3() {
        LocalDateTime localDateTime = DataParser.decodeDateTime(Hex.decode("00eb28c03640"));
        System.out.println(localDateTime);
        assertTrue(LocalDateTime.parse("0001-01-01T00:00:00").isEqual(localDateTime));
    }

    @Test
    public void testDecodeDateTime4() {
        LocalDateTime localDateTime = DataParser.decodeDateTime(Hex.decode("0b34792d9777"));
        System.out.println(localDateTime);
        assertTrue(LocalDateTime.parse("9999-12-31T23:59:59").isEqual(localDateTime));
    }

    @Test
    public void testDecodeHeader() {
        Buffer bb = new Buffer().write(VdsRawBytes.residentPermit);
        VdsHeader vdsHeader = VdsHeader.fromBuffer(bb);
        assertEquals(0x03, vdsHeader.getRawVersion());
        assertEquals("UTO", vdsHeader.getIssuingCountry());
        assertEquals("UTTS", vdsHeader.getSignerIdentifier());
        assertEquals("5B", vdsHeader.getCertificateReference());
        assertEquals("UTTS5B", vdsHeader.getSignerCertRef());
        assertEquals(LocalDate.parse("2020-01-01").toString(), vdsHeader.getIssuingDate().toString());
        assertEquals(LocalDate.parse("2023-07-26").toString(), vdsHeader.getSigDate().toString());
        assertEquals(0xfb, vdsHeader.getDocFeatureRef() & 0xff);
        assertEquals(0x06, vdsHeader.getDocTypeCat() & 0xff);
    }

    @Test
    public void testUnzip() throws IOException {
        byte[] compressedBytes = Hex.decode(
                "78da014e00b1ff61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c743d4280b");
        byte[] decompressedBytes = DataParser.unzip(compressedBytes);
        System.out.println("Decompressed: " + Hex.toHexString(decompressedBytes));
        assertEquals(
                "61120410b0b1b2b3b4b5b6b7b8b9babbbcbdbebf7f3824bbbb332f562a94f487db623b8db55c4a65b9cf532a959843a6a34e117f56343a94d5e187f28262943d84579af46d44804cf6328fa523c7",
                Hex.toHexString(decompressedBytes));
    }

}