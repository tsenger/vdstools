package de.tsenger.vdstools.seals;

import java.util.List;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataParser;

/**
 * @author Tobias Senger
 *
 */
public class AddressStickerPass extends DigitalSeal {

    public AddressStickerPass(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
    }

    private void parseDocumentFeatures(List<DocumentFeatureDto> features) {
        for (DocumentFeatureDto feature : features) {
            switch (feature.getTag()) {
            case 0x01:
                String docNr = DataParser.decodeC40(feature.getValue());
                featureMap.put(Feature.DOCUMENT_NUMBER, docNr);
                break;
            case 0x02:
                String ags = DataParser.decodeC40(feature.getValue());
                featureMap.put(Feature.AGS, ags);
                break;
            case 0x03:
                String postalCode = DataParser.decodeC40(feature.getValue());
                featureMap.put(Feature.POSTAL_CODE, postalCode);
                break;
            default:
                Logger.warn("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }

    }

}
