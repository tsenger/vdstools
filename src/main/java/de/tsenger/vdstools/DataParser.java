package de.tsenger.vdstools;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import org.bouncycastle.util.encoders.Hex;
import org.tinylog.Logger;

import de.tsenger.vdstools.seals.AddressStickerIdCard;
import de.tsenger.vdstools.seals.AddressStickerPass;
import de.tsenger.vdstools.seals.AliensLaw;
import de.tsenger.vdstools.seals.ArrivalAttestation;
import de.tsenger.vdstools.seals.DigitalSeal;
import de.tsenger.vdstools.seals.FictionCert;
import de.tsenger.vdstools.seals.IcaoEmergencyTravelDocument;
import de.tsenger.vdstools.seals.IcaoVisa;
import de.tsenger.vdstools.seals.ResidencePermit;
import de.tsenger.vdstools.seals.SocialInsuranceCard;
import de.tsenger.vdstools.seals.SupplementarySheet;
import de.tsenger.vdstools.seals.TempPassport;
import de.tsenger.vdstools.seals.TempPerso;
import de.tsenger.vdstools.seals.VdsHeader;
import de.tsenger.vdstools.seals.VdsMessage;
import de.tsenger.vdstools.seals.VdsSignature;
import de.tsenger.vdstools.seals.VdsType;

/**
 * Created by Tobias Senger on 18.01.2017.
 */

public class DataParser {

	public static DigitalSeal parseVdsSeal(String rawString) {
		byte[] rawBytes = decodeBase256(rawString);
		Logger.trace("rawString: {}", rawString);
		return parseVdsSeal(rawBytes);
	}

	public static DigitalSeal parseVdsSeal(byte[] rawBytes) {

		ByteBuffer rawData = ByteBuffer.wrap(rawBytes);
		Logger.trace("rawData: {}", () -> Hex.toHexString(rawBytes));

		VdsHeader vdsHeader = decodeHeader(rawData);
		VdsMessage vdsMessage = new VdsMessage(vdsHeader.getVdsType());
		VdsSignature vdsSignature = null;

		int messageStartPosition = rawData.position();
		int signatureStartPosition = 0;
		byte[] vdsMessageRawDataBytes = null;

		// TODO find "cleaner" solution, duplicated in IdbMessageGroup
		while (rawData.hasRemaining()) {
			int tag = (rawData.get() & 0xff);
			if (tag == 0xff) {
				signatureStartPosition = rawData.position() - 1;
			}
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
			byte[] val = getFromByteBuffer(rawData, le);
			// Tag 0xFF marks the Signature
			if (tag == 0xff) {
				vdsSignature = new VdsSignature(val);
				vdsMessageRawDataBytes = Arrays.copyOfRange(rawData.array(), messageStartPosition,
						signatureStartPosition);
				break;
			}
			vdsMessage.addDerTlv(new DerTlv((byte) (tag & 0xff), val));
		}

		// Test if message raw bytes are equal to the calculate raw bytes from
		// vdsMessage.getRawBytes method
		if (!Arrays.equals(vdsMessageRawDataBytes, vdsMessage.getRawBytes())) {
			Logger.error(
					"Message raw bytes and calculated message raw bytes from vdsMessage.getRawBytes are not equal!");
			Logger.debug("Expected: " + Hex.toHexString(vdsMessageRawDataBytes) + " Actual: "
					+ Hex.toHexString(vdsMessage.getRawBytes()));
			return null;
		}

		VdsType vdsType = VdsType.valueOf(vdsHeader.getDocumentRef());
		switch (vdsType) {
		case ARRIVAL_ATTESTATION:
			return new ArrivalAttestation(vdsHeader, vdsMessage, vdsSignature);
		case SOCIAL_INSURANCE_CARD:
			return new SocialInsuranceCard(vdsHeader, vdsMessage, vdsSignature);
		case ICAO_VISA:
			return new IcaoVisa(vdsHeader, vdsMessage, vdsSignature);
		case RESIDENCE_PERMIT:
			return new ResidencePermit(vdsHeader, vdsMessage, vdsSignature);
		case ICAO_EMERGENCY_TRAVEL_DOCUMENT:
			return new IcaoEmergencyTravelDocument(vdsHeader, vdsMessage, vdsSignature);
		case SUPPLEMENTARY_SHEET:
			return new SupplementarySheet(vdsHeader, vdsMessage, vdsSignature);
		case ADDRESS_STICKER_ID:
			return new AddressStickerIdCard(vdsHeader, vdsMessage, vdsSignature);
		case ADDRESS_STICKER_PASSPORT:
			return new AddressStickerPass(vdsHeader, vdsMessage, vdsSignature);
		case ALIENS_LAW:
			return new AliensLaw(vdsHeader, vdsMessage, vdsSignature);
		case TEMP_PASSPORT:
			return new TempPassport(vdsHeader, vdsMessage, vdsSignature);
		case TEMP_PERSO:
			return new TempPerso(vdsHeader, vdsMessage, vdsSignature);
		case FICTION_CERT:
			return new FictionCert(vdsHeader, vdsMessage, vdsSignature);
		default:
			Logger.warn("unknown VDS type with reference: {}", String.format("0x%02X", vdsHeader.getDocumentRef()));
			return null;
		}
	}

	public static VdsHeader decodeHeader(ByteBuffer rawdata) {
		// Magic Byte
		int magicByte = rawdata.get();
		if (magicByte != (byte) 0xdc) {
			Logger.error(String.format("Magic Constant mismatch: 0x%02X instead of 0xdc", magicByte));
			throw new IllegalArgumentException(
					String.format("Magic Constant mismatch: 0x%02X instead of 0xdc", magicByte));
		}

		VdsHeader vdsHeader = new VdsHeader();

		vdsHeader.rawVersion = rawdata.get();
		/*
		 * new in ICAO spec for "Visual Digital Seals for Non-Electronic Documents":
		 * value 0x02 stands for version 3 (uses fix length of Document Signer
		 * Reference: 5 characters) value 0x03 stands for version 4 (uses variable
		 * length of Document Signer Reference) Problem: German "Arrival Attestation
		 * Document" uses value 0x03 for rawVersion 3 and static length of Document
		 * Signer Reference.
		 */
		if (!(vdsHeader.rawVersion == 0x02 || vdsHeader.rawVersion == 0x03)) {
			Logger.error(String.format("Unsupported rawVersion: 0x%02X", vdsHeader.rawVersion));
			throw new IllegalArgumentException(String.format("Unsupported rawVersion: 0x%02X", vdsHeader.rawVersion));
		}
		vdsHeader.issuingCountry = decodeC40(getFromByteBuffer(rawdata, 2)); // 2 bytes stores the three letter country
																				// code
		rawdata.mark();

		// 4 bytes stores first 6 characters of Signer & Certificate Reference
		String signerIdentifierAndCertRefLength = decodeC40(getFromByteBuffer(rawdata, 4));
		vdsHeader.signerIdentifier = signerIdentifierAndCertRefLength.substring(0, 4);

		if (vdsHeader.rawVersion == 0x03) { // ICAO version 4
			// the last two characters store the length of the following Certificate
			// Reference
			int certRefLength = Integer.parseInt(signerIdentifierAndCertRefLength.substring(4), 16);
			Logger.debug("version 4: certRefLength: {}", certRefLength);

			/*
			 * GAAD HACK: If signer is DEME and rawVersion is 0x03 (which is version 4
			 * according to ICAO spec) then anyhow use fixed size certification reference
			 * length and the length characters also used as certificate reference. eg.
			 * DEME03123 signerIdenfifier = DEME length of certificate reference: 03 certRef
			 * = 03123 <-see: here the length is part of the certificate reference which is
			 * not the case in all other seals except the German
			 * "Arrival Attestation Document"
			 */
			boolean gaadHack = (vdsHeader.signerIdentifier.equals("DEME") || vdsHeader.signerIdentifier.equals("DES1"));
			if (gaadHack) {
				Logger.debug("Maybe we found a German Arrival Attestation. GAAD Hack will be applied!");
				certRefLength = 3;
			}
			// get number of bytes we have to decode to get the given certification
			// reference length
			int bytesToDecode = ((certRefLength - 1) / 3 * 2) + 2;
			Logger.debug("version 4: bytesToDecode: {}", bytesToDecode);
			vdsHeader.certificateReference = decodeC40(getFromByteBuffer(rawdata, bytesToDecode));
			if (gaadHack) {
				vdsHeader.certificateReference = signerIdentifierAndCertRefLength.substring(4)
						+ vdsHeader.certificateReference;
			}
		} else { // rawVersion=0x02 -> ICAO version 3
			rawdata.reset();
			String signerCertRef = decodeC40(getFromByteBuffer(rawdata, 6));
			vdsHeader.certificateReference = signerCertRef.substring(4);
		}

		vdsHeader.issuingDate = decodeDate(getFromByteBuffer(rawdata, 3));
		vdsHeader.sigDate = decodeDate(getFromByteBuffer(rawdata, 3));
		vdsHeader.docFeatureRef = rawdata.get();
		vdsHeader.docTypeCat = rawdata.get();
//        vdsHeader.setRawBytes(Arrays.copyOfRange(rawdata.array(), 0, rawdata.position()));
		Logger.debug("VdsHeader: {}", vdsHeader);
		return vdsHeader;
	}

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
	 * @param maskedDateBytes byte array that contains a encoded masked date
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
		String formattedDateString = dateString.replaceAll("(.{2})(.{2})(.{4})", "$3-$1-$2").toLowerCase();
		return formattedDateString;
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
		LocalDateTime localDateTime = LocalDateTime.parse(String.format("%014d", dateBigInt), pattern);
		return localDateTime;
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
}
