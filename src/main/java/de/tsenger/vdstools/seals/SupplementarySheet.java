package de.tsenger.vdstools.seals;

import java.util.List;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataParser;

/**
 * @author Tobias Senger
 *
 */
public class SupplementarySheet extends DigitalSeal {

    public SupplementarySheet(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
    }

    private void parseDocumentFeatures(List<DocumentFeatureDto> features) {
        for (DocumentFeatureDto feature : features) {
            switch (feature.getTag()) {
            case 0x04:
                String mrz = DataParser.decodeC40(feature.getValue()).replace(' ', '<');
                featureMap.put(Feature.MRZ, mrz);
                break;
            case 0x05:
                String suppSheetNumber = DataParser.decodeC40(feature.getValue());
                featureMap.put(Feature.SHEET_NUMBER, suppSheetNumber);
                break;
            default:
                Logger.warn("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }
    }

}
