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

import java.util.ArrayList;


/**
 * Created by Tobias Senger on 12.01.2017.
 */
public class ArrivalAttestation extends DigitalSeal {

    private String mrz = "";
    private String azr = "";

    public enum Feature {
        MRZ,
        AZR
    }

    public ArrivalAttestation(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        super(vdsHeader, vdsMessage, vdsSignature);
        parseDocumentFeatures(vdsMessage.getDocumentFeatures());
        featureMap.put(Feature.MRZ, mrz);
        featureMap.put(Feature.AZR, azr);
    }



    private void parseDocumentFeatures(ArrayList<DocumentFeature> features) {
        for (DocumentFeature feature : features) {
            if (feature.getTag() == 0x02) {
                mrz = DataParser.decodeC40(feature.getValue()).replace(' ', '<');
            } else if (feature.getTag() == 0x03) {
                azr = DataParser.decodeC40(feature.getValue());
            } else {
                Logger.info("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
                throw new IllegalArgumentException("found unknown tag: 0x" + String.format("%02X ", feature.getTag()));
            }
        }
    }

}
