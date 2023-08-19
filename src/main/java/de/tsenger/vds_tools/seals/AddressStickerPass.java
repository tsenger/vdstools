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


public class AddressStickerPass extends DigitalSeal {

    private String docNr = "";
    private String ags = "";
    private String postalCode = "";

    public enum Feature {
        DOCUMENT_NUMBER,
        AGS,
        POSTAL_CODE
    }

    public AddressStickerPass(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
        featureMap.put(Feature.DOCUMENT_NUMBER, docNr);
        featureMap.put(Feature.AGS, ags);
        featureMap.put(Feature.POSTAL_CODE, postalCode);
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
                postalCode = DataParser.decodeC40(feature.getValue());
                Logger.debug("Decoded postal code: " + postalCode);
            } else {
                Logger.info("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
                throw new IllegalArgumentException("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }

    }

}
