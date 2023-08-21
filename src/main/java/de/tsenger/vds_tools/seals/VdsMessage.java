package de.tsenger.vds_tools.seals;

import java.util.ArrayList;

public class VdsMessage {
    private byte[] rawBytes;
    private ArrayList<DocumentFeature> documentFeatures = new ArrayList<>(5);

    public byte[] getRawBytes() {
        return rawBytes;
    }

    public void addDocumentFeature(DocumentFeature docFeature) {
        documentFeatures.add(docFeature);
    }

    public ArrayList<DocumentFeature> getDocumentFeatures() {
        return documentFeatures;
    }

    public void setRawDataBytes(byte[] rawBytes) {
        this.rawBytes = rawBytes;
    }
}
