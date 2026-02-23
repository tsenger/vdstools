package de.tsenger.vdstools.annotation

import de.tsenger.vdstools.generic.Seal
import de.tsenger.vdstools.idb.IdbSeal
import de.tsenger.vdstools.vds.VdsSeal

/**
 * Returns a [SealAnnotation] describing the byte structure of this seal's [Seal.encoded] bytes.
 */
fun Seal.annotate(): SealAnnotation = when (this) {
    is VdsSeal -> annotate()
    is IdbSeal -> annotate()
    else -> throw IllegalArgumentException("No annotator for seal type: ${this::class.simpleName}")
}