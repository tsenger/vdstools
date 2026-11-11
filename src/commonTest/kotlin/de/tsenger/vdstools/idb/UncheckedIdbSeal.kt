package de.tsenger.vdstools.idb

import de.tsenger.vdstools.generic.SealParser

/**
 * Builds a seal without the builder's format check and parses it again, to test how faulty content
 * is read (`IdbSeal.Builder.build()` rejects it).
 */
internal fun uncheckedIdbSeal(country: String, block: IdbMessageGroup.Builder.() -> Unit): IdbSeal {
    val group = IdbMessageGroup.Builder(issuingCountry = country).apply(block).build()
    val seal = IdbSeal(isSigned = false, isZipped = false, barcodePayload = IdbPayload(IdbHeader(country), group, null, null))
    return SealParser().parse(seal.rawString) as IdbSeal
}
