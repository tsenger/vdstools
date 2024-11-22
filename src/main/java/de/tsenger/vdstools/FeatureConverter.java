package de.tsenger.vdstools;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.tsenger.vdstools.vds.Feature;
import de.tsenger.vdstools.vds.dto.FeaturesDto;
import de.tsenger.vdstools.vds.dto.SealDto;
import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FeatureConverter {

	public static String DEFAULT_SEAL_CODINGS = "/SealCodings.json";

	private List<SealDto> sealDtoList;

	private static Map<String, Integer> vdsTypes = new HashMap<>();
	private static Map<Integer, String> vdsTypesReverse = new HashMap<>();
	private static Set<String> vdsFeatures = new TreeSet<>();

	public FeatureConverter() {
		this(null);
	}

	public FeatureConverter(InputStream is) {
		Gson gson = new Gson();
		// Definiere den Typ für die Liste von Document-Objekten
		Type listType = new TypeToken<List<SealDto>>() {
		}.getType();

		if (is == null) {
			is = getClass().getResourceAsStream(DEFAULT_SEAL_CODINGS);
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		this.sealDtoList = gson.fromJson(reader, listType);

		for (SealDto sealDto : sealDtoList) {
			vdsTypes.put(sealDto.documentType, Integer.parseInt(sealDto.documentRef, 16));
			vdsTypesReverse.put(Integer.parseInt(sealDto.documentRef, 16), sealDto.documentType);
			for (FeaturesDto featureDto : sealDto.features) {
				vdsFeatures.add(featureDto.name);
			}
		}
	}

	public Set<String> getAvailableVdsTypes() {
		return new TreeSet<>(vdsTypes.keySet());
	}

	public int getDocumentRef(String vdsType) {
		if (vdsTypes.get(vdsType) == null) throw new IllegalArgumentException("Could find seal type "+vdsType+" in "+DEFAULT_SEAL_CODINGS);
		return vdsTypes.get(vdsType);
	}

	public String getVdsType(Integer docRef) {
		return vdsTypesReverse.get(docRef);
	}

	public Set<String> getAvailableVdsFeatures() {
		return vdsFeatures;
	}

	public List<Feature> convertDerTlvToFeatureList(String vdsType, List<DerTlv> derTlvList) {
		List<Feature> featureList = new ArrayList<>(5);
		SealDto sealDto = getSealDto(vdsType);

		if (sealDto==null) return featureList;

		sealDto.features
				.forEach(featureDto -> {
					derTlvList.stream()
							.filter(derTlv -> derTlv.getTag() == featureDto.tag)
							.findFirst() // Nimmt den ersten passenden Eintrag
							.ifPresent(derTlv -> featureList.add(new Feature(featureDto.name, decodeFeature(sealDto,derTlv), featureDto.coding)));
				});


		return featureList;
	}

	public String getFeatureName(String vdsType, DerTlv derTlv) {
		if (!vdsTypes.containsKey(vdsType)) {
			Logger.warn("No seal type with name '" + vdsType + "' was found.");
			return null;
		}
		SealDto sealDto = getSealDto(vdsType);
		if (sealDto == null) {
			return null;
		}
		return getFeatureName(sealDto, derTlv.getTag());
	}

	public int getFeatureLength(String vdsType, byte tag) {
		if (!vdsTypes.containsKey(vdsType)) {
			Logger.warn("No seal type with name '" + vdsType + "' was found.");
			return -1;
		}
		SealDto sealDto = getSealDto(vdsType);
		if (sealDto == null) {
			return -1;
		}
		FeaturesDto featureDto = getFeatureDto(sealDto, tag);
		if(featureDto == null) {
			return -1;
		}
		return featureDto.decodedLength;
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
		if (sealDto == null) {
			return 0;
		}
		return getTag(sealDto, feature);
	}

	public String getFeatureCoding(String vdsType, DerTlv derTlv) {
		if (!vdsTypes.containsKey(vdsType)) {
			Logger.warn("No seal type with name '" + vdsType + "' was found.");
			return null;
		}
		SealDto sealDto = getSealDto(vdsType);
		if (sealDto == null) {
			return null;
		}
		byte tag = derTlv.getTag();
		return getCoding(sealDto, tag);
	}

	public <T> T decodeFeature(String vdsType, DerTlv derTlv) {
		if (!vdsTypes.containsKey(vdsType)) {
			Logger.warn("No seal type with name '" + vdsType + "' was found.");
			return null;
		}
		SealDto sealDto = getSealDto(vdsType);
		if (sealDto == null) {
			return null;
		}
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
		byte[] value;
		switch (coding) {
		case "C40":
			String valueStr = ((String) inputValue).replaceAll("\r", "").replaceAll("\n", "");
			value = DataEncoder.encodeC40(valueStr);
			break;
		case "ByteArray":
			value = (byte[]) inputValue;
			break;
		case "Utf8String":
            value = ((String) inputValue).getBytes(StandardCharsets.UTF_8);
            break;
		default:
			value = (byte[]) inputValue;
		}
		return new DerTlv(tag, value);
	}

	@SuppressWarnings("unchecked")
	private <T> T decodeFeature(SealDto sealDto, DerTlv derTlv) {
		byte tag = derTlv.getTag();
		String coding = getCoding(sealDto, tag);
		switch (coding) {
		case "C40":
			String featureValue = DataParser.decodeC40(derTlv.getValue());
			String featureName = getFeatureName(sealDto, tag);
            if (featureName.startsWith("MRZ")) {
				featureValue = featureValue.replace(' ', '<');
			}
			return (T) featureValue;
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
			if (featureDto.name.equalsIgnoreCase(feature)) {
				return featureDto.coding;
			}
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

	private FeaturesDto getFeatureDto(SealDto sealDto, byte tag) {
		for (FeaturesDto featureDto : sealDto.features) {
			if (featureDto.tag == tag) {
				return featureDto;
			}
		}
		return null;
	}

	private SealDto getSealDto(String vdsType) {
		for (SealDto sealDto : sealDtoList) {
			if (sealDto.documentType.equals(vdsType)) {
				return sealDto;
			}
		}
		return null;
	}

}
