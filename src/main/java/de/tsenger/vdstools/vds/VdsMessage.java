package de.tsenger.vdstools.vds;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataEncoder;
import de.tsenger.vdstools.DataParser;
import de.tsenger.vdstools.DerTlv;

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

	public Map<String, Feature> getFeatureMap() {
		Map<String, Feature> featureMap = new HashMap<String, Feature>();
		for (DerTlv derTlv : derTlvList) {
			Object value = DataEncoder.getFeatureEncoder().decodeFeature(vdsType, derTlv);
			String key = DataEncoder.getFeatureEncoder().getFeatureName(vdsType, derTlv);
			if (value != null)
				featureMap.put(key, new Feature(value));
		}
		return featureMap;
	}

	public Optional<Feature> getFeature(String feature) {
		Object value = null;
		byte tag = DataEncoder.getFeatureEncoder().getTag(vdsType, feature);
		if (tag != 0) {
			for (DerTlv derTlv : derTlvList) {
				if (derTlv.getTag() == tag) {
					value = DataEncoder.getFeatureEncoder().decodeFeature(vdsType, derTlv);
					break;
				}
			}
		}
		return Optional.ofNullable(value != null ? new Feature(value) : null);
	}

	public static VdsMessage fromByteArray(byte[] rawBytes, String vdsType) {
		List<DerTlv> derTlvList = DataParser.parseDerTLvs(rawBytes);
		return new VdsMessage(vdsType, derTlvList);
	}

	public static class Builder {
		private List<DerTlv> derTlvList = new ArrayList<>(5);
		private String vdsType = null;

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
