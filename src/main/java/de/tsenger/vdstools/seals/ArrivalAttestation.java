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
public class ArrivalAttestation extends DigitalSeal {

    public ArrivalAttestation(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseMessageTlvList(vdsMessage.getMessageTlvList());
    }

    private void parseMessageTlvList(ArrayList<MessageTlv> tlvList) {
        for (MessageTlv tlv : tlvList) {
            switch (tlv.getTag()) {
            case 0x02:
                String mrz = DataParser.decodeC40(tlv.getValue()).replace(' ', '<');
                StringBuilder sb = new StringBuilder(mrz);
                sb.insert(36, '\n');
                featureMap.put(Feature.MRZ, sb.toString());
                break;
            case 0x03:
                String azr = DataParser.decodeC40(tlv.getValue());
                featureMap.put(Feature.AZR, azr);
                break;
            default:
                Logger.warn("found unknown tag: 0x" + String.format("%02X ", tlv.getTag()));
            }
        }
    }
    
    public static List<MessageTlv> parseFeatures(Map<Feature, Object> featureMap) {
		ArrayList<MessageTlv> messageTlvList = new ArrayList<MessageTlv>(2);
		for (var entry : featureMap.entrySet()) {
			switch (entry.getKey()) {
			case MRZ:
				String valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				byte[] valueBytes = DataEncoder.encodeC40(valueStr);
				messageTlvList.add(new MessageTlv((byte) (0x02), valueBytes.length, valueBytes));
				break;
			case AZR:
				valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				valueBytes = DataEncoder.encodeC40(valueStr);
				messageTlvList.add(new MessageTlv((byte) (0x03), valueBytes.length, valueBytes));
				break;
			default:
				Logger.warn("Feature " + entry.getKey().toString() + " is not supported in ResidencePermit.");
			}
		}
		return messageTlvList;
    }

}
