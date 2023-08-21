package de.tsenger.vdstools.seals;

import java.time.LocalDate;

/**
 * @author Tobias Senger
 *
 */
public class VdsHeader {

    public byte[] rawBytes;

    public String issuingCountry;
    public String signerIdentifier;
    public String certificateReference;
    public LocalDate issuingDate;
    public LocalDate sigDate;

    public byte docFeatureRef;
    public byte docTypeCat;

    public byte rawVersion;

    public int getDocumentRef() {
        return ((docFeatureRef & 0xFF) << 8) + (docTypeCat & 0xFF);
    }

    @Override
    public String toString() {
        return ("rawVersion: " + (rawVersion & 0xff) + "\nissuingCountry: " + issuingCountry + "\nsignerIdentifier: "
                + signerIdentifier + "\ncertificateReference: " + certificateReference + "\nissuingDate: " + issuingDate
                + "\nsigDate: " + sigDate + "\ndocFeatureRef: " + String.format("%02X ", docFeatureRef)
                + ", docTypeCat: " + String.format("%02X ", docTypeCat));
    }

}
