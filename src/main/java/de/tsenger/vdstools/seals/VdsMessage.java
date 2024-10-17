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

	private List<MessageTlv> messageTlvList;
	private HashMap<Feature, Object> featureMap = new LinkedHashMap<Feature, Object>(2);
	private VdsType vdsType = null;

	public VdsMessage(List<MessageTlv> messageTlvList) {
		this.messageTlvList = messageTlvList;
	}

	public VdsMessage(VdsType vdsType) {
		this.vdsType = vdsType;
		this.messageTlvList = new ArrayList<>(5);
	}

	public VdsType getVdsType() {
		return vdsType;
	}

	public byte[] getRawBytes() {
		if (!featureMap.isEmpty()) {
			if (messageTlvList.size() != 0) {
				Logger.warn("messageTlvList for " + vdsType.name() + " is NOT empty(size: " + messageTlvList.size()
						+ ")! Parsing featureMap will override messageTlvList.");
			}
			parseFeatures();
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			for (MessageTlv feature : this.messageTlvList) {
				DerTlv derFeature = new DerTlv(feature.getTag(), feature.getValue());
				baos.write(derFeature.getEncoded());
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
			messageTlvList = ResidencePermit.parseFeatures(featureMap);
			break;
		case ADDRESS_STICKER_ID:
			messageTlvList = AddressStickerIdCard.parseFeatures(featureMap);
			break;
		case ADDRESS_STICKER_PASSPORT:
			messageTlvList = AddressStickerPass.parseFeatures(featureMap);
			break;
		case ALIENS_LAW:
			messageTlvList = AliensLaw.parseFeatures(featureMap);
			break;
		case ARRIVAL_ATTESTATION:
			messageTlvList = ArrivalAttestation.parseFeatures(featureMap);
			break;
		case FICTION_CERT:
			messageTlvList = FictionCert.parseFeatures(featureMap);
			break;
		case ICAO_EMERGENCY_TRAVEL_DOCUMENT:
			messageTlvList = IcaoEmergencyTravelDocument.parseFeatures(featureMap);
			break;
		case ICAO_VISA:
			messageTlvList = IcaoVisa.parseFeatures(featureMap);
			break;
		case SOCIAL_INSURANCE_CARD:
			try {
				messageTlvList = SocialInsuranceCard.parseFeatures(featureMap);
			} catch (UnsupportedEncodingException e) {
				Logger.error("Couldn't build VdsMessage for SocialInsuranceCard: " + e.getMessage());
			}
			break;
		case SUPPLEMENTARY_SHEET:
			messageTlvList = SupplementarySheet.parseFeatures(featureMap);
			break;
		case TEMP_PASSPORT:
			messageTlvList = TempPassport.parseFeatures(featureMap);
			break;
		case TEMP_PERSO:
			messageTlvList = TempPerso.parseFeatures(featureMap);
			break;
		default:
			Logger.warn("unknown VdsType: " + vdsType);
			break;
		}
	}

	public void addMessageTlv(MessageTlv tlv) {
		this.messageTlvList.add(tlv);
	}

	public List<MessageTlv> getMessageTlvList() {
		return this.messageTlvList;
	}

	public void addDocumentFeature(Feature feature, Object obj) {
		featureMap.put(feature, obj);
	}

}
