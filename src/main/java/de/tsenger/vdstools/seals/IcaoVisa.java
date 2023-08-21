package de.tsenger.vdstools.seals;

import java.util.List;

import org.tinylog.Logger;

import de.tsenger.vdstools.DataParser;

/**
 * Created by Tobias Senger on 04.11.2019.
 */

public class IcaoVisa extends DigitalSeal {

    private String mrz;
    private int numberOfEntries = -1; // optional field
    private int durationOfStay_days;
    private int durationOfStay_months;
    private int durationOfStay_years;
    private String passportNumber = "";
    private byte[] visaType = null; // optional field
    private byte[] additionalFeatures = null; // optional field
    private int mrzCharsPerLine;

    public IcaoVisa(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
        featureMap.put(Feature.MRZ, mrz);
        featureMap.put(Feature.DURATION_OF_STAY_YEARS, durationOfStay_years);
        featureMap.put(Feature.DURATION_OF_STAY_MONTHS, durationOfStay_months);
        featureMap.put(Feature.DURATION_OF_STAY_DAYS, durationOfStay_days);
        featureMap.put(Feature.PASSPORT_NUMBER, passportNumber);
        if (numberOfEntries != -1) {
            featureMap.put(Feature.NUMBER_OF_ENTRIES, numberOfEntries);
        }
        if (visaType != null) {
            featureMap.put(Feature.VISA_TYPE, visaType);
        }
        if (additionalFeatures != null) {
            featureMap.put(Feature.ADDITIONAL_FEATURES, additionalFeatures);
        }
    }

    private void parseDocumentFeatures(List<DocumentFeature> features) {
        for (DocumentFeature feature : features) {
            switch (feature.getTag()) {
            case 0x01:
                mrzCharsPerLine = 44;
                String short_mrz = DataParser.decodeC40(feature.getValue()).replace(' ', '<');
                // fill mrz to the full length of 88 characters because ICAO cuts last 16
                // characters
                mrz = String.format("%1$-88s", short_mrz).replace(' ', '<');
                featureMap.put(Feature.MRZ, mrz);
                break;
            case 0x02:
                mrzCharsPerLine = 36;
                short_mrz = DataParser.decodeC40(feature.getValue()).replace(' ', '<');
                // fill mrz to the full length of 72 characters because ICAO cuts last 8
                // characters
                mrz = String.format("%1$-72s", short_mrz).replace(' ', '<');
                featureMap.put(Feature.MRZ, mrz);
                break;
            case 0x03:
                numberOfEntries = feature.getValue()[0] & 0xff;
                break;
            case 0x04:
                decodeDuration(feature.getValue());
                break;
            case 0x05:
                passportNumber = DataParser.decodeC40(feature.getValue());
                break;
            case 0x06:
                visaType = feature.getValue();
                break;
            case 0x07:
                additionalFeatures = feature.getValue();
                break;
            default:
                Logger.info("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
                throw new IllegalArgumentException("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }
    }

    private void decodeDuration(byte[] bytes) {
        if (bytes.length != 3)
            throw new IllegalArgumentException("expected three bytes for date decoding");
        durationOfStay_days = bytes[0] & 0xff;
        durationOfStay_months = bytes[1] & 0xff;
        durationOfStay_years = bytes[2] & 0xff;
    }

    public String getDurationOfStay() {
        return (String.format("%02d", durationOfStay_days) + "d, " + String.format("%02d", durationOfStay_months)
                + "m, " + String.format("%02d", durationOfStay_years) + "y");
    }

}
