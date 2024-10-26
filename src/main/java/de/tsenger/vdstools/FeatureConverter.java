package de.tsenger.vdstools;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.tinylog.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.tsenger.vdstools.vds.Feature;
import de.tsenger.vdstools.vds.VdsType;
import de.tsenger.vdstools.vds.seals.FeaturesDto;
import de.tsenger.vdstools.vds.seals.SealDto;

public class FeatureConverter {

	public static String DEFAULT_SEAL_CODINGS = "src/main/resources/SealCodings.json";

	private List<SealDto> sealDtoList;

	private static Map<String, Integer> vdsTypes = new HashMap<>();
	private static Set<String> vdsFeatures = new HashSet<>();

	public FeatureConverter() throws FileNotFoundException {
		this(DEFAULT_SEAL_CODINGS);
	}

	public FeatureConverter(String jsonFile) throws FileNotFoundException {
		Gson gson = new Gson();
		// Definiere den Typ für die Liste von Document-Objekten
		Type listType = new TypeToken<List<SealDto>>() {
		}.getType();

		FileReader reader = new FileReader(jsonFile);
		this.sealDtoList = gson.fromJson(reader, listType);

		for (SealDto sealDto : sealDtoList) {
			vdsTypes.put(sealDto.documentType, Integer.parseInt(sealDto.documentRef, 16));
			for (FeaturesDto featureDto : sealDto.features) {
				vdsFeatures.add(featureDto.name);
			}
		}
	}

	public Feature getFeature(VdsType vdsType, DerTlv derTlv) {
		SealDto sealDto = getSealDto(vdsType);
		if (sealDto == null)
			return null;
		return getFeature(sealDto, derTlv.getTag());
	}

	public String getFeature(String vdsType, DerTlv derTlv) {
		SealDto sealDto = getSealDto(vdsType);
		if (sealDto == null)
			return null;
		return getFeatureName(sealDto, derTlv.getTag());
	}

	public byte getTag(VdsType vdsType, Feature feature) {
		SealDto sealDto = getSealDto(vdsType);
		if (sealDto == null)
			return 0;
		return getTag(sealDto, feature);
	}

	public byte getTag(String vdsType, String feature) {
		SealDto sealDto = getSealDto(vdsType);
		if (sealDto == null)
			return 0;
		return getTag(sealDto, feature);
	}

	public <T> T decodeFeature(VdsType vdsType, DerTlv derTlv) {
		SealDto sealDto = getSealDto(vdsType);
		if (sealDto == null)
			return null;
		return decodeFeature(sealDto, derTlv);
	}

	public <T> T decodeFeature(String vdsType, DerTlv derTlv) {
		SealDto sealDto = getSealDto(vdsType);
		if (sealDto == null)
			return null;
		return decodeFeature(sealDto, derTlv);
	}

	public <T> DerTlv encodeFeature(VdsType vdsType, Feature feature, T inputValue) throws IllegalArgumentException {
		SealDto sealDto = getSealDto(vdsType);
		return encodeFeature(sealDto, feature.name(), inputValue);
	}

	public <T> DerTlv encodeFeature(String vdsType, String feature, T inputValue) throws IllegalArgumentException {
		SealDto sealDto = getSealDto(vdsType);
		return encodeFeature(sealDto, feature, inputValue);
	}

	private <T> DerTlv encodeFeature(SealDto sealDto, String feature, T inputValue) throws IllegalArgumentException {

		byte tag = getTag(sealDto, feature);
		if (tag == 0) {
			Logger.warn("VdsType: " + sealDto.documentType + " has no Feature " + feature);
			throw new IllegalArgumentException("VdsType: " + sealDto.documentType + " has no Feature " + feature);
		}
		String coding = getCoding(sealDto, feature);
		byte[] value = null;
		switch (coding) {
		case "C40":
			String valueStr = ((String) inputValue).replaceAll("\r", "").replaceAll("\n", "");
			value = DataEncoder.encodeC40(valueStr);
			break;
		case "ByteArray":
			value = (byte[]) inputValue;
			break;
		case "Utf8String":
			try {
				value = ((String) inputValue).getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				Logger.error("Couldn't encode String " + (String) inputValue + " to UTF-8 bytes: " + e.getMessage());
			}
			break;
		default:
			value = (byte[]) inputValue;
		}
		return new DerTlv(tag, value);
	}

	@SuppressWarnings("unchecked")
	private <T> T decodeFeature(SealDto sealDto, DerTlv derTlv) {
		String coding = getCoding(sealDto, derTlv.getTag());
		switch (coding) {
		case "C40":
			return (T) DataParser.decodeC40(derTlv.getValue()).replace(' ', '<');
		case "ByteArray":
			return (T) derTlv.getValue();
		case "Utf8String":
			return (T) new String(derTlv.getValue(), StandardCharsets.UTF_8);
		default:
			return (T) derTlv.getValue();
		}
	}

	private byte getTag(SealDto sealDto, Feature feature) {
		for (FeaturesDto featureDto : sealDto.features) {
			if (featureDto.name.equalsIgnoreCase(feature.toString())) {
				return (byte) featureDto.tag;
			}
		}
		return 0;
	}

	private byte getTag(SealDto sealDto, String feature) {
		for (FeaturesDto featureDto : sealDto.features) {
			if (featureDto.name.equalsIgnoreCase(feature)) {
				return (byte) featureDto.tag;
			}
		}
		return 0;
	}

	private Feature getFeature(SealDto sealDto, int tag) {
		Feature feature = null;
		for (FeaturesDto featureDto : sealDto.features) {
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

	private String getFeatureName(SealDto sealDto, int tag) {
		for (FeaturesDto featureDto : sealDto.features) {
			if (featureDto.tag == tag) {
				return featureDto.name;
			}
		}
		return null;
	}

	private String getCoding(SealDto sealDto, String feature) {
		for (FeaturesDto featureDto : sealDto.features) {
			if (featureDto.name.equalsIgnoreCase(feature))
				return featureDto.coding;
		}
		return null;
	}

	private String getCoding(SealDto sealDto, byte tag) {
		for (FeaturesDto featureDto : sealDto.features) {
			if (featureDto.tag == tag) {
				return featureDto.coding;
			}
		}
		return null;
	}

	private SealDto getSealDto(VdsType vdsType) {
		for (SealDto sealdto : sealDtoList) {
			int docRefInt = Integer.parseInt(sealdto.documentRef, 16);
			VdsType docVdsType = VdsType.valueOf(docRefInt);
			if (docVdsType == vdsType)
				return sealdto;
		}
		return null;
	}

	private SealDto getSealDto(String vdsType) {
		for (SealDto sealDto : sealDtoList) {
			if (sealDto.documentType.equals(vdsType))
				return sealDto;
		}
		return null;
	}

}
