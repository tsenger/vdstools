package de.tsenger.vdstools.seals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;

/**
 * @author Tobias Senger
 *
 */
public class ResidencePermit extends DigitalSeal {

	public ResidencePermit(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
		super(vdsHeader, vdsMessage, vdsSignature);
		parseMessageTlvList(vdsMessage.getMessageTlvList());
	}

	private void parseMessageTlvList(List<MessageTlv> tlvList) {
		for (MessageTlv tlv : tlvList) {
			switch (tlv.getTag()) {
			case 0x02:
				String mrz = DataParser.decodeC40(tlv.getValue()).replace(' ', '<');
				StringBuilder sb = new StringBuilder(mrz);
				sb.insert(36, '\n');
				featureMap.put(Feature.MRZ, sb.toString());
				break;
			case 0x03:
				String passportNumber = DataParser.decodeC40(tlv.getValue());
				featureMap.put(Feature.PASSPORT_NUMBER, passportNumber);
				break;
			default:
				Logger.warn("found unknown tag: 0x" + String.format("%02X ", tlv.getTag()));
			}
		}

	}
	
	// TODO do this in all other Seals too
	public static List<MessageTlv> parseFeatures(Map<Feature, Object> featureMap) {
		ArrayList<MessageTlv> messageTlvList = new ArrayList<MessageTlv>(2);
		for (var entry : featureMap.entrySet()) {
			switch (entry.getKey()) {
			case MRZ:
				String mrz = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				byte[] encodedMrz = DataEncoder.encodeC40(mrz);
				messageTlvList.add(new MessageTlv((byte) (0x02), encodedMrz.length, encodedMrz));
				break;
			case PASSPORT_NUMBER:
				String ppNo = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				byte[] encodedPpNo = DataEncoder.encodeC40(ppNo);
				messageTlvList.add(new MessageTlv((byte) (0x03), encodedPpNo.length, encodedPpNo));
				break;
			default:
				Logger.warn("Feature " + entry.getKey().toString() + " is not supported in ResidencePermit.");
			}
		}
		return messageTlvList;
	}

}
