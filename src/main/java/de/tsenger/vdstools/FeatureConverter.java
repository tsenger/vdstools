package de.tsenger.vdstools;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import de.tsenger.vdstools.vds.Feature;
import de.tsenger.vdstools.vds.FeatureCoding;
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

	private final List<SealDto> sealDtoList;

	private static final Map<String, Integer> vdsTypes = new HashMap<>();
	private static final Map<Integer, String> vdsTypesReverse = new HashMap<>();
	private static final Set<String> vdsFeatures = new TreeSet<>();

	public FeatureConverter() {
		this(null);
	}

	public FeatureConverter(InputStream is) {
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(FeatureCoding.class, new FeatureEncodingDeserializer())
				.create();
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

        sealDto.features
				.forEach(featureDto -> derTlvList.stream()
                        .filter(derTlv -> derTlv.getTag() == featureDto.tag)
                        .findFirst() // Nimmt den ersten passenden Eintrag
                        .ifPresent(derTlv -> featureList.add(new Feature(featureDto.name, decodeFeature(sealDto,derTlv), featureDto.coding))));
		return featureList;
	}

	public String getFeatureName(String vdsType, DerTlv derTlv) throws IllegalArgumentException{
		if (!vdsTypes.containsKey(vdsType)) {
			Logger.warn("No seal type with name '" + vdsType + "' was found.");
			throw new IllegalArgumentException("No seal type with name '" + vdsType + "' was found.");
		}
		SealDto sealDto = getSealDto(vdsType);
        return getFeatureName(sealDto, derTlv.getTag());
	}

	public int getFeatureLength(String vdsType, byte tag) throws IllegalArgumentException{
		if (!vdsTypes.containsKey(vdsType)) {
			Logger.warn("No seal type with name '" + vdsType + "' was found.");
			throw new IllegalArgumentException("No seal type with name '" + vdsType + "' was found.");
		}
		SealDto sealDto = getSealDto(vdsType);
        FeaturesDto featureDto = getFeatureDto(sealDto, tag);
        return featureDto.decodedLength;
	}

	public byte getTag(String vdsType, String feature) throws IllegalArgumentException {
		if (!vdsTypes.containsKey(vdsType)) {
			Logger.warn("No VdsSeal type with name '" + vdsType + "' was found.");
			throw new IllegalArgumentException("No seal type with name '" + vdsType + "' was found.");
		}
		if (!vdsFeatures.contains(feature)) {
			Logger.warn("No VdsSeal feature with name '" + feature + "' was found.");
			throw new IllegalArgumentException("No VdsSeal feature with name '" + feature + "' was found.");
		}
		SealDto sealDto = getSealDto(vdsType);
        return getTag(sealDto, feature);
	}

	public FeatureCoding getFeatureCoding(String vdsType, DerTlv derTlv) throws IllegalArgumentException{
		if (!vdsTypes.containsKey(vdsType)) {
			Logger.warn("No seal type with name '" + vdsType + "' was found.");
			throw new IllegalArgumentException("No seal type with name '" + vdsType + "' was found.");
		}
		SealDto sealDto = getSealDto(vdsType);
        byte tag = derTlv.getTag();
		return getCoding(sealDto, tag);
	}

	public <T> T decodeFeature(String vdsType, DerTlv derTlv) throws IllegalArgumentException{
		if (!vdsTypes.containsKey(vdsType)) {
			Logger.warn("No seal type with name '" + vdsType + "' was found.");
			throw new IllegalArgumentException("No seal type with name '" + vdsType + "' was found.");
		}
		SealDto sealDto = getSealDto(vdsType);
        return decodeFeature(sealDto, derTlv);
	}

	public <T> DerTlv encodeFeature(String vdsType, String feature, T inputValue) throws IllegalArgumentException {
		if (!vdsTypes.containsKey(vdsType)) {
			Logger.warn("No VdsSeal type with name '" + vdsType + "' was found.");
			throw new IllegalArgumentException("No seal type with name '" + vdsType + "' was found.");
		}
		if (!vdsFeatures.contains(feature)) {
			Logger.warn("No VdsSeal feature with name '" + feature + "' was found.");
			throw new IllegalArgumentException("No VdsSeal feature with name '" + feature + "' was found.");
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
		FeatureCoding coding = getCoding(sealDto, feature);
		byte[] value;
		switch (coding) {
			case C40:
				String valueStr = ((String) inputValue).replaceAll("\r", "").replaceAll("\n", "");
				value = DataEncoder.encodeC40(valueStr);
				break;
			case UTF8_STRING:
				value = ((String) inputValue).getBytes(StandardCharsets.UTF_8);
				break;
			case BYTE:
				value = new byte[]{(byte)inputValue};
				break;
			case BYTES:
			default:
				value = (byte[]) inputValue;
		}
		return new DerTlv(tag, value);
	}

	@SuppressWarnings("unchecked")
	private <T> T decodeFeature(SealDto sealDto, DerTlv derTlv) {
		byte tag = derTlv.getTag();
		FeatureCoding coding = getCoding(sealDto, tag);
		switch (coding) {
			case C40:
				String featureValue = DataParser.decodeC40(derTlv.getValue());
				String featureName = getFeatureName(sealDto, tag);
				if (featureName!=null && featureName.startsWith("MRZ")) {
					int mrzLength =  getFeatureDto(sealDto, tag).decodedLength;
					String newMrz = String.format("%1$-"+mrzLength+"s", featureValue).replace(' ', '<');
					featureValue = newMrz.substring(0, mrzLength / 2) + "\n" + newMrz.substring(mrzLength / 2);
				}
				return (T) featureValue;
			case UTF8_STRING:
				return (T) new String(derTlv.getValue(), StandardCharsets.UTF_8);
			case BYTE:
				return (T) Byte.valueOf(derTlv.getValue()[0]);
			case BYTES:
			default:
				return (T) derTlv.getValue();
		}
	}

	private byte getTag(SealDto sealDto, String feature) throws IllegalArgumentException {
		for (FeaturesDto featureDto : sealDto.features) {
			if (featureDto.name.equalsIgnoreCase(feature)) {
				return (byte) featureDto.tag;
			}
		}
		throw new IllegalArgumentException("Feature '" + feature + "' is unspecified for the given seal '" + sealDto.documentType+ "'");
	}

	private String getFeatureName(SealDto sealDto, int tag) throws IllegalArgumentException {
		for (FeaturesDto featureDto : sealDto.features) {
			if (featureDto.tag == tag) {
				return featureDto.name;
			}
		}
		throw new IllegalArgumentException("No Feature with tag '" + tag + "' is specified for the given seal '" + sealDto.documentType+ "'");
	}

	private FeatureCoding getCoding(SealDto sealDto, String feature) throws IllegalArgumentException {
		for (FeaturesDto featureDto : sealDto.features) {
			if (featureDto.name.equalsIgnoreCase(feature)) {
				return featureDto.coding;
			}
		}
		throw new IllegalArgumentException("Feature '" + feature + "' is unspecified for the given seal '" + sealDto.documentType+ "'");
	}

	private FeatureCoding getCoding(SealDto sealDto, byte tag) throws IllegalArgumentException {
		for (FeaturesDto featureDto : sealDto.features) {
			if (featureDto.tag == tag) {
				return featureDto.coding;
			}
		}
		throw new IllegalArgumentException("No Feature with tag '" + tag + "' is specified for the given seal '" + sealDto.documentType+ "'");
	}

	private FeaturesDto getFeatureDto(SealDto sealDto, byte tag) throws IllegalArgumentException {
		for (FeaturesDto featureDto : sealDto.features) {
			if (featureDto.tag == tag) {
				return featureDto;
			}
		}
		throw new IllegalArgumentException("No Feature with tag '" + tag + "' is specified for the given seal '" + sealDto.documentType+ "'");
	}

	private SealDto getSealDto(String vdsType) throws IllegalArgumentException {
		for (SealDto sealDto : sealDtoList) {
			if (sealDto.documentType.equals(vdsType)) {
				return sealDto;
			}
		}
		throw new IllegalArgumentException("VdsType '" + vdsType + "' is unspecified in SealCodings.");
	}
}

class FeatureEncodingDeserializer implements JsonDeserializer<FeatureCoding> {
	@Override
	public FeatureCoding deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		String value = json.getAsString();
		try {
			return FeatureCoding.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new JsonParseException("Invalid value for FeatureCoding: " + value);
		}
	}
}
