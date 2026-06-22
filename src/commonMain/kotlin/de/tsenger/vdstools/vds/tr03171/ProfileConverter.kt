package de.tsenger.vdstools.vds.tr03171

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.vds.dto.VdsProfileDefinitionDto
import de.tsenger.vdstools.vds.dto.MessageDto

object ProfileConverter {

    /**
     * Converts a parsed TR-03171 XML profile to a [VdsProfileDefinitionDto] that can be
     * registered in the [de.tsenger.vdstools.VdsProfileDefinitionRegistry].
     *
     * @param profile The parsed profile DTO from [ProfileXmlParser].
     * @param baseDocumentType The VDS document type that carries seals of this profile in its
     *   header. Use [DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8] for legacy 0xC8 seals (TR-03171
     *   up to v0.8) and [DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9] for 0xC9 seals (TR-03171 v0.9
     *   and later). Defaults to [DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9] — pass
     *   [DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8] explicitly when working with legacy 0xC8 seals.
     */
    fun toVdsProfileDefinition(
        profile: ProfileDto,
        baseDocumentType: String = DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9
    ): VdsProfileDefinitionDto {
        return VdsProfileDefinitionDto(
            definitionId = profile.profileNumber.lowercase(),
            definitionName = profile.profileName,
            baseDocumentType = baseDocumentType,
            version = 1,
            messages = profile.entries.map { toMessageDto(it) }
        )
    }

    private fun toMessageDto(entry: ProfileEntryDto): MessageDto {
        val coding = mapCoding(entry.type, entry.length)
        val maxLength = mapMaxLength(entry.type, entry.length)
        return MessageDto(
            name = entry.name,
            tag = entry.tag,
            coding = coding,
            required = !entry.optional,
            minBytes = 1,
            maxBytes = maxLength
        )
    }

    private fun mapCoding(type: Asn1Type, length: Int?): MessageCoding {
        return when (type) {
            Asn1Type.BOOLEAN -> MessageCoding.BYTE
            Asn1Type.INTEGER -> if (length != null && length == 1) MessageCoding.BYTE else MessageCoding.BYTES
            Asn1Type.OCTET_STRING -> MessageCoding.BYTES
            Asn1Type.UTF8String -> MessageCoding.UTF8_STRING
            // TR-03171 uses ASN.1 DATE as YYYYMMDD UTF-8 (8 bytes), not the 3-byte ICAO binary format
            Asn1Type.DATE -> MessageCoding.DATE_STRING
            Asn1Type.DATE_TIME -> MessageCoding.BYTES
        }
    }

    private fun mapMaxLength(type: Asn1Type, length: Int?): Int {
        return when (type) {
            Asn1Type.BOOLEAN -> 1
            // DATE_STRING is 8 bytes (YYYYMMDD as UTF-8), not 3 bytes like the ICAO binary format
            Asn1Type.DATE -> 8
            Asn1Type.DATE_TIME -> 6
            else -> length ?: 255
        }
    }
}
