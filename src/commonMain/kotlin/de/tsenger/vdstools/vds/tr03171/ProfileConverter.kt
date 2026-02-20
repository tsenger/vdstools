package de.tsenger.vdstools.vds.tr03171

import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.vds.dto.ExtendedMessageDefinitionDto
import de.tsenger.vdstools.vds.dto.MessageDto

object ProfileConverter {

    fun toExtendedMessageDefinition(profile: ProfileDto): ExtendedMessageDefinitionDto {
        return ExtendedMessageDefinitionDto(
            definitionId = profile.profileNumber.lowercase(),
            definitionName = profile.profileName,
            baseDocumentType = "ADMINISTRATIVE_DOCUMENTS",
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
            Asn1Type.DATE -> MessageCoding.DATE
            Asn1Type.DATE_TIME -> MessageCoding.BYTES
        }
    }

    private fun mapMaxLength(type: Asn1Type, length: Int?): Int {
        return when (type) {
            Asn1Type.BOOLEAN -> 1
            Asn1Type.DATE -> 3
            Asn1Type.DATE_TIME -> 6
            else -> length ?: 255
        }
    }
}
