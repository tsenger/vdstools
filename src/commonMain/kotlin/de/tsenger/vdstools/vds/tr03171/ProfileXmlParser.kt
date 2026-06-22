package de.tsenger.vdstools.vds.tr03171

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlStreaming

object ProfileXmlParser {

    fun parse(xmlString: String): ProfileDto {
        val reader = xmlStreaming.newReader(xmlString)
        val profile = parseProfile(reader)
        validate(profile)
        return profile
    }

    private fun parseProfile(reader: XmlReader): ProfileDto {
        var profileNumber: String? = null
        var versionTR: String? = null
        var profileName: String? = null
        var creator: String? = null
        var category: String? = null
        var leikaID: String? = null
        var validFromPresent: Boolean? = null
        var validToPresent: Boolean? = null
        val entries = mutableListOf<ProfileEntryDto>()

        // Advance to root element
        while (reader.hasNext()) {
            val event = reader.next()
            if (event == EventType.START_ELEMENT && reader.localName == "profile") break
        }

        // Parse children of <profile>
        while (reader.hasNext()) {
            val event = reader.next()
            if (event == EventType.END_ELEMENT && reader.localName == "profile") break
            if (event != EventType.START_ELEMENT) continue

            when (reader.localName) {
                "profileNumber" -> profileNumber = reader.readElementText()
                "versionTR" -> versionTR = reader.readElementText()
                "profileName" -> profileName = reader.readElementText()
                "creator" -> creator = reader.readElementText()
                "category" -> category = reader.readElementText()
                "leikaID" -> leikaID = reader.readElementText()
                "validFromPresent" -> validFromPresent = reader.readBoolean("validFromPresent")
                "validToPresent" -> validToPresent = reader.readBoolean("validToPresent")
                "entry" -> entries.add(parseEntry(reader))
                else -> skipElement(reader)
            }
        }

        return ProfileDto(
            profileNumber = requireNotNull(profileNumber) { "profileNumber is required" },
            versionTR = requireNotNull(versionTR) { "versionTR is required" },
            profileName = profileName,
            creator = creator,
            category = category,
            leikaID = leikaID,
            validFromPresent = requireNotNull(validFromPresent) { "validFromPresent is required" },
            validToPresent = requireNotNull(validToPresent) { "validToPresent is required" },
            entries = entries
        )
    }

    private fun parseEntry(reader: XmlReader): ProfileEntryDto {
        val tag = reader.getAttributeValue(null, "tag")?.toIntOrNull()
            ?: throw IllegalArgumentException("entry requires a 'tag' attribute")
        val optional = reader.getAttributeValue(null, "optional")?.toBooleanStrictOrNull() ?: false

        var name: String? = null
        var description: String? = null
        var length: Int? = null
        var type: Asn1Type? = null
        var defaultValue: String? = null

        while (reader.hasNext()) {
            val event = reader.next()
            if (event == EventType.END_ELEMENT && reader.localName == "entry") break
            if (event != EventType.START_ELEMENT) continue

            when (reader.localName) {
                "name" -> name = reader.readElementText()
                "description" -> description = reader.readElementText()
                "length" -> length = reader.readElementText().toInt()
                "type" -> type = parseAsn1Type(reader.readElementText())
                "defaultValue" -> defaultValue = reader.readElementText()
                else -> skipElement(reader)
            }
        }

        return ProfileEntryDto(
            tag = tag,
            optional = optional,
            name = requireNotNull(name) { "entry/name is required" },
            description = requireNotNull(description) { "entry/description is required" },
            length = length,
            type = requireNotNull(type) { "entry/type is required" },
            defaultValue = defaultValue
        )
    }

    private fun parseAsn1Type(value: String): Asn1Type {
        return when (value) {
            "BOOLEAN" -> Asn1Type.BOOLEAN
            "INTEGER" -> Asn1Type.INTEGER
            "OCTET_STRING" -> Asn1Type.OCTET_STRING
            "UTF8String" -> Asn1Type.UTF8String
            "DATE" -> Asn1Type.DATE
            "DATE-TIME" -> Asn1Type.DATE_TIME
            else -> throw IllegalArgumentException("Unknown ASN.1 type: $value")
        }
    }

    private fun XmlReader.readBoolean(elementName: String): Boolean {
        val text = readElementText().trim()
        return text.toBooleanStrictOrNull()
            ?: throw IllegalArgumentException("$elementName must be 'true' or 'false', got: $text")
    }

    private fun XmlReader.readElementText(): String {
        val sb = StringBuilder()
        while (hasNext()) {
            val event = next()
            if (event == EventType.END_ELEMENT) break
            if (event == EventType.TEXT || event == EventType.CDSECT) {
                sb.append(text)
            }
        }
        return sb.toString()
    }

    private fun skipElement(reader: XmlReader) {
        var depth = 1
        while (reader.hasNext() && depth > 0) {
            when (reader.next()) {
                EventType.START_ELEMENT -> depth++
                EventType.END_ELEMENT -> depth--
                else -> {}
            }
        }
    }

    private fun validate(profile: ProfileDto) {
        require(profile.profileNumber.matches(Regex("[0-9A-F]{32}"))) {
            "profileNumber must be 32 uppercase hex characters"
        }
        require(profile.versionTR.isNotBlank()) { "versionTR must not be blank" }
        if (profile.leikaID != null) {
            require(profile.leikaID.matches(Regex("\\d{14}(;\\d{14})*"))) {
                "leikaID must be one or more 14-digit numeric IDs separated by ';'"
            }
        }
        require(profile.entries.isNotEmpty()) { "At least one entry required" }
        // Tags 0x00-0x09 are reserved (metadata 0x00-0x06, future use 0x07-0x09);
        // profile entries use 0x0A-0xFE (10-254), giving at most 245 entries.
        require(profile.entries.size <= 245) { "Maximum 245 entries allowed" }

        val tags = profile.entries.map { it.tag }
        require(tags.distinct().size == tags.size) { "Duplicate tags found" }
        profile.entries.forEach { entry ->
            require(entry.tag in 10..254) { "Tag ${entry.tag} out of range 10-254 (0x0A-0xFE)" }
        }
    }
}
