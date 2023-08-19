/*
 * Sealva VDS Validator scans and verifies visible digital seals in barcodes
 *     Copyright (C) 2023.  Tobias Senger <sealva@tsenger.de>
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.tsenger.vds_tools.seals;


import org.tinylog.Logger;

import de.tsenger.vds_tools.DataParser;

import java.util.List;



/**
 * Created by Tobias Senger on 04.11.2019.
 */

public class IcaoVisa extends DigitalSeal {

    private String mrz;
    private int numberOfEntries = -1; //optional field
    private int durationOfStay_days;
    private int durationOfStay_months;
    private int durationOfStay_years;
    private String passportNumber = "";
    private byte[] visaType = null; //optional field
    private byte[] additionalFeatures = null; //optional field
    private int mrzCharsPerLine;

    public enum Feature {
        MRZ,
        DURATION_OF_STAY_DAYS,
        DURATION_OF_STAY_MONTHS,
        DURATION_OF_STAY_YEARS,
        PASSPORT_NUMBER,
        NUMBER_OF_ENTRIES,
        VISA_TYPE,
        ADDITIONAL_FEATURES
    }

    public IcaoVisa(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
        featureMap.put(Feature.MRZ, mrz);
        featureMap.put(Feature.DURATION_OF_STAY_YEARS, durationOfStay_years);
        featureMap.put(Feature.DURATION_OF_STAY_MONTHS, durationOfStay_months);
        featureMap.put(Feature.DURATION_OF_STAY_DAYS, durationOfStay_days);
        featureMap.put(Feature.PASSPORT_NUMBER, passportNumber);
        featureMap.put(Feature.NUMBER_OF_ENTRIES, numberOfEntries);
        featureMap.put(Feature.VISA_TYPE, visaType);
        featureMap.put(Feature.ADDITIONAL_FEATURES, additionalFeatures);
    }

    private void parseDocumentFeatures(List<DocumentFeature> features) {
        for (DocumentFeature feature : features) {
            switch (feature.getTag()) {
                case 0x01:
                    mrzCharsPerLine = 44;
                    String short_mrz = DataParser.decodeC40(feature.getValue()).replace(' ', '<');
                    //fill mrz to the full length of 88 characters because ICAO cuts last 16 characters
                    mrz = String.format("%1$-88s", short_mrz).replace(' ', '<');
                    break;
                case 0x02:
                    mrzCharsPerLine = 36;
                    short_mrz = DataParser.decodeC40(feature.getValue()).replace(' ', '<');
                    //fill mrz to the full length of 72 characters because ICAO cuts last 8 characters
                    mrz = String.format("%1$-72s", short_mrz).replace(' ', '<');
                    Logger.debug("Decoded MRZ: " + mrz);
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
        return (String.format("%02d", durationOfStay_days) +
                "d, " + String.format("%02d", durationOfStay_months) +
                "m, " + String.format("%02d", durationOfStay_years) + "y");
    }

}
