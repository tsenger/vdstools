package de.tsenger.vds_tools.seals;

import java.util.List;

import org.tinylog.Logger;

import de.tsenger.vds_tools.DataParser;

/**
 * Created by Tobias Senger on 08.11.2019
 */
public class ResidencePermit extends DigitalSeal {

    private String mrz = "";
    private String passportNumber = "";

    public ResidencePermit(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
        featureMap.put(Feature.MRZ, mrz);
        featureMap.put(Feature.PASSPORT_NUMBER, passportNumber);
    }

    private void parseDocumentFeatures(List<DocumentFeature> features) {
        for (DocumentFeature feature : features) {
            if (feature.getTag() == 0x02) {
                mrz = DataParser.decodeC40(feature.getValue()).replace(' ', '<');
                Logger.debug("Decoded MRZ: " + mrz);
            } else if (feature.getTag() == 0x03) {
                passportNumber = DataParser.decodeC40(feature.getValue());
            } else {
                Logger.info("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
                throw new IllegalArgumentException("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }

    }

}
