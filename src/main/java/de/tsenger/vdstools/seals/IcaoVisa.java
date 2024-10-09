package de.tsenger.vdstools.seals;

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
public class IcaoVisa extends DigitalSeal {

    public IcaoVisa(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseMessageTlvList(vdsMessage.getMessageTlvList());
    }

    private void parseMessageTlvList(List<MessageTlv> tlvList) {
        for (MessageTlv tlv : tlvList) {
            switch (tlv.getTag()) {
            case 0x01:
                // MRZ chars per line: 44
                String short_mrz = DataParser.decodeC40(tlv.getValue()).replace(' ', '<');
                // fill mrz to the full length of 88 characters because ICAO cuts last 16
                // characters
                String mrz = String.format("%1$-88s", short_mrz).replace(' ', '<');
                StringBuilder sb = new StringBuilder(mrz);
                sb.insert(44, '\n');
                featureMap.put(Feature.MRZ, sb.toString());
                break;
            case 0x02:
                // MRZ chars per line: 36
                short_mrz = DataParser.decodeC40(tlv.getValue()).replace(' ', '<');
                // fill mrz to the full length of 72 characters because ICAO cuts last 8
                // characters
                mrz = String.format("%1$-72s", short_mrz).replace(' ', '<');
                sb = new StringBuilder(mrz);
                sb.insert(36, '\n');
                featureMap.put(Feature.MRZ, sb.toString());
                break;
            case 0x03:
                int numberOfEntries = tlv.getValue()[0] & 0xff;
                featureMap.put(Feature.NUMBER_OF_ENTRIES, numberOfEntries);
                break;
            case 0x04:
                decodeDuration(tlv.getValue());
                break;
            case 0x05:
                String passportNumber = DataParser.decodeC40(tlv.getValue());
                featureMap.put(Feature.PASSPORT_NUMBER, passportNumber);
                break;
            case 0x06:
                byte[] visaType = tlv.getValue();
                featureMap.put(Feature.VISA_TYPE, visaType);
                break;
            case 0x07:
                byte[] additionalFeatures = tlv.getValue();
                featureMap.put(Feature.ADDITIONAL_FEATURES, additionalFeatures);
                break;
            default:
                Logger.warn("found unknown tag: 0x" + String.format("%02X ", tlv.getTag()));
            }
        }
    }

    private void decodeDuration(byte[] bytes) {
        if (bytes.length != 3)
            throw new IllegalArgumentException("expected three bytes for date decoding");
        int durationOfStay_days = bytes[0] & 0xff;
        int durationOfStay_months = bytes[1] & 0xff;
        int durationOfStay_years = bytes[2] & 0xff;

        featureMap.put(Feature.DURATION_OF_STAY_YEARS, durationOfStay_years);
        featureMap.put(Feature.DURATION_OF_STAY_MONTHS, durationOfStay_months);
        featureMap.put(Feature.DURATION_OF_STAY_DAYS, durationOfStay_days);
    }
    
    public static List<MessageTlv> parseFeatures(Map<Feature, Object> featureMap) {
		ArrayList<MessageTlv> messageTlvList = new ArrayList<MessageTlv>(featureMap.size());
		byte[] durationBytes = new byte[3];
		for (Entry<Feature, Object> entry : featureMap.entrySet()) {
			switch (entry.getKey()) {
			case MRZ:
				String valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				byte[] valueBytes = DataEncoder.encodeC40(valueStr);
				messageTlvList.add(new MessageTlv((byte) (0x02), valueBytes.length, valueBytes));
				break;
			case NUMBER_OF_ENTRIES:
				valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				valueBytes = DataEncoder.encodeC40(valueStr);
				messageTlvList.add(new MessageTlv((byte) (0x03), valueBytes.length, valueBytes));
				break;				
				
			case DURATION_OF_STAY_DAYS:
				durationBytes[0] = (byte) ((byte)entry.getValue() & 0xff);		
				break;
			case DURATION_OF_STAY_MONTHS:
				durationBytes[1] = (byte) ((byte)entry.getValue() & 0xff);		
				break;
			case DURATION_OF_STAY_YEARS:
				durationBytes[2] = (byte) ((byte)entry.getValue() & 0xff);	
				break;
			case DURATION_OF_STAY_RAWBYTES:
				durationBytes = (byte[]) entry.getValue();
				break;
				
			case PASSPORT_NUMBER:
				valueStr = ((String) entry.getValue()).replaceAll("\r", "").replaceAll("\n", "");
				valueBytes = DataEncoder.encodeC40(valueStr);
				messageTlvList.add(new MessageTlv((byte) (0x05), valueBytes.length, valueBytes));
				break;				
			case VISA_TYPE:
				valueBytes = (byte[]) entry.getValue();
				messageTlvList.add(new MessageTlv((byte) (0x06), valueBytes.length, valueBytes));
				break;
			case ADDITIONAL_FEATURES:
				valueBytes = (byte[]) entry.getValue();
				messageTlvList.add(new MessageTlv((byte) (0x07), valueBytes.length, valueBytes));
				break;
			default:
				Logger.warn("Feature " + entry.getKey().toString() + " is not supported in ResidencePermit.");
			}
		}
		// Build duration of stays at last to be sure days, months and years are processed before.
		messageTlvList.add(new MessageTlv((byte) (0x04), durationBytes.length, durationBytes));
		
		return messageTlvList;
    }

}
