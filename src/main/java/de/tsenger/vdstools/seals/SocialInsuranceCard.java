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
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
    }

    private void parseDocumentFeatures(List<DocumentFeatureDto> features) throws IllegalArgumentException {
        for (DocumentFeatureDto feature : features) {
            switch (feature.getTag()) {
            case 0x01:
                String socialInsuranceNumber = DataParser.decodeC40(feature.getValue());
                featureMap.put(Feature.SOCIAL_INSURANCE_NUMBER, socialInsuranceNumber);
                break;
            case 0x02:
                String surName = new String(feature.getValue(), StandardCharsets.UTF_8);
                featureMap.put(Feature.SURNAME, surName);
                break;
            case 0x03:
                String firstName = new String(feature.getValue(), StandardCharsets.UTF_8);
                featureMap.put(Feature.FIRST_NAME, firstName);
                break;
            case 0x04:
                String birthName = new String(feature.getValue(), StandardCharsets.UTF_8);
                featureMap.put(Feature.BIRTH_NAME, birthName);
                break;
            default:
                Logger.warn("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }

    }
}
