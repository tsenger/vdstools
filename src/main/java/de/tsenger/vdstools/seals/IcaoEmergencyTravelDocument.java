package de.tsenger.vdstools.seals;

import java.util.List;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataParser;

/**
 * Created by Tobias Senger on 04.11.2019.
 */

public class IcaoEmergencyTravelDocument extends DigitalSeal {

    private String mrz;
    private static final int MRZ_CHARS_PER_LINE = 36;

    public IcaoEmergencyTravelDocument(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
        featureMap.put(Feature.MRZ, mrz);
    }

    private void parseDocumentFeatures(List<DocumentFeature> features) {
        for (DocumentFeature feature : features) {
            if (feature.getTag() == 0x02) {
                mrz = DataParser.decodeC40(feature.getValue()).replace(' ', '<');

                Logger.debug("Decoded MRZ: " + mrz);
            } else {
                Logger.info("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
                throw new IllegalArgumentException("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }
    }

}
