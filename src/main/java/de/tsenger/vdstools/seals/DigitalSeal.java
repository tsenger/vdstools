package de.tsenger.vdstools.seals;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;

import org.bouncycastle.util.Arrays;

import de.tsenger.vdstools.DataParser;

/**
 * @author Tobias Senger
 *
 */
public abstract class DigitalSeal {

    private VdsType vdsType;

    private VdsHeader vdsHeader;

    private VdsMessage vdsMessage;

    private VdsSignature vdsSignature;

    private String rawString;

    protected EnumMap<Feature, Object> featureMap = new EnumMap<>(Feature.class);

    public DigitalSeal(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        this.vdsHeader = vdsHeader;
        this.vdsMessage = vdsMessage;
        this.vdsSignature = vdsSignature;
        this.vdsType = VdsType.valueOf(vdsHeader.getDocumentRef());
    }

    public static DigitalSeal getInstance(String rawString) {
        DigitalSeal seal = DataParser.parseVdsSeal(rawString);
        seal.rawString = rawString;
        return seal;
    }

    public VdsType getVdsType() {
        return vdsType;
    }

    public ArrayList<DocumentFeatureDto> getDocumentFeatures() {
        return vdsMessage.getDocumentFeatures();
    }

    public EnumMap<Feature, Object> getFeatureMap() {
        return featureMap.clone();
    }

    public String getIssuingCountry() {
        return vdsHeader.issuingCountry;
    }

    public String getSignerCertRef() {
        return (vdsHeader.signerIdentifier + vdsHeader.certificateReference);
    }

    public String getSignerIdentifier() {
        return getSignerCertRef().substring(0, 4);
    }

    public BigInteger getCertSerialNumber() {
        return new BigInteger(getSignerCertRef().substring(4), 16);
    }

    public LocalDate getIssuingDate() {
        return vdsHeader.issuingDate;
    }

    public LocalDate getSigDate() {
        return vdsHeader.sigDate;
    }

    public byte getDocFeatureRef() {
        return vdsHeader.docFeatureRef;
    }

    public byte getDocTypeCat() {
        return vdsHeader.docTypeCat;
    }

    public byte[] getHeaderAndMessageBytes() {
        return Arrays.concatenate(vdsHeader.rawBytes, vdsMessage.getRawBytes());
    }

    public byte[] getSignatureBytes() {
        return vdsSignature.getSignatureBytes();
    }

    public String getRawString() {
        return rawString;
    }

    public Object getFeature(Enum<Feature> feature) {
        try {
            return featureMap.get(feature);
        } catch (Exception e) {
            return null;
        }
    }

}
