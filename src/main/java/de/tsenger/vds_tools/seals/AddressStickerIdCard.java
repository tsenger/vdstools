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


public class AddressStickerIdCard extends DigitalSeal {

    private String docNr = "";
    private String ags = "";
    private String rawAddress = "";
    private String postalCode;
    private String street;
    private String streetNr;

    public enum Feature {
        DOCUMENT_NUMBER,
        AGS,
        RAW_ADDRESS,
        POSTAL_CODE,
        STREET,
        STREET_NR
    }

    public AddressStickerIdCard(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
        parseAddress();
        featureMap.put(Feature.DOCUMENT_NUMBER, docNr);
        featureMap.put(Feature.AGS, ags);
        featureMap.put(Feature.RAW_ADDRESS, rawAddress);
        featureMap.put(Feature.POSTAL_CODE, postalCode);
        featureMap.put(Feature.STREET, street);
        featureMap.put(Feature.STREET_NR, streetNr);
    }

    private void parseDocumentFeatures(List<DocumentFeature> features) {
        for (DocumentFeature feature : features) {
            if (feature.getTag() == 0x01) {
                docNr = DataParser.decodeC40(feature.getValue());
                Logger.debug("Decoded document number: " + docNr);
            } else if (feature.getTag() == 0x02) {
                ags = DataParser.decodeC40(feature.getValue());
                Logger.debug("Decoded AGS: " + ags);
            } else if (feature.getTag() == 0x03) {
                rawAddress = DataParser.decodeC40(feature.getValue());
                Logger.debug("Decoded (raw) address: " + rawAddress);
            } else {
                Logger.info("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
                throw new IllegalArgumentException("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }
    }

    private void parseAddress() {
        postalCode = rawAddress.substring(0, 5);
        street = rawAddress.substring(5).replaceAll("(\\d+\\w+)(?!.*\\d)", "");
        streetNr = rawAddress.substring(5).replaceAll(street, "");
        Logger.debug("parsed address: " + String.format("%s:%s:%s", postalCode, street, streetNr));
    }

}
