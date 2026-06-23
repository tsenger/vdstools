package de.tsenger.vdstools.vds.tr03171

import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.vds.dto.VdsProfileDefinitionDto
import de.tsenger.vdstools.vds.dto.MessageDto

object ProfileConverter {

    /**
     * Converts a parsed TR-03171 v0.9 XML profile to a [VdsProfileDefinitionDto] that can be
     * registered in the [de.tsenger.vdstools.VdsProfileDefinitionRegistry].
     *
     * Profiles parsed from XML are always TR-03171 v0.9 and are therefore carried by
     * [DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9] (document category 0xC9). Legacy 0xC8 seals are
     * decoded via the bundled JSON profile definitions, not through this converter.
     *
     * @param profile The parsed profile DTO from [ProfileXmlParser].
     */
    fun toVdsProfileDefinition(profile: ProfileDto): VdsProfileDefinitionDto {
        return VdsProfileDefinitionDto(
            definitionId = profile.profileNumber.lowercase(),
            // profileName is optional in v0.9 — fall back to the (mandatory) profile number
            definitionName = profile.profileName ?: profile.profileNumber,
            baseDocumentType = DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9,
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
