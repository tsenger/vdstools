package de.tsenger.vdstools.seals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.bouncycastle.asn1.DEROctetString;
import org.tinylog.Logger;

import de.tsenger.vdstools.DataEncoder;

/**
 * @author Tobias Senger
 *
 */
public class VdsMessage {
	private ArrayList<DocumentFeatureDto> documentFeatures = new ArrayList<>(5);

	public byte[] getRawBytes() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			for (DocumentFeatureDto feature : documentFeatures) {
				baos.write(DataEncoder.buildTLVStructure(feature.getTag(), feature.getValue()));
			}
		} catch (IOException e) {
			Logger.error("Can't build raw bytes: "+e.getMessage());
			return new byte[0];
		}
		return baos.toByteArray();
	}

	public void addDocumentFeature(DocumentFeatureDto docFeature) {
		documentFeatures.add(docFeature);
	}

	public ArrayList<DocumentFeatureDto> getDocumentFeatures() {
		return documentFeatures;
	}
	
}
