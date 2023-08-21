package de.tsenger.vdstools.seals;

import java.util.ArrayList;

/**
 * @author Tobias Senger
 *
 */
public class VdsMessage {
    private byte[] rawBytes;
    private ArrayList<DocumentFeatureDto> documentFeatures = new ArrayList<>(5);

    public byte[] getRawBytes() {
        return rawBytes;
    }

    public void addDocumentFeature(DocumentFeatureDto docFeature) {
        documentFeatures.add(docFeature);
    }

    public ArrayList<DocumentFeatureDto> getDocumentFeatures() {
        return documentFeatures;
    }

    public void setRawDataBytes(byte[] rawBytes) {
        this.rawBytes = rawBytes;
    }
}
