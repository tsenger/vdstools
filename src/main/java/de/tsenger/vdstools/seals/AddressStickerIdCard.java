package de.tsenger.vdstools.seals;

import java.util.List;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataParser;

public class AddressStickerIdCard extends DigitalSeal {

    public AddressStickerIdCard(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
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
                String rawAddress = DataParser.decodeC40(feature.getValue());
                featureMap.put(Feature.RAW_ADDRESS, rawAddress);
                parseAddress(rawAddress);
                break;
            default:
                Logger.warn("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }
    }

    private void parseAddress(String rawAddress) {
        String postalCode = rawAddress.substring(0, 5);
        String street = rawAddress.substring(5).replaceAll("(\\d+\\w+)(?!.*\\d)", "");
        String streetNr = rawAddress.substring(5).replaceAll(street, "");

        featureMap.put(Feature.POSTAL_CODE, postalCode);
        featureMap.put(Feature.STREET, street);
        featureMap.put(Feature.STREET_NR, streetNr);

        Logger.debug("parsed address: " + String.format("%s:%s:%s", postalCode, street, streetNr));
    }

}
