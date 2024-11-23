package de.tsenger.vdstools;

import de.tsenger.vdstools.vds.Feature;
import de.tsenger.vdstools.vds.VdsMessage;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.tinylog.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FeatureConverterTest {

	@Test
	public void testFeatureConverter() {
		new FeatureConverter();
	}

	@Test
	public void testFeatureConverterString() throws FileNotFoundException {
		File fe = new File("src/test/resources/SealCodings.json");
		FileInputStream fis = new FileInputStream(fe);
		new FeatureConverter(fis);
	}

	@Test(expected = FileNotFoundException.class)
	public void testFeatureConverterString_notFound() throws FileNotFoundException {
		File fe = new File("src/test/resources/Codings.json");
		FileInputStream fis = new FileInputStream(fe);
		new FeatureConverter(fis);
	}

	@Test
	public void testGetFeature_String() throws IOException {
		FeatureConverter featureConverter = new FeatureConverter();
		String feature = featureConverter.getFeatureName("FICTION_CERT",
				DerTlv.fromByteArray(Hex.decode("0306d79519a65306")));
		assertEquals("PASSPORT_NUMBER", feature);
	}

	@Test
	public void testGetTag_String() {
		FeatureConverter featureConverter = new FeatureConverter();
		byte tag = featureConverter.getTag("ALIENS_LAW", "AZR");
		assertEquals(4, tag);
	}

	@Test
	public void testDecodeFeature_String() throws IOException {
		FeatureConverter featureConverter = new FeatureConverter();
		String value = featureConverter.decodeFeature("FICTION_CERT",
				DerTlv.fromByteArray(Hex.decode("0306d79519a65306")));
		assertEquals("UFO001979", value);
	}

	@Test
	public void testEncodeFeature_String() throws IOException {
		FeatureConverter featureConverter = new FeatureConverter();
		String mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<\n6525845096USA7008038M2201018<<<<<<06";
		DerTlv derTlv = featureConverter.encodeFeature("RESIDENCE_PERMIT", "MRZ", mrz);
		assertEquals(
				"02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b",
				Hex.toHexString(derTlv.getEncoded()));
	}

	@Test
	public void testGetAvailableVdsTypes() {
		FeatureConverter featureConverter = new FeatureConverter();
		System.out.println(featureConverter.getAvailableVdsTypes());
		assertTrue(featureConverter.getAvailableVdsTypes().contains("ADDRESS_STICKER_ID"));
	}

	@Test
	public void testGetAvailableVdsFeatures() {
		FeatureConverter featureConverter = new FeatureConverter();
		System.out.println(featureConverter.getAvailableVdsFeatures());
		assertTrue(featureConverter.getAvailableVdsFeatures().contains("MRZ"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetDocumentRef_fakeSeal() {
		DataEncoder.getFeatureEncoder().getDocumentRef("FAKE_SEAL");
	}

	@Test
	public void convertDerTlvToFeatureList() {
		byte[] messageBytes = Hex.decode("02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b0306d79519a65306");
		VdsMessage message = VdsMessage.fromByteArray(messageBytes, "RESIDENCE_PERMIT");
		FeatureConverter featureConverter = new FeatureConverter();
		List<Feature> featureList = featureConverter.convertDerTlvToFeatureList("RESIDENCE_PERMIT", message.getDerTlvList());
		for (Feature feature: featureList) {
			Logger.debug(feature.getName() + ", " + feature.getCoding() + ", " + feature.asString());
		}
	}

}
