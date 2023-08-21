package de.tsenger.vds_tools.seals;

import java.util.ArrayList;

import org.tinylog.Logger;

import de.tsenger.vds_tools.DataParser;

/**
 * Created by Tobias Senger on 12.01.2017.
 */
public class AliensLaw extends DigitalSeal {

    private String mrz = "";
    private String azr = "";
    private String passportNumber = "";
    private byte[] faceImage;

    public AliensLaw(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
        featureMap.put(Feature.MRZ, mrz);
        featureMap.put(Feature.AZR, azr);
        featureMap.put(Feature.PASSPORT_NUMBER, passportNumber);
        featureMap.put(Feature.FACE_IMAGE, faceImage);
    }

    private void parseDocumentFeatures(ArrayList<DocumentFeature> features) {
        for (DocumentFeature feature : features) {
            if (feature.getTag() == 0x01) {
                faceImage = feature.getValue();
            } else if (feature.getTag() == 0x02) {
                String short_mrz = DataParser.decodeC40(feature.getValue()).replace(' ', '<');
                mrz = String.format("%1$-72s", short_mrz).replace(' ', '<');
            } else if (feature.getTag() == 0x03) {
                passportNumber = DataParser.decodeC40(feature.getValue());
            } else if (feature.getTag() == 0x04) {
                azr = DataParser.decodeC40(feature.getValue());
            } else {
                Logger.info("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
                throw new IllegalArgumentException("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }
    }

}
