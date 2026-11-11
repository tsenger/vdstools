package de.tsenger.vdstools.internal

/**
 * Normalizes a three-letter country code for comparison: C40 fillers (`<`) and padding spaces are
 * removed, e.g. "D<<" and "D  " become "D".
 */
internal fun normalizeCountry(code: String): String = code.filter { it != '<' && it != ' ' }.uppercase()
