package de.tsenger.vdstools.generic

import de.tsenger.vdstools.idb.IdbSeal
import de.tsenger.vdstools.tddoc.TdDocSeal
import de.tsenger.vdstools.vds.VdsSeal

class SealParser(val allowedTypes: Set<SealType> = SealType.entries.toSet()) {

    @Throws(SealParseException::class)
    fun parse(input: String): Seal {
        val type = detectType(input)
            ?: throw SealParseException("Unknown seal format: input does not match any known seal type")
        if (type !in allowedTypes)
            throw SealParseException("Seal type $type is not in allowed types: $allowedTypes")
        return try {
            when (type) {
                SealType.VDS    -> VdsSeal.fromRawString(input)
                SealType.IDB    -> IdbSeal.fromString(input)
                SealType.TDDOC  -> TdDocSeal.fromRawString(input)
            }
        } catch (e: SealParseException) {
            throw e
        } catch (e: Exception) {
            throw SealParseException("Failed to parse $type seal: ${e.message}", e)
        }
    }

    @Throws(SealParseException::class)
    fun parse(bytes: ByteArray): Seal {
        if (SealType.VDS !in allowedTypes)
            throw SealParseException("Seal type VDS is not in allowed types: $allowedTypes")
        return try {
            VdsSeal.fromByteArray(bytes)
        } catch (e: SealParseException) {
            throw e
        } catch (e: Exception) {
            throw SealParseException("Failed to parse VDS seal: ${e.message}", e)
        }
    }

    private fun detectType(input: String): SealType? = when {
        input.startsWith("RDB1") || input.startsWith("NDB1") -> SealType.IDB
        input.startsWith("Ü")                                -> SealType.VDS
        input.startsWith("DC")                               -> SealType.TDDOC
        else                                                 -> null
    }
}
