package de.tsenger.vdstools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.util.Arrays;
import org.tinylog.Logger;

import de.tsenger.vdstools.seals.AddressStickerIdCard;
import de.tsenger.vdstools.seals.AddressStickerPass;
import de.tsenger.vdstools.seals.AliensLaw;
import de.tsenger.vdstools.seals.ArrivalAttestation;
import de.tsenger.vdstools.seals.DigitalSeal;
import de.tsenger.vdstools.seals.Feature;
import de.tsenger.vdstools.seals.FictionCert;
import de.tsenger.vdstools.seals.IcaoEmergencyTravelDocument;
import de.tsenger.vdstools.seals.IcaoVisa;
import de.tsenger.vdstools.seals.MessageTlv;
import de.tsenger.vdstools.seals.ResidencePermit;
import de.tsenger.vdstools.seals.SocialInsuranceCard;
import de.tsenger.vdstools.seals.SupplementarySheet;
import de.tsenger.vdstools.seals.TempPassport;
import de.tsenger.vdstools.seals.TempPerso;
import de.tsenger.vdstools.seals.VdsHeader;
import de.tsenger.vdstools.seals.VdsMessage;
import de.tsenger.vdstools.seals.VdsSignature;
import de.tsenger.vdstools.seals.VdsType;

public class DataEncoder {
	
	
	public static DigitalSeal buildDigitalSeal(VdsType vdsType, Map<Feature, Object> featureMap, X509Certificate cert, Signer signer) {
		VdsHeader vdsHeader = buildHeader(vdsType, cert);
		VdsMessage vdsMessage = buildVdsMessage(vdsType, featureMap);
		VdsSignature vdsSignature = createVdsSignature(vdsHeader, vdsMessage, signer);
		return new DigitalSeal(vdsHeader, vdsMessage, vdsSignature);
	}
	
	public static DigitalSeal buildDigitalSeal(VdsHeader vdsHeader, Map<Feature, Object> featureMap, Signer signer) {
		VdsMessage vdsMessage = buildVdsMessage(vdsHeader.getVdsType(), featureMap);
		VdsSignature vdsSignature = createVdsSignature(vdsHeader, vdsMessage, signer);
		return new DigitalSeal(vdsHeader, vdsMessage, vdsSignature);
	}

	public static VdsSignature createVdsSignature(VdsHeader vdsHeader, VdsMessage vdsMessage, Signer signer) {
		byte[] headerMessage = Arrays.concatenate(vdsHeader.getRawBytes(), vdsMessage.getRawBytes());
		try {
			byte[] signatureBytes = signer.sign(headerMessage);
			return new VdsSignature(signatureBytes);
		} catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException
				| InvalidAlgorithmParameterException | NoSuchProviderException | IOException e) {
			Logger.error("Signature creation failed: "+e.getMessage());
			return null;
		}
	}
	
	public static VdsMessage buildVdsMessage(VdsType vdsType, Map<Feature, Object> featureMap) {
		List<MessageTlv> messageTlvList= null;
		switch (vdsType) {
		case RESIDENCE_PERMIT:
			 messageTlvList =  ResidencePermit.parseFeatures(featureMap);
			 break;
		case ADDRESS_STICKER_ID:
			 messageTlvList =  AddressStickerIdCard.parseFeatures(featureMap);
			 break;
		case ADDRESS_STICKER_PASSPORT:
			 messageTlvList =  AddressStickerPass.parseFeatures(featureMap);
			 break;
		case ALIENS_LAW:
			 messageTlvList =  AliensLaw.parseFeatures(featureMap);
			 break;
		case ARRIVAL_ATTESTATION:
			 messageTlvList =  ArrivalAttestation.parseFeatures(featureMap);
			 break;
		case FICTION_CERT:
			 messageTlvList =  FictionCert.parseFeatures(featureMap);
			 break;
		case ICAO_EMERGENCY_TRAVEL_DOCUMENT:
			 messageTlvList =  IcaoEmergencyTravelDocument.parseFeatures(featureMap);
			 break;
		case ICAO_VISA:
			 messageTlvList =  IcaoVisa.parseFeatures(featureMap);
			 break;
		case SOCIAL_INSURANCE_CARD:
			 try {
				messageTlvList =  SocialInsuranceCard.parseFeatures(featureMap);
			} catch (UnsupportedEncodingException e) {
				Logger.error("Couldn't build VdsMessage for SocialInsuranceCard: "+e.getMessage());
				return null;
			}
			 break;	
		case SUPPLEMENTARY_SHEET:
			 messageTlvList =  SupplementarySheet.parseFeatures(featureMap);
			 break;	 
		case TEMP_PASSPORT:
			 messageTlvList =  TempPassport.parseFeatures(featureMap);
			 break;
		case TEMP_PERSO:
			 messageTlvList =  TempPerso.parseFeatures(featureMap);
			 break;
		default:
			break;
		}		
		VdsMessage vdsMessage = new VdsMessage(messageTlvList);		
		return vdsMessage;
	}

	public static VdsHeader buildHeader(VdsType vdsType, X509Certificate cert) {
		return buildHeader(vdsType, cert, null, (byte) 0x03, LocalDate.now());
	}
	
	public static VdsHeader buildHeader(VdsType vdsType, X509Certificate cert, String issuingCountry) {
		return buildHeader(vdsType, cert, issuingCountry, (byte) 0x03, LocalDate.now());
	}
	
	public static VdsHeader buildHeader(VdsType vdsType, X509Certificate cert, String issuingCountry, byte rawVersion, LocalDate issuingDate) {
		VdsHeader header = new VdsHeader();
		header.setDocumentType(vdsType);
		header.rawVersion = rawVersion;
		try {
			String signerCertRef[] = getSignerCertRef(cert);
			header.signerIdentifier = signerCertRef[0];
			header.certificateReference = signerCertRef[1];
		} catch (InvalidNameException e) {
			Logger.error("Couldn't build header, because getSignerCertRef throws error: " + e.getMessage());
		}
		
		if (issuingCountry != null) {
			header.issuingCountry = issuingCountry;
		} else {
			header.issuingCountry = Doc9303CountryCodes.convertToIcaoOrIso3(header.signerIdentifier.substring(0, 2));
		}
		
		header.issuingDate = issuingDate;
		return header;
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
	 * @return String array that contains the signerIdentifier at index 0 and CertRef at index 1
	 * @throws InvalidNameException
	 */
	public static String[] getSignerCertRef(X509Certificate cert) throws InvalidNameException {
		String signerCertRef[] = new String[2];
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
		signerCertRef[0] = String.format("%s%s", c, cn).toUpperCase();
		signerCertRef[1] = cert.getSerialNumber().toString(16); // Serial Number as Hex

		Logger.info("generated signerCertRef: " + signerCertRef[0] + signerCertRef[1]);
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
