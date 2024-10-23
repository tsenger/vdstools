package de.tsenger.vdstools;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.tinylog.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.tsenger.vdstools.vds.Feature;
import de.tsenger.vdstools.vds.VdsType;
import de.tsenger.vdstools.vds.seals.FeaturesDto;
import de.tsenger.vdstools.vds.seals.SealDto;

public class FeatureConverter {

	public static String DEFAULT_SEAL_CODINGS = "src/main/resources/SealCodings.json";

	private List<SealDto> documents;

	public FeatureConverter() throws FileNotFoundException {
		this(DEFAULT_SEAL_CODINGS);
	}

	public FeatureConverter(String jsonFile) throws FileNotFoundException {
		Gson gson = new Gson();
		// Definiere den Typ für die Liste von Document-Objekten
		Type listType = new TypeToken<List<SealDto>>() {
		}.getType();

		FileReader reader = new FileReader(jsonFile);
		this.documents = gson.fromJson(reader, listType);
	}

	public Feature getFeature(VdsType vdsType, DerTlv derTlv) {
		SealDto document = getDocument(vdsType);
		if (document == null)
			return null;
		return getFeature(document, derTlv.getTag());
	}

	public byte getTag(VdsType vdsType, Feature feature) {
		SealDto document = getDocument(vdsType);
		if (document == null)
			return 0;
		return getTag(document, feature);
	}

	public Object decodeFeature(VdsType vdsType, DerTlv derTlv) {
		SealDto document = getDocument(vdsType);
		if (document == null)
			return null;
		String coding = getCoding(document, derTlv.getTag());
		switch (coding) {
		case "C40":
			return DataParser.decodeC40(derTlv.getValue()).replace(' ', '<');
		case "ByteArray":
			return derTlv.getValue();
		case "Utf8String":
			return new String(derTlv.getValue(), StandardCharsets.UTF_8);
		default:
			return derTlv.getValue();
		}
	}

	public DerTlv encodeFeature(VdsType vdsType, Feature feature, Object object) throws IllegalArgumentException {
		SealDto document = getDocument(vdsType);
		if (document == null) {
			Logger.warn("Couldn't find VdsType " + vdsType.toString());
			throw new IllegalArgumentException("Couldn't find VdsType " + vdsType.toString());
		}
		byte tag = getTag(document, feature);
		if (tag == 0) {
			Logger.warn("VdsType: " + vdsType.toString() + " has no Feature " + feature.toString());
			throw new IllegalArgumentException(
					"VdsType: " + vdsType.toString() + " has no Feature " + feature.toString());
		}
		String coding = getCoding(document, feature);
		byte[] value = null;
		switch (coding) {
		case "C40":
			String valueStr = ((String) object).replaceAll("\r", "").replaceAll("\n", "");
			value = DataEncoder.encodeC40(valueStr);
			break;
		case "ByteArray":
			value = (byte[]) object;
			break;
		case "Utf8String":
			try {
				value = ((String) object).getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				Logger.error("Couldn't encode String " + (String) object + " to UTF-8 bytes: " + e.getMessage());
			}
			break;
		default:
			value = (byte[]) object;
		}
		return new DerTlv(tag, value);
	}

	private byte getTag(SealDto document, Feature feature) {
		for (FeaturesDto featureDto : document.features) {
			if (featureDto.name.equalsIgnoreCase(feature.toString())) {
				return (byte) featureDto.tag;
			}
		}
		return 0;
	}

	private Feature getFeature(SealDto document, int tag) {
		Feature feature = null;
		for (FeaturesDto featureDto : document.features) {
			if (featureDto.tag == tag) {
				try {
					feature = Feature.valueOf(featureDto.name);
				} catch (IllegalArgumentException e) {
					Logger.error("Couldn't parse " + featureDto.name + " to an Feature enum value.");
				}
			}
		}
		return feature;
	}

	private String getCoding(SealDto document, Feature feature) {
		for (FeaturesDto featureDto : document.features) {
			if (featureDto.name.equalsIgnoreCase(feature.toString()))
				return featureDto.coding;
		}
		return null;
	}

	private String getCoding(SealDto document, byte tag) {
		for (FeaturesDto featureDto : document.features) {
			if (featureDto.tag == tag) {
				return featureDto.coding;
			}
		}
		return null;
	}

	private SealDto getDocument(VdsType vdsType) {
		for (SealDto document : documents) {
			int docRefInt = Integer.parseInt(document.documentRef, 16);
			VdsType docVdsType = VdsType.valueOf(docRefInt);
			if (docVdsType == vdsType)
				return document;
		}
		return null;
	}

}
