package de.tsenger.vdstools.vds.seals;

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
public class ResidencePermit extends DigitalSeal {

	public ResidencePermit(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
		super(vdsHeader, vdsMessage, vdsSignature);
		parseMessageTlvList(vdsMessage.getDerTlvList());
	}

	private void parseMessageTlvList(List<DerTlv> tlvList) {
		for (DerTlv tlv : tlvList) {
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

	public static List<DerTlv> parseFeatures(Map<Feature, Object> featureMap) {
		ArrayList<DerTlv> derTlvList = new ArrayList<DerTlv>(featureMap.size());
		for (Entry<Feature, Object> entry : featureMap.entrySet()) {
			switch (entry.getKey()) {
			case MRZ:
				String mrz = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				derTlvList.add(new DerTlv((byte) (0x02), DataEncoder.encodeC40(mrz)));
				break;
			case PASSPORT_NUMBER:
				String ppNo = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				derTlvList.add(new DerTlv((byte) (0x03), DataEncoder.encodeC40(ppNo)));
				break;
			default:
				Logger.warn("Feature " + entry.getKey().toString() + " is not supported in ResidencePermit.");
			}
		}
		return derTlvList;
	}

}
