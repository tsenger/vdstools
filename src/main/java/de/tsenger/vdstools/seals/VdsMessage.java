package de.tsenger.vdstools.seals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.tinylog.Logger;

import de.tsenger.vdstools.DerTlv;

/**
 * @author Tobias Senger
 *
 */
public class VdsMessage {

	private List<DerTlv> derTlvList;
	private HashMap<Feature, Object> featureMap = new LinkedHashMap<Feature, Object>(2);
	private VdsType vdsType = null;

	public VdsMessage(List<DerTlv> derTlvList) {
		this.derTlvList = derTlvList;
	}

	public VdsMessage(VdsType vdsType) {
		this.vdsType = vdsType;
		this.derTlvList = new ArrayList<>(5);
	}

	public VdsType getVdsType() {
		return vdsType;
	}

	public byte[] getRawBytes() {
		if (!featureMap.isEmpty()) {
			if (derTlvList.size() != 0) {
				Logger.warn("messageTlvList for " + vdsType.name() + " is NOT empty(size: " + derTlvList.size()
						+ ")! Parsing featureMap will override messageTlvList.");
			}
			parseFeatures();
		}
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

	private void parseFeatures() {
		switch (vdsType) {
		case RESIDENCE_PERMIT:
			derTlvList = ResidencePermit.parseFeatures(featureMap);
			break;
		case ADDRESS_STICKER_ID:
			derTlvList = AddressStickerIdCard.parseFeatures(featureMap);
			break;
		case ADDRESS_STICKER_PASSPORT:
			derTlvList = AddressStickerPass.parseFeatures(featureMap);
			break;
		case ALIENS_LAW:
			derTlvList = AliensLaw.parseFeatures(featureMap);
			break;
		case ARRIVAL_ATTESTATION:
			derTlvList = ArrivalAttestation.parseFeatures(featureMap);
			break;
		case FICTION_CERT:
			derTlvList = FictionCert.parseFeatures(featureMap);
			break;
		case ICAO_EMERGENCY_TRAVEL_DOCUMENT:
			derTlvList = IcaoEmergencyTravelDocument.parseFeatures(featureMap);
			break;
		case ICAO_VISA:
			derTlvList = IcaoVisa.parseFeatures(featureMap);
			break;
		case SOCIAL_INSURANCE_CARD:
			try {
				derTlvList = SocialInsuranceCard.parseFeatures(featureMap);
			} catch (UnsupportedEncodingException e) {
				Logger.error("Couldn't build VdsMessage for SocialInsuranceCard: " + e.getMessage());
			}
			break;
		case SUPPLEMENTARY_SHEET:
			derTlvList = SupplementarySheet.parseFeatures(featureMap);
			break;
		case TEMP_PASSPORT:
			derTlvList = TempPassport.parseFeatures(featureMap);
			break;
		case TEMP_PERSO:
			derTlvList = TempPerso.parseFeatures(featureMap);
			break;
		default:
			Logger.warn("unknown VdsType: " + vdsType);
			break;
		}
	}

	public void addDerTlv(DerTlv derTlv) {
		this.derTlvList.add(derTlv);
	}

	public List<DerTlv> getDerTlvList() {
		return this.derTlvList;
	}

	public void addDocumentFeature(Feature feature, Object obj) {
		featureMap.put(feature, obj);
	}

}
