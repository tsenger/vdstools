package de.tsenger.vds_tools.seals;

import java.util.ArrayList;

import org.tinylog.Logger;

import de.tsenger.vds_tools.DataParser;

/**
 * Created by Tobias Senger on 12.01.2017.
 */
public class ArrivalAttestation extends DigitalSeal {

    private String mrz = "";
    private String azr = "";

    public ArrivalAttestation(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
        featureMap.put(Feature.MRZ, mrz);
        featureMap.put(Feature.AZR, azr);
    }

    private void parseDocumentFeatures(ArrayList<DocumentFeature> features) {
        for (DocumentFeature feature : features) {
            if (feature.getTag() == 0x02) {
                mrz = DataParser.decodeC40(feature.getValue()).replace(' ', '<');
            } else if (feature.getTag() == 0x03) {
                azr = DataParser.decodeC40(feature.getValue());
            } else {
                Logger.info("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
                throw new IllegalArgumentException("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }
    }

}
