package de.tsenger.vdstools.seals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataEncoder;

/**
 * @author Tobias Senger
 *
 */
public class VdsHeader {
	
	public static final byte DC = (byte) 0xDC;

    private byte[] rawBytes = null;

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
    
    public void setDocumentType(VdsType vdsType) {
    	docFeatureRef = (byte) ((vdsType.getValue() >> 8) & 0xFF);
    	docTypeCat = (byte) (vdsType.getValue() & 0xFF);
    }
    
    public VdsType getVdsType() {
    	return VdsType.valueOf(getDocumentRef());
    }
    
    // TODO: remove this if possible - with help of parser or constructor/parser
//    public void setRawBytes(byte[] rawBytes) {
//    	this.rawBytes = rawBytes.clone();
//    }
    
    public byte[] getRawBytes()  {    	
    	if (rawBytes==null) encode() ;
		return rawBytes;
    }

	private void encode() {
		if (sigDate == null) {
			sigDate = LocalDate.now();
		}
		if (issuingDate == null) {
			issuingDate = LocalDate.now();
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();		
		try {
			baos.write(DC);
			baos.write(rawVersion);
			baos.write(DataEncoder.encodeC40(issuingCountry));
			baos.write(DataEncoder.encodeC40(getEncodedSignerIdentifierandCertificateReference()));
			baos.write(DataEncoder.encodeDate(issuingDate));
			baos.write(DataEncoder.encodeDate(sigDate));
			baos.write(docFeatureRef);
			baos.write(docTypeCat);
		} catch (IOException e) {
			Logger.error("Error while encoding header data: " + e.getMessage());
		}		
		rawBytes = baos.toByteArray();
	}
	
	private String getEncodedSignerIdentifierandCertificateReference() {
		if (rawVersion==2) return String.format("%s%5s", signerIdentifier, certificateReference).toUpperCase().replace(' ', '0');
		else if (rawVersion==3) return String.format("%s%02x%s", signerIdentifier, certificateReference.length(), certificateReference).toUpperCase();
		else return "";
	}
    
    @Override
    public String toString() {
        return ("rawVersion: " + (rawVersion & 0xff) + "\nissuingCountry: " + issuingCountry + "\nsignerIdentifier: "
                + signerIdentifier + "\ncertificateReference: " + certificateReference + "\nissuingDate: " + issuingDate
                + "\nsigDate: " + sigDate + "\ndocFeatureRef: " + String.format("%02X ", docFeatureRef)
                + ", docTypeCat: " + String.format("%02X ", docTypeCat));
    }

}
