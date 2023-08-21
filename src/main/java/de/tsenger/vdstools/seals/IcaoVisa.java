package de.tsenger.vdstools.seals;

import java.util.List;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataParser;

/**
 * @author Tobias Senger
 *
 */
public class IcaoVisa extends DigitalSeal {

    public IcaoVisa(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
    }

    private void parseDocumentFeatures(List<DocumentFeatureDto> features) {
        for (DocumentFeatureDto feature : features) {
            switch (feature.getTag()) {
            case 0x01:
                // MRZ chars per line: 44
                String short_mrz = DataParser.decodeC40(feature.getValue()).replace(' ', '<');
                // fill mrz to the full length of 88 characters because ICAO cuts last 16
                // characters
                String mrz = String.format("%1$-88s", short_mrz).replace(' ', '<');
                featureMap.put(Feature.MRZ, mrz);
                break;
            case 0x02:
                // MRZ chars per line: 36
                short_mrz = DataParser.decodeC40(feature.getValue()).replace(' ', '<');
                // fill mrz to the full length of 72 characters because ICAO cuts last 8
                // characters
                mrz = String.format("%1$-72s", short_mrz).replace(' ', '<');
                featureMap.put(Feature.MRZ, mrz);
                break;
            case 0x03:
                int numberOfEntries = feature.getValue()[0] & 0xff;
                featureMap.put(Feature.NUMBER_OF_ENTRIES, numberOfEntries);
                break;
            case 0x04:
                decodeDuration(feature.getValue());
                break;
            case 0x05:
                String passportNumber = DataParser.decodeC40(feature.getValue());
                featureMap.put(Feature.PASSPORT_NUMBER, passportNumber);
                break;
            case 0x06:
                byte[] visaType = feature.getValue();
                featureMap.put(Feature.VISA_TYPE, visaType);
                break;
            case 0x07:
                byte[] additionalFeatures = feature.getValue();
                featureMap.put(Feature.ADDITIONAL_FEATURES, additionalFeatures);
                break;
            default:
                Logger.warn("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }
    }

    private void decodeDuration(byte[] bytes) {
        if (bytes.length != 3)
            throw new IllegalArgumentException("expected three bytes for date decoding");
        int durationOfStay_days = bytes[0] & 0xff;
        int durationOfStay_months = bytes[1] & 0xff;
        int durationOfStay_years = bytes[2] & 0xff;

        featureMap.put(Feature.DURATION_OF_STAY_YEARS, durationOfStay_years);
        featureMap.put(Feature.DURATION_OF_STAY_MONTHS, durationOfStay_months);
        featureMap.put(Feature.DURATION_OF_STAY_DAYS, durationOfStay_days);
    }

}
