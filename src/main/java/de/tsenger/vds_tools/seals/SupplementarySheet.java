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

import java.util.List;

import de.tsenger.vds_tools.DataParser;

/**
 * Created by Tobias Senger on 08.11.2019
 */
public class SupplementarySheet extends DigitalSeal {

    private String mrz = "";
    private String suppSheetNumber = "";

    public enum Feature {
        MRZ,
        SHEET_NUMBER
    }

    public SupplementarySheet(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
        featureMap.put(Feature.MRZ, mrz);
        featureMap.put(Feature.SHEET_NUMBER, suppSheetNumber);
    }


    private void parseDocumentFeatures(List<DocumentFeature> features) {
        for (DocumentFeature feature : features) {
            switch (feature.getTag()) {
                case 0x04:
                    mrz = DataParser.decodeC40(feature.getValue()).replace(' ', '<');
                    break;
                case 0x05:
                    suppSheetNumber = DataParser.decodeC40(feature.getValue());
                    break;
                default:
                    Logger.info("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
                    throw new IllegalArgumentException("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }
    }

}
