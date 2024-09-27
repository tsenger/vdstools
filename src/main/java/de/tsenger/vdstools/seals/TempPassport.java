package de.tsenger.vdstools.seals;

import java.util.ArrayList;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataParser;

/**
 * @author Tobias Senger
 *
 */
public class TempPassport extends DigitalSeal {

    public TempPassport(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
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
                String mrz = DataParser.decodeC40(tlv.getValue()).replace(' ', '<');
                StringBuilder sb = new StringBuilder(mrz);
                sb.insert(44, '\n');
                featureMap.put(Feature.MRZ, sb.toString());
                break;
            default:
                Logger.warn("found unknown tag: 0x" + String.format("%02X ", tlv.getTag()));
            }
        }
    }

}
