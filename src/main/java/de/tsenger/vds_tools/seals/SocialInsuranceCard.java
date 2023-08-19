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

import java.nio.charset.StandardCharsets;
import java.util.List;


/**
 * Created by Tobias Senger on 12.01.2017.
 */

public class SocialInsuranceCard extends DigitalSeal {

    private String socialInsuranceNumber = "";
    private String surName = "";
    private String firstName = "";
    private String birthName = "**********";

    public enum Feature {
        SOCIAL_INSURANCE_NUMBER,
        SURNAME,
        FIRST_NAME,
        BIRTH_NAME
    }

    public SocialInsuranceCard(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
        featureMap.put(Feature.SOCIAL_INSURANCE_NUMBER, socialInsuranceNumber);
        featureMap.put(Feature.SURNAME, surName);
        featureMap.put(Feature.FIRST_NAME, firstName);
        featureMap.put(Feature.BIRTH_NAME, birthName);
    }

    private void parseDocumentFeatures(List<DocumentFeature> features) throws IllegalArgumentException {
        for (DocumentFeature feature : features) {
            switch (feature.getTag()) {
                case 0x01:
                    socialInsuranceNumber = DataParser.decodeC40(feature.getValue());
                    break;
                case 0x02:
                    surName = new String(feature.getValue(), StandardCharsets.UTF_8);
                    break;
                case 0x03:
                    firstName = new String(feature.getValue(), StandardCharsets.UTF_8);
                    break;
                case 0x04:
                    birthName = new String(feature.getValue(), StandardCharsets.UTF_8);
                    break;
                default:
                    Logger.info("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
                    throw new IllegalArgumentException("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }

    }
}
