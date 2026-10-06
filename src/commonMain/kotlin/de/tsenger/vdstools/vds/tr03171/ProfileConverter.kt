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
            validFromPresent = profile.validFromPresent,
            validToPresent = profile.validToPresent,
            messages = profile.entries.map { toMessageDto(it) }
        )
    }

    private fun toMessageDto(entry: ProfileEntryDto): MessageDto {
        val coding = mapCoding(entry.type)
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

    private fun mapCoding(type: Asn1Type): MessageCoding {
        return when (type) {
            Asn1Type.BOOLEAN -> MessageCoding.BOOLEAN
            // the coding is always the dedicated INTEGER coding, independent of length
            Asn1Type.INTEGER -> MessageCoding.INTEGER
            Asn1Type.OCTET_STRING -> MessageCoding.BYTES
            Asn1Type.UTF8String -> MessageCoding.UTF8_STRING
            // TR-03171 uses ASN.1 DATE as YYYYMMDD UTF-8 (8 bytes), not the 3-byte ICAO binary format
            Asn1Type.DATE -> MessageCoding.DATE_STRING
            // analogous to DATE: ASN.1 DATE-TIME per X.690 as YYYYMMDDHHMMSS UTF-8 (14 bytes), not the 6-byte ICAO format
            Asn1Type.DATE_TIME -> MessageCoding.DATE_TIME_STRING
        }
    }

    private fun mapMaxLength(type: Asn1Type, length: Int?): Int {
        return when (type) {
            Asn1Type.BOOLEAN -> 1
            // TR-03171 §4: length is to be ignored for INTEGER; 8 bytes is the INTEGER coding limit
            Asn1Type.INTEGER -> 8
            // DATE_STRING is 8 bytes (YYYYMMDD as UTF-8), not 3 bytes like the ICAO binary format
            Asn1Type.DATE -> 8
            Asn1Type.DATE_TIME -> 14
            else -> length ?: 255
        }
    }
}
