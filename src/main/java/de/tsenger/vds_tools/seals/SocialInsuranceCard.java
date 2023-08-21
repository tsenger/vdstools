package de.tsenger.vds_tools.seals;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.tinylog.Logger;

import de.tsenger.vds_tools.DataParser;

/**
 * Created by Tobias Senger on 12.01.2017.
 */

public class SocialInsuranceCard extends DigitalSeal {

    private String socialInsuranceNumber = "";
    private String surName = "";
    private String firstName = "";
    private String birthName = "**********";

    public SocialInsuranceCard(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
        featureMap.put(Feature.SOCIAL_INSURANCE_NUMBER, socialInsuranceNumber);
        featureMap.put(Feature.SURNAME, surName);
        featureMap.put(Feature.FIRST_NAME, firstName);
        featureMap.put(Feature.BIRTH_NAME, birthName);
    }

    private void parseDocumentFeatures(List<DocumentFeature> features) throws IllegalArgumentException {
        for (DocumentFeature feature : features) {
            switch (feature.getTag()) {
            case 0x01:
                socialInsuranceNumber = DataParser.decodeC40(feature.getValue());
                break;
            case 0x02:
                surName = new String(feature.getValue(), StandardCharsets.UTF_8);
                break;
            case 0x03:
                firstName = new String(feature.getValue(), StandardCharsets.UTF_8);
                break;
            case 0x04:
                birthName = new String(feature.getValue(), StandardCharsets.UTF_8);
                break;
            default:
                Logger.info("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
                throw new IllegalArgumentException("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }

    }
}
