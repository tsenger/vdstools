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

import org.bouncycastle.util.encoders.Hex;

/**
 * Created by Tobias Senger on 12.01.2017.
 */

public class DocumentFeature {

    private final byte tag;
    private final int length;
    private final byte[] value;

    public DocumentFeature(byte tag, int len, byte[] val) {
        this.tag = tag;
        this.length = len;
        this.value = val.clone();
    }

    byte getTag() {
        return tag;
    }

    byte[] getValue() {
        return value;
    }

    @Override
    public String toString() {
        return " T: " + String.format("%02X ", tag) + " L: " + String.format("%02X ", length) + " V: " + Hex.toHexString(value);
    }
}
