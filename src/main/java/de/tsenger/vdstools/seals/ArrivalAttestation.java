package de.tsenger.vdstools.seals;

import java.util.ArrayList;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataParser;

/**
 * @author Tobias Senger
 *
 */
public class ArrivalAttestation extends DigitalSeal {

    public ArrivalAttestation(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
    }

    private void parseDocumentFeatures(ArrayList<DocumentFeatureDto> features) {
        for (DocumentFeatureDto feature : features) {
            switch (feature.getTag()) {
            case 0x02:
                String mrz = DataParser.decodeC40(feature.getValue()).replace(' ', '<');
                featureMap.put(Feature.MRZ, mrz);
                break;
            case 0x03:
                String azr = DataParser.decodeC40(feature.getValue());
                featureMap.put(Feature.AZR, azr);
                break;
            default:
                Logger.warn("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }
    }

}
