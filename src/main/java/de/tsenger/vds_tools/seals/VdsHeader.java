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

import java.time.LocalDate;

public class VdsHeader {

    public byte[] rawBytes;

    public String issuingCountry;
    public String signerIdentifier;
    public String certificateReference;
    public LocalDate issuingDate;
    public LocalDate sigDate;

    public byte docFeatureRef;
    public byte docTypeCat;

    public byte rawVersion;

    public int getDocumentRef() {
        return ((docFeatureRef & 0xFF) << 8) + (docTypeCat & 0xFF);
    }

    @Override
    public String toString() {
        return ("rawVersion: " + (rawVersion & 0xff) +
                "\nissuingCountry: " + issuingCountry +
                "\nsignerIdentifier: " + signerIdentifier +
                "\ncertificateReference: " + certificateReference +
                "\nissuingDate: " + issuingDate +
                "\nsigDate: " + sigDate +
                "\ndocFeatureRef: " + String.format("%02X ", docFeatureRef) +
                ", docTypeCat: " + String.format("%02X ", docTypeCat));
    }

}
