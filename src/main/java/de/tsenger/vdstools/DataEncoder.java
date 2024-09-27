package de.tsenger.vdstools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.util.Arrays;
import org.tinylog.Logger;

import de.tsenger.vdstools.seals.VdsHeader;
import de.tsenger.vdstools.seals.VdsMessage;
import de.tsenger.vdstools.seals.VdsSignature;

public class DataEncoder {

	private VdsHeader vdsHeader;
	private VdsMessage vdsMessage;
	private VdsSignature vdsSignature;


	public DataEncoder(VdsHeader vdsHeader, VdsMessage vdsMessage, Signer signer) {
		this.vdsHeader = vdsHeader;
		this.vdsMessage = vdsMessage;
		this.vdsSignature = createVdsSignature(vdsHeader, vdsMessage, signer);
	}


	private VdsSignature createVdsSignature(VdsHeader vdsHeader, VdsMessage vdsMessage, Signer signer) {
		byte[] headerMessage = Arrays.concatenate(vdsHeader.getRawBytes(), vdsMessage.getRawBytes());
		try {
			byte[] signatureBytes = signer.sign(headerMessage);
			return new VdsSignature(buildTLVStructure((byte) 0xff, signatureBytes));
		} catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException
				| InvalidAlgorithmParameterException | NoSuchProviderException | IOException e) {
			Logger.error("Signature creation failed: "+e.getMessage());
			return null;
		}
	}
	
	public byte[] getEncodedBytes() {
		return Arrays.concatenate(vdsHeader.getRawBytes(), vdsMessage.getRawBytes(), vdsSignature.getRawSignatureBytes());
	}
	
	/**
	 * wraps the given data (Value) in a TLV object with free choice of the tag
	 * Length will be calculated as defined in ASN.1 DER length encoding
	 * 
	 * @param tag   Tag
	 * @param value Value
	 * @return value with added tag and length
	 * @throws IOException
	 */
	public static byte[] buildTLVStructure(byte tag, byte[] value) throws IOException {
		DEROctetString dos = new DEROctetString(value);
		byte[] encodeBytes = dos.getEncoded();
		encodeBytes[0] = tag;
		return encodeBytes;
	}

	/**
	 * Builds the signer certificate reference based on the the given X.509
	 * certificate signer certificate reference is C + CN + serial number for
	 * version 0x02 or C + CN + len(serial number) + serial number for versions 0x03
	 * Serial number value will be encoded as hexstring
	 * 
	 * @param cert X509 certificate to get the signer information from
	 * @param rawVersion value to use for the encoding of the return value, use raw version number here.
	 * 			0x02 for version 3 and 0x03 for version 4
	 * @return String that contains the signerCertRef build as described above
	 * @throws InvalidNameException
	 */
	public static String getSignerCertRef(X509Certificate cert, byte rawVersion) throws InvalidNameException {
		String signerCertRef = "";
		LdapName ln = new LdapName(cert.getSubjectDN().getName());

		String c = "";
		String cn = "";
		for (Rdn rdn : ln.getRdns()) {
			if (rdn.getType().equalsIgnoreCase("CN")) {
				cn = (String) rdn.getValue();
				Logger.debug("CN is: " + cn);
			} else if (rdn.getType().equalsIgnoreCase("C")) {
				c = (String) rdn.getValue();
				Logger.debug("C is: " + c);
			}
		}
		String sn = cert.getSerialNumber().toString(16); // Serial Number as Hex
		switch (rawVersion) {
		case 0x03:
			signerCertRef = String.format("%s%s%02x%s", c, cn, sn.length(), sn).toUpperCase();
			break;
		case 0x02:
			signerCertRef = String.format("%s%s%5s", c, cn, sn).toUpperCase().replace(' ', '0');
			break;
		default:
			throw new IllegalArgumentException("unknown seal raw version value: " + rawVersion);
		}

		Logger.info("generated signerCertRef: " + signerCertRef);
		return signerCertRef;
	}

	/**
	 * @param dateString Date as String formated as yyyy-MM-dd
	 * @return date encoded in 3 bytes
	 * @throws ParseException
	 */
	public static byte[] encodeDate(String dateString) throws ParseException {
		LocalDate dt = LocalDate.parse(dateString);
		return encodeDate(dt);
	}

	/**
	 * @param localDate Date
	 * @return date encoded in 3 bytes
	 * @throws ParseException
	 */
	public static byte[] encodeDate(LocalDate localDate) {
		DateTimeFormatter pattern = DateTimeFormatter.ofPattern("MMddyyyy");
		String formattedDate = localDate.format(pattern);
		int dateInt = Integer.parseInt(formattedDate);
		return new byte[] { (byte) (dateInt >>> 16), (byte) (dateInt >>> 8), (byte) dateInt };

	}

	public static byte[] encodeC40(String dataString) {
		int c1, c2, c3, sum;
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		dataString = dataString.toUpperCase().replaceAll("<", " ");

		int len = dataString.length();

		for (int i = 0; i < len; i++) {
			if (i % 3 == 0) {
				if (i + 2 < len) {
					// encode standard way
					c1 = getC40Value(dataString.charAt(i));
					c2 = getC40Value(dataString.charAt(i + 1));
					c3 = getC40Value(dataString.charAt(i + 2));
					sum = (1600 * c1) + (40 * c2) + c3 + 1;
					out.write(sum / 256);
					out.write(sum % 256);
				} else if (i + 1 < len) {
					// use zero (Shift1) als filler symbol for c3
					c1 = getC40Value(dataString.charAt(i));
					c2 = getC40Value(dataString.charAt(i + 1));
					sum = (1600 * c1) + (40 * c2) + 1;
					out.write(sum / 256);
					out.write(sum % 256);
				} else {
					// two missing chars: add 0xFE (254 = unlatch) and encode as ASCII
					// (in datamatrix standard, actual encoded value is ASCII value + 1)
					out.write(254);
					out.write(toUnsignedInt((byte) dataString.charAt(i)) + 1);
				}
			}
		}
		return out.toByteArray();
	}

	private static int getC40Value(char c) {
		int value = toUnsignedInt((byte) c);
		if (value == 32) {
			return 3;
		} else if (value >= 48 && value <= 57) {
			return value - 44;
		} else if (value >= 65 && value <= 90) {
			return value - 51;
		} else {
			throw new IllegalArgumentException("Not a C40 encodable char: " + c + "value: " + value);
		}
	}

	public static int toUnsignedInt(byte value) {
		return (value & 0x7F) + (value < 0 ? 128 : 0);
	}

}
