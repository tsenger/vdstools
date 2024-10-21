package de.tsenger.vdstools.vds.seals;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.DerTlv;
import de.tsenger.vdstools.vds.Feature;
import de.tsenger.vdstools.vds.VdsHeader;
import de.tsenger.vdstools.vds.VdsMessage;
import de.tsenger.vdstools.vds.VdsSignature;

/**
 * @author Tobias Senger
 *
 */
public class SocialInsuranceCard extends DigitalSeal {

	public SocialInsuranceCard(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
		super(vdsHeader, vdsMessage, vdsSignature);
		parseDerTlvList(vdsMessage.getDerTlvList());
	}

	private void parseDerTlvList(List<DerTlv> tlvList) throws IllegalArgumentException {
		for (DerTlv tlv : tlvList) {
			switch (tlv.getTag()) {
			case 0x01:
				String socialInsuranceNumber = DataParser.decodeC40(tlv.getValue());
				featureMap.put(Feature.SOCIAL_INSURANCE_NUMBER, socialInsuranceNumber);
				break;
			case 0x02:
				String surName = new String(tlv.getValue(), StandardCharsets.UTF_8);
				featureMap.put(Feature.SURNAME, surName);
				break;
			case 0x03:
				String firstName = new String(tlv.getValue(), StandardCharsets.UTF_8);
				featureMap.put(Feature.FIRST_NAME, firstName);
				break;
			case 0x04:
				String birthName = new String(tlv.getValue(), StandardCharsets.UTF_8);
				featureMap.put(Feature.BIRTH_NAME, birthName);
				break;
			default:
				Logger.warn("found unknown tag: 0x" + String.format("%02X ", tlv.getTag()));
			}
		}
	}

	public static List<DerTlv> parseFeatures(Map<Feature, Object> featureMap) throws UnsupportedEncodingException {
		ArrayList<DerTlv> derTlvList = new ArrayList<DerTlv>(featureMap.size());
		for (Entry<Feature, Object> entry : featureMap.entrySet()) {
			switch (entry.getKey()) {
			case SOCIAL_INSURANCE_NUMBER:
				String valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				derTlvList.add(new DerTlv((byte) (0x01), DataEncoder.encodeC40(valueStr)));
				break;
			case SURNAME:
				valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				derTlvList.add(new DerTlv((byte) (0x02), valueStr.getBytes("UTF-8")));
				break;
			case FIRST_NAME:
				valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				derTlvList.add(new DerTlv((byte) (0x03), valueStr.getBytes("UTF-8")));
				break;
			case BIRTH_NAME:
				valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				derTlvList.add(new DerTlv((byte) (0x04), valueStr.getBytes("UTF-8")));
				break;
			default:
				Logger.warn("Feature " + entry.getKey().toString() + " is not supported in ResidencePermit.");
			}
		}
		return derTlvList;
	}
}
