package de.tsenger.vdstools;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.tinylog.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.tsenger.vdstools.vds.dto.FeaturesDto;
import de.tsenger.vdstools.vds.dto.SealDto;

public class FeatureConverter {

	public static String DEFAULT_SEAL_CODINGS = "SealCodings.json";

	private List<SealDto> sealDtoList;

	private static Map<String, Integer> vdsTypes = new HashMap<>();
	private static Map<Integer, String> vdsTypesReverse = new HashMap<>();
	private static Set<String> vdsFeatures = new TreeSet<>();

	public FeatureConverter() throws FileNotFoundException {
		this(DEFAULT_SEAL_CODINGS);
	}

	public FeatureConverter(String jsonFile) throws FileNotFoundException {
		Gson gson = new Gson();
		// Definiere den Typ für die Liste von Document-Objekten
		Type listType = new TypeToken<List<SealDto>>() {
		}.getType();

		try (InputStream in = getClass().getResourceAsStream(DEFAULT_SEAL_CODINGS);
				BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			this.sealDtoList = gson.fromJson(reader, listType);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (SealDto sealDto : sealDtoList) {
			vdsTypes.put(sealDto.documentType, Integer.parseInt(sealDto.documentRef, 16));
			vdsTypesReverse.put(Integer.parseInt(sealDto.documentRef, 16), sealDto.documentType);
			for (FeaturesDto featureDto : sealDto.features) {
				vdsFeatures.add(featureDto.name);
			}
		}
	}

	public Set<String> getAvailableVdsTypes() {
		return new TreeSet<String>(vdsTypes.keySet());
	}

	public int getDocumentRef(String vdsType) {
		return vdsTypes.get(vdsType);
	}

	public String getVdsType(Integer docRef) {
		return vdsTypesReverse.get(docRef);
	}

	public Set<String> getAvailableVdsFeatures() {
		return vdsFeatures;
	}

	public String getFeature(String vdsType, DerTlv derTlv) {
		if (!vdsTypes.containsKey(vdsType)) {
			Logger.warn("No seal type with name '" + vdsType + "' was found.");
			return null;
		}
		SealDto sealDto = getSealDto(vdsType);
		if (sealDto == null)
			return null;
		return getFeatureName(sealDto, derTlv.getTag());
	}

	public byte getTag(String vdsType, String feature) {
		if (!vdsTypes.containsKey(vdsType)) {
			Logger.warn("No VdsSeal type with name '" + vdsType + "' was found.");
			return 0;
		}
		if (!vdsFeatures.contains(feature)) {
			Logger.warn("No VdsSeal feature with name '" + feature + "' was found.");
			return 0;
		}
		SealDto sealDto = getSealDto(vdsType);
		if (sealDto == null)
			return 0;
		return getTag(sealDto, feature);
	}

	public <T> T decodeFeature(String vdsType, DerTlv derTlv) {
		if (!vdsTypes.containsKey(vdsType)) {
			Logger.warn("No seal type with name '" + vdsType + "' was found.");
			return null;
		}
		SealDto sealDto = getSealDto(vdsType);
		if (sealDto == null)
			return null;
		return decodeFeature(sealDto, derTlv);
	}

	public <T> DerTlv encodeFeature(String vdsType, String feature, T inputValue) throws IllegalArgumentException {
		if (!vdsTypes.containsKey(vdsType)) {
			Logger.warn("No VdsSeal type with name '" + vdsType + "' was found.");
			return null;
		}
		if (!vdsFeatures.contains(feature)) {
			Logger.warn("No VdsSeal feature with name '" + feature + "' was found.");
			return null;
		}
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

	private byte getTag(SealDto sealDto, String feature) {
		for (FeaturesDto featureDto : sealDto.features) {
			if (featureDto.name.equalsIgnoreCase(feature)) {
				return (byte) featureDto.tag;
			}
		}
		return 0;
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

	private SealDto getSealDto(String vdsType) {
		for (SealDto sealDto : sealDtoList) {
			if (sealDto.documentType.equals(vdsType))
				return sealDto;
		}
		return null;
	}

}
