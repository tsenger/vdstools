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

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.tinylog.Logger;

import java.io.IOException;
import java.security.*;
import java.security.interfaces.ECPrivateKey;

public class Signer {

	private BCECPrivateKey ecPrivKey;

	public Signer(ECPrivateKey privKey) {
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

	public byte[] sign(byte[] dataToSign) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException,
			InvalidAlgorithmParameterException, IOException, NoSuchProviderException {
		if (ecPrivKey == null) {
			throw new InvalidKeyException("private key not initialized. Load from file or generate new one.");
		}

		// Changed 02.12.2021:
		// Signature depends now on the curves bit length according to BSI TR-03116-2
		// 2024-10-20: even more precise Doc9309-13 chapter 2.4
		int fieldBitLength = getFieldSize();
		Signature ecdsaSign;
		if (fieldBitLength <= 224) {
			ecdsaSign = Signature.getInstance("SHA224withPLAIN-ECDSA", "BC");
		} else if (fieldBitLength <= 256) {
			ecdsaSign = Signature.getInstance("SHA256withPLAIN-ECDSA", "BC");
		} else if (fieldBitLength <= 384) {
			ecdsaSign = Signature.getInstance("SHA384withPLAIN-ECDSA", "BC");
		} else if (fieldBitLength <= 512) {
			ecdsaSign = Signature.getInstance("SHA512withPLAIN-ECDSA", "BC");
		} else {
			Logger.error("Bit length of Field is out of defined value: " + fieldBitLength);
			throw new InvalidAlgorithmParameterException(
					"Bit length of Field is out of defined value (224 to 512 bits): " + fieldBitLength);
		}

		Logger.info("ECDSA algorithm: " + ecdsaSign.getAlgorithm());

		ecdsaSign.initSign(ecPrivKey);
		ecdsaSign.update(dataToSign);

		return ecdsaSign.sign();
	}
}
