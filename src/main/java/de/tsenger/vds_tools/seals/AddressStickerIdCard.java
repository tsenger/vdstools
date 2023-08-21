package de.tsenger.vds_tools.seals;

import java.util.List;

import org.tinylog.Logger;

import de.tsenger.vds_tools.DataParser;

public class AddressStickerIdCard extends DigitalSeal {

    private String docNr = "";
    private String ags = "";
    private String rawAddress = "";
    private String postalCode;
    private String street;
    private String streetNr;

    public AddressStickerIdCard(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
        parseAddress();
        featureMap.put(Feature.DOCUMENT_NUMBER, docNr);
        featureMap.put(Feature.AGS, ags);
        featureMap.put(Feature.RAW_ADDRESS, rawAddress);
        featureMap.put(Feature.POSTAL_CODE, postalCode);
        featureMap.put(Feature.STREET, street);
        featureMap.put(Feature.STREET_NR, streetNr);
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
                rawAddress = DataParser.decodeC40(feature.getValue());
                Logger.debug("Decoded (raw) address: " + rawAddress);
            } else {
                Logger.info("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
                throw new IllegalArgumentException("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }
    }

    private void parseAddress() {
        postalCode = rawAddress.substring(0, 5);
        street = rawAddress.substring(5).replaceAll("(\\d+\\w+)(?!.*\\d)", "");
        streetNr = rawAddress.substring(5).replaceAll(street, "");
        Logger.debug("parsed address: " + String.format("%s:%s:%s", postalCode, street, streetNr));
    }

}
