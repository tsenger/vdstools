package de.tsenger.vdstools;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

import org.tinylog.Logger;

public class Doc9303CountryCodes {

    // Mapping für ICAO DOC 9303 Sonderfälle
    private static final Map<String, String> doc9303Codes = new HashMap<>();

    static {
        // Supranationale Organisationen
        doc9303Codes.put("EU", "EUE"); // Europäische Union
        doc9303Codes.put("UN", "UNO"); // Vereinte Nationen

        // Sonderfall für Deutschland
        doc9303Codes.put("DE", "D<<"); // Deutschland
        
        // andere Sonderfälle
        doc9303Codes.put("UT", "UTO"); // Utopia
        doc9303Codes.put("IA", "IAO"); // ICAO
        doc9303Codes.put("NT", "NTZ"); // Neutral Zone
        doc9303Codes.put("AN", "ANT"); // Netherlands Antilles
        doc9303Codes.put("AN", "ANT"); //
    }

    // Methode zur Umwandlung von ISO-3166-Alpha-2 nach ICAO DOC 9303 Code oder ISO-3166-Alpha-3
    public static String convertToIcaoOrIso3(String alpha2Code) {
        // Sonderfall nach ICAO DOC 9303 prüfen
        if (doc9303Codes.containsKey(alpha2Code)) {
            return doc9303Codes.get(alpha2Code);
        }

        // Standard-Umwandlung von ISO-3166-Alpha-2 nach ISO-3166-Alpha-3
        Locale locale = new Locale("", alpha2Code);
        try {
            return locale.getISO3Country();
        } catch (MissingResourceException e) {
            Logger.warn("unbekannter Country Code. " + e.getMessage());
            return null;
        }
    }

}

