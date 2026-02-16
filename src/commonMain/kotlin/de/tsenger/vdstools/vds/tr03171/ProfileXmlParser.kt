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
        var profileName: String? = null
        var creator: String? = null
        var category: String? = null
        var leikaID: String? = null
        var statusIndicator: StatusIndicator? = null
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
                "profileName" -> profileName = reader.readElementText()
                "creator" -> creator = reader.readElementText()
                "category" -> category = reader.readElementText()
                "leikaID" -> leikaID = reader.readElementText()
                "statusIndicator" -> statusIndicator = StatusIndicator.valueOf(reader.readElementText())
                "entry" -> entries.add(parseEntry(reader))
                else -> skipElement(reader)
            }
        }

        return ProfileDto(
            profileNumber = requireNotNull(profileNumber) { "profileNumber is required" },
            profileName = requireNotNull(profileName) { "profileName is required" },
            creator = requireNotNull(creator) { "creator is required" },
            category = category,
            leikaID = leikaID,
            statusIndicator = statusIndicator,
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
        require(profile.entries.isNotEmpty()) { "At least one entry required" }
        require(profile.entries.size <= 251) { "Maximum 251 entries allowed" }

        val tags = profile.entries.map { it.tag }
        require(tags.distinct().size == tags.size) { "Duplicate tags found" }
        profile.entries.forEach { entry ->
            require(entry.tag in 4..254) { "Tag ${entry.tag} out of range 4-254" }
        }
    }
}
