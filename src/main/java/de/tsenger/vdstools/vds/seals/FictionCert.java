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
public class FictionCert extends DigitalSeal {

	public FictionCert(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
		super(vdsHeader, vdsMessage, vdsSignature);
		parseDerTlvList(vdsMessage.getDerTlvList());
	}

	private void parseDerTlvList(List<DerTlv> tlvList) {
		for (DerTlv tlv : tlvList) {
			switch (tlv.getTag()) {
			case 0x01:
				byte[] faceImage = tlv.getValue();
				featureMap.put(Feature.FACE_IMAGE, faceImage);
				break;
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
			case 0x04:
				String azr = DataParser.decodeC40(tlv.getValue());
				featureMap.put(Feature.AZR, azr);
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
			case FACE_IMAGE:
				derTlvList.add(new DerTlv((byte) (0x01), (byte[]) entry.getValue()));
				break;
			case MRZ:
				String valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				derTlvList.add(new DerTlv((byte) (0x02), DataEncoder.encodeC40(valueStr)));
				break;
			case PASSPORT_NUMBER:
				valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				derTlvList.add(new DerTlv((byte) (0x03), DataEncoder.encodeC40(valueStr)));
				break;
			case AZR:
				valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				derTlvList.add(new DerTlv((byte) (0x04), DataEncoder.encodeC40(valueStr)));
				break;
			default:
				Logger.warn("Feature " + entry.getKey().toString() + " is not supported in ResidencePermit.");
			}
		}
		return derTlvList;
	}

}
