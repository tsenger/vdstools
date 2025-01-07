package vdstools

import vdstools.generated.CountryCodeMap


object Doc9303CountryCodes {
    // Mapping für ICAO DOC 9303 Sonderfälle
    private val doc9303Codes: MutableMap<String, String> = HashMap()

    init {
        // Supranationale Organisationen
        doc9303Codes["EU"] = "EUE" // Europäische Union
        doc9303Codes["UN"] = "UNO" // Vereinte Nationen

        // Sonderfall für Deutschland
        doc9303Codes["DE"] = "D<<" // Deutschland

        // andere Sonderfälle
        doc9303Codes["UT"] = "UTO" // Utopia
        doc9303Codes["IA"] = "IAO" // ICAO
        doc9303Codes["NT"] = "NTZ" // Neutral Zone
        doc9303Codes["AN"] = "ANT" // Netherlands Antilles
    }

    // Methode zur Umwandlung von ISO-3166-Alpha-2 nach ICAO DOC 9303 Code oder
    // ISO-3166-Alpha-3
    @JvmStatic
    fun convertToIcaoOrIso3(alpha2Code: String): String? {
        // Sonderfall nach ICAO DOC 9303 prüfen
        if (doc9303Codes.containsKey(alpha2Code)) {
            return doc9303Codes[alpha2Code]
        }

        return CountryCodeMap.alpha2ToAlpha3[alpha2Code]

    }
}
