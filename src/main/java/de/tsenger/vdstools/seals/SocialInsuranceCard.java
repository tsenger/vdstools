package de.tsenger.vdstools.seals;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.tinylog.Logger;

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
}
