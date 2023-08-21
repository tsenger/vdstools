package de.tsenger.vds_tools.seals;

import java.util.List;

import org.tinylog.Logger;

import de.tsenger.vds_tools.DataParser;

/**
 * Created by Tobias Senger on 08.11.2019
 */
public class SupplementarySheet extends DigitalSeal {

    private String mrz = "";
    private String suppSheetNumber = "";

    public SupplementarySheet(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
        featureMap.put(Feature.MRZ, mrz);
        featureMap.put(Feature.SHEET_NUMBER, suppSheetNumber);
    }

    private void parseDocumentFeatures(List<DocumentFeature> features) {
        for (DocumentFeature feature : features) {
            switch (feature.getTag()) {
            case 0x04:
                mrz = DataParser.decodeC40(feature.getValue()).replace(' ', '<');
                break;
            case 0x05:
                suppSheetNumber = DataParser.decodeC40(feature.getValue());
                break;
            default:
                Logger.info("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
                throw new IllegalArgumentException("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }
    }

}
