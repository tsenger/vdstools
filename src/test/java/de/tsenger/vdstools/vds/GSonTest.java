package de.tsenger.vdstools.vds;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import de.tsenger.vdstools.vds.seals.FeaturesDto;
import de.tsenger.vdstools.vds.seals.SealDto;

public class GSonTest {

	private static String jsonFile = "src/main/resources/SealCodings.json";

	@Test
	public void test() throws JsonSyntaxException, IOException {
		Gson gson = new Gson();
		// Definiere den Typ für die Liste von Document-Objekten
		Type listType = new TypeToken<List<SealDto>>() {
		}.getType();

		// Lese die JSON-Datei
		FileReader reader = new FileReader(jsonFile);

		// Parse das JSON in eine Liste von Dokumenten
		List<SealDto> documents = gson.fromJson(reader, listType);

		// Zugriff auf die Daten
		for (SealDto document : documents) {
			System.out.println("Document Type: " + document.documentType);
			System.out.println("Document Ref: " + document.documentRef);
			System.out.println("Version: " + document.version);

			for (FeaturesDto feature : document.features) {
				System.out.println("Feature Name: " + feature.name);
				System.out.println("Optional: " + feature.required);
				System.out.println("Coding: " + feature.coding);
				System.out.println("Min Length: " + feature.minLength);
				System.out.println("Max Length: " + feature.maxLength);
			}
		}

	}

	public String readFile(Path path) throws IOException {
		return Files.readString(path);
	}

}
