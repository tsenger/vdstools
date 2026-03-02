package de.tsenger.vdstools.dissect

import de.tsenger.vdstools.generic.Seal
import de.tsenger.vdstools.idb.IdbSeal
import de.tsenger.vdstools.vds.VdsSeal

/**
 * Returns a [SealDissection] describing the byte structure of this seal's [Seal.encoded] bytes.
 */
fun Seal.dissect(): SealDissection = when (this) {
    is VdsSeal -> dissect()
    is IdbSeal -> dissect()
    else -> throw IllegalArgumentException("No dissector for seal type: ${this::class.simpleName}")
}
