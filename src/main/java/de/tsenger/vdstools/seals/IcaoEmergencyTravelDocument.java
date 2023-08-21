package de.tsenger.vdstools.seals;

import java.util.List;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataParser;

/**
 * @author Tobias Senger
 *
 */
public class IcaoEmergencyTravelDocument extends DigitalSeal {

    public IcaoEmergencyTravelDocument(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
    }

    private void parseDocumentFeatures(List<DocumentFeatureDto> features) {
        for (DocumentFeatureDto feature : features) {
            if (feature.getTag() == 0x02) {
                String mrz = DataParser.decodeC40(feature.getValue()).replace(' ', '<');
                featureMap.put(Feature.MRZ, mrz);
            } else {
                Logger.warn("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }
    }

}
