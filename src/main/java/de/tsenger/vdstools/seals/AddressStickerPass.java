package de.tsenger.vdstools.seals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.DerTlv;

/**
 * @author Tobias Senger
 *
 */
public class AddressStickerPass extends DigitalSeal {

	public AddressStickerPass(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
		super(vdsHeader, vdsMessage, vdsSignature);
		parseDerTlvList(vdsMessage.getDerTlvList());
	}

	private void parseDerTlvList(List<DerTlv> tlvList) {
		for (DerTlv tlv : tlvList) {
			switch (tlv.getTag()) {
			case 0x01:
				String docNr = DataParser.decodeC40(tlv.getValue());
				featureMap.put(Feature.DOCUMENT_NUMBER, docNr);
				break;
			case 0x02:
				String ags = DataParser.decodeC40(tlv.getValue());
				featureMap.put(Feature.AGS, ags);
				break;
			case 0x03:
				String postalCode = DataParser.decodeC40(tlv.getValue());
				featureMap.put(Feature.POSTAL_CODE, postalCode);
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
			case DOCUMENT_NUMBER:
				String valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				derTlvList.add(new DerTlv((byte) (0x01), DataEncoder.encodeC40(valueStr)));
				break;
			case AGS:
				valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				derTlvList.add(new DerTlv((byte) (0x02), DataEncoder.encodeC40(valueStr)));
				break;
			case POSTAL_CODE:
				valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				derTlvList.add(new DerTlv((byte) (0x03), DataEncoder.encodeC40(valueStr)));
				break;
			default:
				Logger.warn("Feature " + entry.getKey().toString() + " is not supported in ResidencePermit.");
			}
		}
		return derTlvList;
	}

}
