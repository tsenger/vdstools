package de.tsenger.vdstools.seals;

import java.util.ArrayList;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataParser;

/**
 * @author Tobias Senger
 *
 */
public class AliensLaw extends DigitalSeal {

    public AliensLaw(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseMessageTlvList(vdsMessage.getMessageTlvList());
    }

    private void parseMessageTlvList(ArrayList<MessageTlv> tlvList) {
        for (MessageTlv tlv : tlvList) {
            switch (tlv.getTag()) {
            case 0x01:
                byte[] faceImage = tlv.getValue();
                featureMap.put(Feature.FACE_IMAGE, faceImage);
                break;
            case 0x02:
                String short_mrz = DataParser.decodeC40(tlv.getValue()).replace(' ', '<');
                String mrz = String.format("%1$-72s", short_mrz).replace(' ', '<');
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

}
