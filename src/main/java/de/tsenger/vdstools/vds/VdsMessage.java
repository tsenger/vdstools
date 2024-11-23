package de.tsenger.vdstools.vds;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.DerTlv;
import org.tinylog.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

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

	private VdsMessage(Builder builder) {
		this.derTlvList = builder.derTlvList;
		this.vdsType = builder.vdsType;
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

	public List<DerTlv> getDerTlvList() {
		return this.derTlvList;
	}

	/**
	 * @return  a list of all decoded Features
	 */
	public List<Feature> getFeatureList() {
		List<Feature> featureList = new ArrayList<>();
		for (DerTlv derTlv : derTlvList) {
			Object value = DataEncoder.getFeatureEncoder().decodeFeature(vdsType, derTlv);
			String name = DataEncoder.getFeatureEncoder().getFeatureName(vdsType, derTlv);
			FeatureCoding coding = DataEncoder.getFeatureEncoder().getFeatureCoding(vdsType, derTlv);
			if (value != null)
				featureList.add(new Feature(name, value, coding));
		}
		return featureList;
	}

	public Optional<Feature> getFeature(String featureName) {
		Object value = null;
		FeatureCoding coding = null;
		byte tag = 0;
		try {
			tag = DataEncoder.getFeatureEncoder().getTag(vdsType, featureName);
		} catch (IllegalArgumentException ignored){}
		if (tag != 0) {
			for (DerTlv derTlv : derTlvList) {
				if (derTlv.getTag() == tag) {
					value = DataEncoder.getFeatureEncoder().decodeFeature(vdsType, derTlv);
					coding = DataEncoder.getFeatureEncoder().getFeatureCoding(vdsType, derTlv);
					break;
				}
			}
		}
		return Optional.ofNullable(value != null ? new Feature(featureName, value, coding) : null);
	}

	public static VdsMessage fromByteArray(byte[] rawBytes, String vdsType) {
		List<DerTlv> derTlvList = DataParser.parseDerTLvs(rawBytes);
		return new VdsMessage(vdsType, derTlvList);
	}

	public static class Builder {
		private final List<DerTlv> derTlvList = new ArrayList<>(5);
		private final String vdsType;

		public Builder(String vdsType) {
			this.vdsType = vdsType;
		}

		public <T> Builder addDocumentFeature(String feature, T value) throws IllegalArgumentException {
			DerTlv derTlv = DataEncoder.getFeatureEncoder().encodeFeature(this.vdsType, feature, value);
			this.derTlvList.add(derTlv);
			return this;
		}

		public VdsMessage build() {
			return new VdsMessage(this);
		}
	}

}
