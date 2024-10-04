package de.tsenger.vdstools.seals;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

import org.bouncycastle.util.Arrays;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.Signer;

/**
 * @author Tobias Senger
 *
 */
public class DigitalSeal {

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
    
    public static DigitalSeal getInstance(VdsType vdsType, Map<Feature, Object> featureMap, X509Certificate cert, Signer signer) {
        DigitalSeal seal = DataEncoder.buildDigitalSeal(vdsType, featureMap, cert, signer);
        return seal;
    }

    public VdsType getVdsType() {
        return vdsType;
    }

    public ArrayList<MessageTlv> getMessageTlvList() {
        return vdsMessage.getMessageTlvList();
    }

    public EnumMap<Feature, Object> getFeatureMap() {
        return featureMap.clone();
    }

    public String getIssuingCountry() {
        return vdsHeader.issuingCountry;
    }

    /**
     * Returns a string that identifies the signer certificate. The SignerCertRef
     * string is build from Signer Identifier (country code || signer id) and
     * Certificate Reference. The Signer Identifier maps to the signer certificates
     * subject (C || CN) The Certificate Reference will be interpreted as an hex
     * string integer that represents the serial number of the signer certificate.
     * Leading zeros in Certificate Reference the will be cut off. e.g. Signer
     * Identifier 'DETS' and CertificateReference '00027' will result in 'DETS27'
     * 
     * @return Formated SignerCertRef all UPPERCASE
     */
    public String getSignerCertRef() {
        int certRefInteger = Integer.decode("0x" + vdsHeader.certificateReference);
        return String.format("%s%x", vdsHeader.signerIdentifier, certRefInteger).toUpperCase();
    }

    public String getSignerIdentifier() {
        return vdsHeader.signerIdentifier;
    }

    public String getCertificateReference() {
        return vdsHeader.certificateReference;
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
        return Arrays.concatenate(vdsHeader.getRawBytes(), vdsMessage.getRawBytes());
    }
    
	public byte[] getEncodedBytes() throws IOException {
		return Arrays.concatenate(vdsHeader.getRawBytes(), vdsMessage.getRawBytes(), vdsSignature.getRawBytes());
	}

    public byte[] getSignatureBytes() {
        return vdsSignature.getSignatureBytes();
    }

    public String getRawString() {
        return rawString;
    }

    public Object getFeature(Feature feature) {
        try {
            return featureMap.get(feature);
        } catch (Exception e) {
            return null;
        }
    }
    
    public void setFeature(Feature feature, Object object) {
    	featureMap.put(feature, object);

    }

}
