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



import de.tsenger.vds_tools.DataParser;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.util.Arrays;


/**
 * Created by Tobias Senger on 03.08.2016.
 */
public abstract class DigitalSeal {

    private VdsType vdsType;

    private VdsHeader vdsHeader;

    private VdsMessage vdsMessage;

    private VdsSignature vdsSignature;

    private String rawString;

    @SuppressWarnings("rawtypes")
    protected Map<Enum, Object> featureMap = new HashMap<>();

    public DigitalSeal(VdsHeader vdsHeader, VdsMessage vdsMessage, VdsSignature vdsSignature) {
        this.vdsHeader = vdsHeader;
        this.vdsMessage = vdsMessage;
        this.vdsSignature = vdsSignature;
        this.vdsType = VdsType.valueOf(vdsHeader.getDocumentRef());
    }

    public static DigitalSeal getInstance(String rawString) {
        DigitalSeal seal = DataParser.parseVdsSeal(rawString);
        seal.rawString = rawString;
        return seal;
    }

    public VdsType getVdsType() {
        return vdsType;
    }

    public ArrayList<DocumentFeature> getDocumentFeatures() {
        return vdsMessage.getDocumentFeatures();
    }

    public String getIssuingCountry() {
        return vdsHeader.issuingCountry;
    }

    public String getSignerCertRef() {
        return (vdsHeader.signerIdentifier + vdsHeader.certificateReference);
    }

    public String getSignerIdentifier() {
        return getSignerCertRef().substring(0, 4);
    }

    public BigInteger getCertSerialNumber() {
        return new BigInteger(getSignerCertRef().substring(4), 16);
    }

    public LocalDate getIssuingDate() {
        return vdsHeader.issuingDate;
    }

    public LocalDate getSigDate() {
        return vdsHeader.sigDate;
    }

    public byte getDocFeatureRef() {
        return vdsHeader.docFeatureRef;
    }

    public byte getDocTypeCat() {
        return vdsHeader.docTypeCat;
    }

    public byte[] getHeaderAndMessageBytes() {
        return Arrays.concatenate(vdsHeader.rawBytes, vdsMessage.getRawBytes());
    }

    public byte[] getSignatureBytes() {
        return vdsSignature.getSignatureBytes();
    }

    public String getRawString() {
        return rawString;
    }

    public Object getFeature(Enum feature) {
        try {
            return featureMap.get(feature);
        } catch (Exception e) {
            return null;
        }
    }

}
