/* 
 * Copyright (C) 2020 Tobias Senger (info@tsenger.de)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package de.tsenger.vdstools;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.tinylog.Logger;

public class Signer {
	
    private BCECPrivateKey ecPrivKey;


    public Signer(ECPrivateKey privKey) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        this.ecPrivKey = (BCECPrivateKey) privKey;
    }
    
    public Signer(KeyStore keyStore, String keyStorePassword, String keyAlias) {
    	try {
    		this.ecPrivKey = (BCECPrivateKey) keyStore.getKey(keyAlias, keyStorePassword.toCharArray());
		} catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
			Logger.error("getPrivateKeyByAlias failed: " + e.getMessage());
		}
    }

    public int getFieldSize() {
        return ecPrivKey.getParameters().getCurve().getFieldSize();
    }

    public byte[] sign(byte[] dataToSign) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, InvalidAlgorithmParameterException, IOException, NoSuchProviderException {
        if (ecPrivKey == null) {
            throw new InvalidKeyException("private key not initialized. Load from file or generate new one.");
        }
        
        // Changed 02.12.2021:
        // Signature depends now on curves bit length according to BSI TR-03116-2
        Signature ecdsaSign;
        switch (getFieldSize()) {
        case 224:
        	ecdsaSign = Signature.getInstance("SHA224withPLAIN-ECDSA", "BC");
        	break;
        case 256:
        default:
        	ecdsaSign = Signature.getInstance("SHA256withPLAIN-ECDSA", "BC");
        	break;
        }
        
        Logger.info("ECDSA algorithm: " + ecdsaSign.getAlgorithm());

        ecdsaSign.initSign(ecPrivKey);
        ecdsaSign.update(dataToSign);

        return ecdsaSign.sign();
    }
}
