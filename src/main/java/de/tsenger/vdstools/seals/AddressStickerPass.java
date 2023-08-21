package de.tsenger.vdstools.seals;

import java.util.List;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataParser;

public class AddressStickerPass extends DigitalSeal {

    private String docNr = "";
    private String ags = "";
    private String postalCode = "";

    public AddressStickerPass(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
        featureMap.put(Feature.DOCUMENT_NUMBER, docNr);
        featureMap.put(Feature.AGS, ags);
        featureMap.put(Feature.POSTAL_CODE, postalCode);
    }

    private void parseDocumentFeatures(List<DocumentFeature> features) {
        for (DocumentFeature feature : features) {
            if (feature.getTag() == 0x01) {
                docNr = DataParser.decodeC40(feature.getValue());
                Logger.debug("Decoded document number: " + docNr);
            } else if (feature.getTag() == 0x02) {
                ags = DataParser.decodeC40(feature.getValue());
                Logger.debug("Decoded AGS: " + ags);
            } else if (feature.getTag() == 0x03) {
                postalCode = DataParser.decodeC40(feature.getValue());
                Logger.debug("Decoded postal code: " + postalCode);
            } else {
                Logger.info("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
                throw new IllegalArgumentException("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }

    }

}
