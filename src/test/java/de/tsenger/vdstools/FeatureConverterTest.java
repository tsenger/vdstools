package de.tsenger.vdstools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import de.tsenger.vdstools.vds.Feature;
import de.tsenger.vdstools.vds.VdsType;

public class FeatureConverterTest {

	@Test
	public void testFeatureConverter() throws FileNotFoundException {
		assertNotNull(new FeatureConverter());
	}

	@Test
	public void testFeatureConverterString() throws FileNotFoundException {
		assertNotNull(new FeatureConverter("src/test/resources/TestSealCodings.json"));
	}

	@Test(expected = FileNotFoundException.class)
	public void testFeatureConverterString_notFound() throws FileNotFoundException {
		assertNotNull(new FeatureConverter("src/test/resources/Codings.json"));
	}

	@Test
	public void testGetFeature() throws IOException {
		FeatureConverter featureConverter = new FeatureConverter();
		Feature feature = featureConverter.getFeature(VdsType.FICTION_CERT,
				DerTlv.fromByteArray(Hex.decode("0306d79519a65306")));
		assertEquals(Feature.PASSPORT_NUMBER, feature);
	}

	@Test
	public void testGetFeature_String() throws IOException {
		FeatureConverter featureConverter = new FeatureConverter();
		String feature = featureConverter.getFeature("FICTION_CERT",
				DerTlv.fromByteArray(Hex.decode("0306d79519a65306")));
		assertEquals("PASSPORT_NUMBER", feature);
	}

	@Test
	public void testGetTag() throws FileNotFoundException {
		FeatureConverter featureConverter = new FeatureConverter();
		byte tag = featureConverter.getTag(VdsType.ALIENS_LAW, Feature.AZR);
		assertEquals(4, tag);
	}

	@Test
	public void testGetTag_String() throws FileNotFoundException {
		FeatureConverter featureConverter = new FeatureConverter();
		byte tag = featureConverter.getTag("ALIENS_LAW", "AZR");
		assertEquals(4, tag);
	}

	@Test
	public void testDecodeFeature() throws IOException {
		FeatureConverter featureConverter = new FeatureConverter();
		String value = featureConverter.decodeFeature(VdsType.FICTION_CERT,
				DerTlv.fromByteArray(Hex.decode("0306d79519a65306")));
		assertEquals("UFO001979", value);
	}

	@Test
	public void testDecodeFeature_String() throws IOException {
		FeatureConverter featureConverter = new FeatureConverter();
		String value = featureConverter.decodeFeature("FICTION_CERT",
				DerTlv.fromByteArray(Hex.decode("0306d79519a65306")));
		assertEquals("UFO001979", value);
	}

	@Test
	public void testEncodeFeature() throws IOException {
		FeatureConverter featureConverter = new FeatureConverter();
		String mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06";
		DerTlv derTlv = featureConverter.encodeFeature(VdsType.RESIDENCE_PERMIT, Feature.MRZ, mrz);
		assertEquals(
				"02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b",
				Hex.toHexString(derTlv.getEncoded()));
	}

	@Test
	public void testEncodeFeature_String() throws IOException {
		FeatureConverter featureConverter = new FeatureConverter();
		String mrz = "ATD<<RESIDORCE<<ROLAND<<<<<<<<<<<<<<6525845096USA7008038M2201018<<<<<<06";
		DerTlv derTlv = featureConverter.encodeFeature("RESIDENCE_PERMIT", "MRZ", mrz);
		assertEquals(
				"02305cba135875976ec066d417b59e8c6abc133c133c133c133c3fef3a2938ee43f1593d1ae52dbb26751fe64b7c133c136b",
				Hex.toHexString(derTlv.getEncoded()));
	}

	@Test
	public void testGetAvailableVdsTypes() throws FileNotFoundException {
		FeatureConverter featureConverter = new FeatureConverter();
		System.out.println(featureConverter.getAvailableVdsTypes());
		assertTrue(featureConverter.getAvailableVdsTypes().contains("ADDRESS_STICKER_ID"));
	}

	@Test
	public void testGetAvailableVdsFeatures() throws FileNotFoundException {
		FeatureConverter featureConverter = new FeatureConverter();
		System.out.println(featureConverter.getAvailableVdsFeatures());
		assertTrue(featureConverter.getAvailableVdsFeatures().contains("MRZ"));
	}

}
