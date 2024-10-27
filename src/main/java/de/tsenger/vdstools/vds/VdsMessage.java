package de.tsenger.vdstools.vds;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.DerTlv;

/**
 * @author Tobias Senger
 *
 */
public class VdsMessage {

	private List<DerTlv> derTlvList;
	private String vdsType = null;

	private VdsMessage() {
	}

	public VdsMessage(String vdsType, List<DerTlv> derTlvList) {
		this();
		this.vdsType = vdsType;
		this.derTlvList = derTlvList;
	}

	public VdsMessage(String vdsType) {
		this();
		this.vdsType = vdsType;
		this.derTlvList = new ArrayList<>(5);
	}

	public String getVdsType() {
		return vdsType;
	}

	public byte[] getEncoded() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			for (DerTlv feature : this.derTlvList) {
				baos.write(feature.getEncoded());
			}
		} catch (IOException e) {
			Logger.error("Can't build raw bytes: " + e.getMessage());
			return new byte[0];
		}
		return baos.toByteArray();
	}

	public void addDerTlv(DerTlv derTlv) {
		this.derTlvList.add(derTlv);
	}

	public List<DerTlv> getDerTlvList() {
		return this.derTlvList;
	}

	public <T> void addDocumentFeature(String feature, T value) throws IllegalArgumentException {
		DerTlv derTlv = DataEncoder.getFeatureEncoder().encodeFeature(vdsType, feature, value);
		this.derTlvList.add(derTlv);
	}

	public <T> T getDocumentFeature(String feature) {
		T value = null;
		byte tag = DataEncoder.getFeatureEncoder().getTag(vdsType, feature);
		for (DerTlv derTlv : derTlvList) {
			if (derTlv.getTag() == tag)
				value = DataEncoder.getFeatureEncoder().decodeFeature(vdsType, derTlv);
		}
		return value;
	}

	public static VdsMessage fromByteArray(byte[] rawBytes, String vdsType) {
		List<DerTlv> derTlvList = DataParser.parseDerTLvs(rawBytes);
		return new VdsMessage(vdsType, derTlvList);
	}

}
