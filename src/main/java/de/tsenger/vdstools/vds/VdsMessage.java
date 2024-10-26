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
	private VdsType vdsType = null;

	private VdsMessage() {
	}

	public VdsMessage(VdsType vdsType, List<DerTlv> derTlvList) {
		this();
		this.vdsType = vdsType;
		this.derTlvList = derTlvList;
	}

	public VdsMessage(VdsType vdsType) {
		this();
		this.vdsType = vdsType;
		this.derTlvList = new ArrayList<>(5);
	}

	public VdsType getVdsType() {
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

	public void addDocumentFeature(Feature feature, Object obj) throws IllegalArgumentException {
		DerTlv derTlv = DataEncoder.getFeatureEncoder().encodeFeature(vdsType, feature, obj);
		this.derTlvList.add(derTlv);
	}

	public Object getDocumentFeature(Feature feature) {
		Object returnObj = null;
		byte tag = DataEncoder.getFeatureEncoder().getTag(vdsType, feature);
		for (DerTlv derTlv : derTlvList) {
			if (derTlv.getTag() == tag)
				returnObj = DataEncoder.getFeatureEncoder().decodeFeature(vdsType, derTlv);
		}
		return returnObj;
	}

	public static VdsMessage fromByteArray(byte[] rawBytes, VdsType vdsType) {
		List<DerTlv> derTlvList = DataParser.parseDerTLvs(rawBytes);
		return new VdsMessage(vdsType, derTlvList);
	}

}
