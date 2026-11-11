package de.tsenger.vdstools.internal

/**
 * Normalizes a three-letter country code for comparison: C40 fillers (`<`) and padding spaces are
 * removed, e.g. "D<<" and "D  " become "D".
 */
internal fun normalizeCountry(code: String): String = code.filter { it != '<' && it != ' ' }.uppercase()

/**
 * The one lookup rule for definitions that may be national: among [candidates] for the same key, the
 * definition of [issuingCountry] first, then an international one (no country), then the national
 * definition of another country if it is the only candidate left. Null if none or ambiguous.
 */
internal fun <T> selectForCountry(candidates: List<T>, issuingCountry: String?, countryOf: (T) -> String?): T? {
    val country = issuingCountry?.let(::normalizeCountry)
    candidates.lastOrNull { def -> countryOf(def)?.let(::normalizeCountry)?.let { it == country } == true }?.let { return it }
    candidates.lastOrNull { countryOf(it) == null }?.let { return it }
    return candidates.singleOrNull()
}
