package de.tsenger.vdstools.seals;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;

/**
 * @author Tobias Senger
 *
 */
public class SocialInsuranceCard extends DigitalSeal {

    public SocialInsuranceCard(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseMessageTlvList(vdsMessage.getMessageTlvList());
    }

    private void parseMessageTlvList(List<MessageTlv> tlvList) throws IllegalArgumentException {
        for (MessageTlv tlv : tlvList) {
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
    
    public static List<MessageTlv> parseFeatures(Map<Feature, Object> featureMap) throws UnsupportedEncodingException {
		ArrayList<MessageTlv> messageTlvList = new ArrayList<MessageTlv>(featureMap.size());
		for (Entry<Feature, Object> entry : featureMap.entrySet()) {
			switch (entry.getKey()) {
			case SOCIAL_INSURANCE_NUMBER:
				String valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				byte[] valueBytes = DataEncoder.encodeC40(valueStr);
				messageTlvList.add(new MessageTlv((byte) (0x01), valueBytes.length, valueBytes));
				break;
			case SURNAME:
				valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				valueBytes = valueStr.getBytes("UTF-8");
				messageTlvList.add(new MessageTlv((byte) (0x02), valueBytes.length, valueBytes));
				break;
			case FIRST_NAME:
				valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				valueBytes = valueStr.getBytes("UTF-8");
				messageTlvList.add(new MessageTlv((byte) (0x03), valueBytes.length, valueBytes));
				break;	
			case BIRTH_NAME:
				valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				valueBytes = valueStr.getBytes("UTF-8");
				messageTlvList.add(new MessageTlv((byte) (0x04), valueBytes.length, valueBytes));
				break;
			default:
				Logger.warn("Feature " + entry.getKey().toString() + " is not supported in ResidencePermit.");
			}
		}
		return messageTlvList;
	}
}
