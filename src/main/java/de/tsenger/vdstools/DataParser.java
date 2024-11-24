package de.tsenger.vdstools;

import org.tinylog.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterOutputStream;

/**
 * Created by Tobias Senger on 18.01.2017.
 */

public class DataParser {

	/**
	 * Returns a byte array of the requested size which contains the number of bytes
	 * from the given ByteBuffer beginning at the current pointer of the ByteBuffer.
	 * 
	 * @param buffer The ByteBuffer to get the number of bytes from.
	 * @param size   Number of bytes to get from ByteBuffer. Starting from the
	 *               internal ByteBuffers pointer
	 * @return byte array of length 'size' with bytes from ByteBuffer
	 */
	public static byte[] getFromByteBuffer(ByteBuffer buffer, int size) {
		byte[] tmpByteArray = new byte[size];
		if (buffer.position() + size <= buffer.capacity()) {
			buffer.get(tmpByteArray);
		}
		return tmpByteArray;
	}

	/**
	 * Decodes a byte[] encoded masked date as described in ICAO TR "Datastructure
	 * for Barcode". Returns a date string in format yyyy-MM-dd where unknown parts
	 * of the date are marked with an 'x'. e.g. 19xx-10-xx
	 * 
	 * @param maskedDateBytes byte array that contains an encoded masked date
	 * @return date string where unknown parts of the date are marked with an 'x'
	 */
	public static String decodeMaskedDate(byte[] maskedDateBytes) throws IllegalArgumentException {
		if (maskedDateBytes.length != 4) {
			throw new IllegalArgumentException("expected four bytes for masked date decoding");
		}
		byte mask = maskedDateBytes[0];
		long intval = (long) toUnsignedInt(maskedDateBytes[1]) * 256 * 256 + toUnsignedInt(maskedDateBytes[2]) * 256L
				+ toUnsignedInt(maskedDateBytes[3]);
		int day = (int) ((intval % 1000000) / 10000);
		int month = (int) (intval / 1000000);
		int year = (int) (intval % 10000);
		// MMddyyyy
		char[] dateCharArray = String.format("%02d%02d%04d", month, day, year).toCharArray();

		for (int i = 0; i < 8; i++) {
			byte unknownBit = (byte) ((mask >> (7 - i)) & 1);
			if (unknownBit == 1) {
				dateCharArray[i] = 'x';
			}
		}
		String dateString = String.valueOf(dateCharArray);
        return dateString.replaceAll("(.{2})(.{2})(.{4})", "$3-$1-$2").toLowerCase();
	}

	public static LocalDate decodeDate(byte[] dateBytes) {
		if (dateBytes.length != 3) {
			throw new IllegalArgumentException("expected three bytes for date decoding");
		}

		long intval = (long) toUnsignedInt(dateBytes[0]) * 256 * 256 + toUnsignedInt(dateBytes[1]) * 256L
				+ toUnsignedInt(dateBytes[2]);
		int day = (int) ((intval % 1000000) / 10000);
		int month = (int) (intval / 1000000);
		int year = (int) (intval % 10000);

		return LocalDate.of(year, month, day);
	}

	/**
	 * Decodes a byte[] encoded datetime as described in ICAO TR "Datastructure for
	 * Barcode". Returns a LocalDateTime object
	 * 
	 * @param dateTimeBytes byte array with length 6 which contains encoded datetime
	 * @return LocalDateTime object
	 */
	public static LocalDateTime decodeDateTime(byte[] dateTimeBytes) {
		if (dateTimeBytes.length != 6) {
			throw new IllegalArgumentException("expected three bytes for date decoding");
		}
		BigInteger dateBigInt = new BigInteger(dateTimeBytes);
		DateTimeFormatter pattern = DateTimeFormatter.ofPattern("MMddyyyyHHmmss");
        return LocalDateTime.parse(String.format("%014d", dateBigInt), pattern);
	}

	public static List<DerTlv> parseDerTLvs(byte[] rawBytes) {
		ByteBuffer rawData = ByteBuffer.wrap(rawBytes);
		List<DerTlv> derTlvList = new ArrayList<>();
		while (rawData.hasRemaining()) {
			byte tag = rawData.get();

			int le = rawData.get() & 0xff;
			if (le == 0x81) {
				le = rawData.get() & 0xff;
			} else if (le == 0x82) {
				le = ((rawData.get() & 0xff) * 0x100) + (rawData.get() & 0xff);
			} else if (le == 0x83) {
				le = ((rawData.get() & 0xff) * 0x1000) + ((rawData.get() & 0xff) * 0x100) + (rawData.get() & 0xff);
			} else if (le > 0x7F) {
				Logger.error(String.format("can't decode length: 0x%02X", le));
				throw new IllegalArgumentException(String.format("can't decode length: 0x%02X", le));
			}
			byte[] val = DataParser.getFromByteBuffer(rawData, le);
			derTlvList.add(new DerTlv(tag, val));
		}
		return derTlvList;
	}

	private static int toUnsignedInt(byte value) {
		return (value & 0x7F) + (value < 0 ? 128 : 0);
	}

	public static String decodeC40(byte[] bytes) {
		StringBuilder sb = new StringBuilder();

		for (int idx = 0; idx < bytes.length; idx++) {
			if (idx % 2 == 0) {
				byte i1 = bytes[idx];
				byte i2 = bytes[idx + 1];

				if (i1 == (byte) 0xFE) {
					sb.append((char) (i2 - 1));
				} else {
					int v16 = (toUnsignedInt(i1) << 8) + toUnsignedInt(i2) - 1;
					int temp = v16 / 1600;
					int u1 = temp;
					v16 -= temp * 1600;
					temp = v16 / 40;
					int u2 = temp;
					int u3 = v16 - temp * 40;

					if (u1 != 0) {
						sb.append(toChar(u1));
					}
					if (u2 != 0) {
						sb.append(toChar(u2));
					}
					if (u3 != 0) {
						sb.append(toChar(u3));
					}
				}
			}
		}
		return sb.toString();
	}

	private static char toChar(int intValue) {
		if (intValue == 3) {
			return (char) 32;
		} else if (intValue >= 4 && intValue <= 13) {
			return (char) (intValue + 44);
		} else if (intValue >= 14 && intValue <= 39) {
			return (char) (intValue + 51);
		}

		// if character is unknown return "?"
		return (char) 63;
	}

	public static byte[] decodeBase256(String s) {
		char[] ca = s.toCharArray();
		byte[] ba = new byte[ca.length];
		for (int i = 0; i < ba.length; i++) {
			ba[i] = (byte) ca[i];
		}
		return ba;
	}

	public static byte[] unzip(byte[] bytesToDecompress) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		InflaterOutputStream infos = new InflaterOutputStream(bos);
		infos.write(bytesToDecompress);
		infos.finish();
		byte[] decompressedBytes = bos.toByteArray();
		bos.close();
		infos.close();
		return decompressedBytes;
	}
}
