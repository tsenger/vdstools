package de.tsenger.vdstools


import co.touchlab.kermit.Logger
import de.tsenger.vdstools.asn1.DerTlv
import de.tsenger.vdstools.vds.tr03171.ProfileConverter
import de.tsenger.vdstools.vds.tr03171.ProfileXmlParser
import de.tsenger.vdstools.generated.ResourceConstants
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.generic.MessageValue
import de.tsenger.vdstools.vds.dto.ExtendedMessageDefinitionDto
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.SHA1
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import okio.*


object DataEncoder {
    private lateinit var vdsSealCodingRegistry: VdsSealCodingRegistry
    private lateinit var idbMessageTypeRegistry: IdbMessageTypeRegistry
    private lateinit var idbDocumentTypeRegistry: IdbNationalDocumentTypeRegistry
    private lateinit var extendedMessageDefinitionRegistry: ExtendedMessageDefinitionRegistry
    private val log = Logger.withTag(this::class.simpleName ?: "")

    init {
        resetToDefaults()
    }

    /**
     * Resets all registries to their default values using the embedded JSON resources.
     *
     * This is useful for testing scenarios where custom configurations have been loaded
     * and need to be reverted to the original state.
     */
    fun resetToDefaults() {
        try {
            // Use generated constants (embedded at compile time)
            vdsSealCodingRegistry = VdsSealCodingRegistry(ResourceConstants.SEAL_CODINGS_JSON)
            idbMessageTypeRegistry = IdbMessageTypeRegistry(ResourceConstants.IDB_MESSAGE_TYPES_JSON)
            idbDocumentTypeRegistry = IdbNationalDocumentTypeRegistry(ResourceConstants.IDB_DOCUMENT_TYPES_JSON)
            extendedMessageDefinitionRegistry =
                ExtendedMessageDefinitionRegistry(ResourceConstants.EXTENDED_MESSAGE_DEFINITIONS_JSON)
            log.i("Reset all registries to defaults")
        } catch (e: Exception) {
            log.e("Failed to initialize from embedded resources: ${e.message}")
            println("Failed to initialize from embedded resources: ${e.message}")
        }
    }

    /**
     * Allows users to override the default SealCodings with custom JSON.
     *
     * @param jsonString Custom SealCodings JSON content
     * @throws Exception if JSON is invalid
     */
    fun loadCustomSealCodings(jsonString: String) {
        vdsSealCodingRegistry = VdsSealCodingRegistry(jsonString)
        log.i("Loaded custom SealCodings")
    }

    /**
     * Allows users to override the default IDB Message Types with custom JSON.
     *
     * @param jsonString Custom IdbMessageTypes JSON content
     * @throws Exception if JSON is invalid
     */
    fun loadCustomIdbMessageTypes(jsonString: String) {
        idbMessageTypeRegistry = IdbMessageTypeRegistry(jsonString)
        log.i("Loaded custom IdbMessageTypes")
    }

    /**
     * Allows users to override the default IDB Document Types with custom JSON.
     *
     * @param jsonString Custom IdbNationalDocumentTypes JSON content
     * @throws Exception if JSON is invalid
     */
    fun loadCustomIdbNationalDocumentTypes(jsonString: String) {
        idbDocumentTypeRegistry = IdbNationalDocumentTypeRegistry(jsonString)
        log.i("Loaded custom IdbDocumentTypes")
    }

    /**
     * Allows users to override the default Extended Message Definitions with custom JSON.
     *
     * @param jsonString Custom ExtendedMessageDefinitions JSON content
     * @throws Exception if JSON is invalid
     */
    fun loadCustomExtendedMessageDefinitions(jsonString: String) {
        extendedMessageDefinitionRegistry = ExtendedMessageDefinitionRegistry(jsonString)
        log.i("Loaded custom ExtendedMessageDefinitions")
    }

    /**
     * Convenience method to load custom JSON from file using readTextResource.
     *
     * Example usage:
     * ```
     * DataEncoder.loadCustomSealCodingsFromFile("custom_seals.json")
     * ```
     */
    @Throws(FileNotFoundException::class)
    fun loadCustomSealCodingsFromFile(fileName: String) {
        loadCustomSealCodings(readTextResource(fileName))
    }

    /**
     * Convenience method to load custom IDB Message Types from file.
     */
    @Throws(FileNotFoundException::class)
    fun loadCustomIdbMessageTypesFromFile(fileName: String) {
        loadCustomIdbMessageTypes(readTextResource(fileName))
    }

    /**
     * Convenience method to load custom IDB Document Types from file.
     */
    @Throws(FileNotFoundException::class)
    fun loadCustomIdbDocumentTypesFromFile(fileName: String) {
        loadCustomIdbNationalDocumentTypes(readTextResource(fileName))
    }

    /**
     * Convenience method to load custom Extended Message Definitions from file.
     */
    @Throws(FileNotFoundException::class)
    fun loadCustomExtendedMessageDefinitionsFromFile(fileName: String) {
        loadCustomExtendedMessageDefinitions(readTextResource(fileName))
    }

    /**
     * Loads an extended message definition from an XML document profile (BSI TR-03171).
     *
     * The XML profile is parsed and converted to an ExtendedMessageDefinitionDto,
     * which is then added to the existing registry without replacing other definitions.
     *
     * @param xmlString XML content conforming to the DocProfileSchema TR-03171
     * @throws IllegalArgumentException if the XML is invalid or fails validation
     */
    fun loadExtendedMessageDefinitionFromXml(xmlString: String) {
        val profile = ProfileXmlParser.parse(xmlString)
        val definition = ProfileConverter.toExtendedMessageDefinition(profile)
        extendedMessageDefinitionRegistry.addDefinition(definition)
        log.i("Loaded extended message definition from XML: ${definition.definitionName}")
    }

    /**
     * @param dateString Date as String formated as yyyy-MM-dd
     * @return date encoded in 3 bytes
     */
    fun encodeDate(dateString: String): ByteArray {
        val dt: LocalDate = LocalDate.parse(dateString)
        return encodeDate(dt)
    }

    /**
     * Encode a LocalDate as described in ICAO Doc9303 Part 13 in three bytes
     *
     * @param localDate Date
     * @return date encoded in 3 bytes
     */
    fun encodeDate(localDate: LocalDate?): ByteArray {
        if (localDate == null) return ByteArray(3)
        val formattedDate: String = localDate.month.number.toString().padStart(2, '0') +
                localDate.day.toString().padStart(2, '0') +
                localDate.year.toString().padStart(4, '0')
        val dateInt = formattedDate.toInt()
        return numberToByteArray(dateInt)

    }


    /**
     * Encode a LocalDate as described in as described in ICAO TR "Datastructure for
     * Barcode" in six bytes.
     *
     * @param localDatetime LocalDateTime to encode
     * @return local date time encoded in 6 bytes
     */
    fun encodeDateTime(localDatetime: LocalDateTime): ByteArray {
        val formattedDateTime: String =
            localDatetime.month.number.toString().padStart(2, '0') +
                    localDatetime.day.toString().padStart(2, '0') +
                    localDatetime.year.toString().padStart(4, '0') +
                    localDatetime.hour.toString().padStart(2, '0') +
                    localDatetime.minute.toString().padStart(2, '0') +
                    localDatetime.second.toString().padStart(2, '0')

        val dateInt = formattedDateTime.toLong()
        return numberToByteArray(dateInt)

    }

    private fun numberToByteArray(value: Number): ByteArray {
        return when (value) {
            is Long -> {
                // Konvertiere Long zu ByteArray (8 Bytes), beschränke auf 6 und ergänze führende Nullen
                val byteArray = ByteArray(8) { i ->
                    (value shr (8 * (7 - i)) and 0xFF).toByte()
                }.takeLast(6).toByteArray()
                // Falls kürzer als 6 Bytes, mit führenden Nullen auffüllen
                ByteArray(6 - byteArray.size) { 0 } + byteArray
            }

            is Int -> {
                // Konvertiere Int zu ByteArray (4 Bytes), beschränke auf 3 und ergänze führende Nullen
                val byteArray = ByteArray(4) { i ->
                    (value shr (8 * (3 - i)) and 0xFF).toByte()
                }.takeLast(3).toByteArray()
                // Falls kürzer als 3 Bytes, mit führenden Nullen auffüllen
                ByteArray(3 - byteArray.size) { 0 } + byteArray
            }

            else -> throw IllegalArgumentException("Unsupported type: ${value::class}")
        }
    }

    /**
     * Encodes a date string with unknown date parts as described in ICAO TR
     * "Datastructure for Barcode". Unknown parts of the date string shall be filled
     * with an 'x', e.g. 19xx-10-xx
     *
     * @param dateString date as String formated as yyyy-MM-dd where unknown parts
     * could be replaced by an x
     * @return masked date encoded in 4 bytes
     */

    fun encodeMaskedDate(dateString: String): ByteArray {
        require(dateString.matches("(.{4})-(.{2})-(.{2})".toRegex())) { "Date string must be formated as yyyy-MM-dd." }

        val formattedDate =
            dateString.replace("(.{4})-(.{2})-(.{2})".toRegex(), "$2$3$1").lowercase()
        val dateInt = formattedDate.replace("x".toRegex(), "0").toInt()
        val dateCharArray = formattedDate.toCharArray()

        var mask: Byte = 0
        for (i in 0..7) {
            if (dateCharArray[i] == 'x') {
                mask = (mask.toInt() or (0x80 shr i)).toByte()
            }
        }
        return byteArrayOf(mask, (dateInt ushr 16).toByte(), (dateInt ushr 8).toByte(), dateInt.toByte())
    }

    fun encodeC40(string: String): ByteArray {
        var dataString = string
        var c1: Int
        var c2: Int
        var c3: Int
        var sum: Int
        val out = Buffer()

        dataString = dataString
            .uppercase()
            .replace("<", " ")
            .replace("\r".toRegex(), "")
            .replace("\n".toRegex(), "")

        val len = dataString.length

        for (i in 0..<len) {
            if (i % 3 == 0) {
                if (i + 2 < len) {
                    // encode standard way
                    c1 = getC40Value(dataString[i])
                    c2 = getC40Value(dataString[i + 1])
                    c3 = getC40Value(dataString[i + 2])
                    sum = (1600 * c1) + (40 * c2) + c3 + 1
                    out.writeByte(sum / 256)
                    out.writeByte(sum % 256)
                } else if (i + 1 < len) {
                    // use zero (Shift1) als filler symbol for c3
                    c1 = getC40Value(dataString[i])
                    c2 = getC40Value(dataString[i + 1])
                    sum = (1600 * c1) + (40 * c2) + 1
                    out.writeByte(sum / 256)
                    out.writeByte(sum % 256)
                } else {
                    // two missing chars: add 0xFE (254 = unlatch) and encode as ASCII
                    // (in datamatrix standard, actual encoded value is ASCII value + 1)
                    out.writeByte(254)
                    out.writeByte(toUnsignedInt(dataString[i].code.toByte()) + 1)
                }
            }
        }
        return out.readByteArray()
    }

    fun formatMRZ(mrzString: String, mrzLength: Int): String {
        val paddedMrz = mrzString
            .padEnd(mrzLength, '<')
            .replace(' ', '<')
        return if (mrzLength == 90) {
            paddedMrz.take(30) + "\n" +
                    paddedMrz.substring(30, 60) + "\n" +
                    paddedMrz.substring(60, 90)
        } else paddedMrz.take(mrzLength / 2) + "\n" +
                paddedMrz.substring(mrzLength / 2)
    }

    private fun getC40Value(c: Char): Int {
        return when (val value = toUnsignedInt(c.code.toByte())) {
            32 -> {
                3
            }

            in 48..57 -> {
                value - 44
            }

            in 65..90 -> {
                value - 51
            }

            else -> {
                throw IllegalArgumentException("Not a C40 encodable char: " + c + "value: " + value)
            }
        }
    }


    fun encodeBase256(ba: ByteArray): String {
        val ca = CharArray(ba.size)
        for (i in ba.indices) {
            ca[i] = (ba[i].toInt() and 0xFF).toChar()
        }
        return ca.concatToString()
    }

    fun zip(bytesToCompress: ByteArray): ByteArray {
        val outputBuffer = Buffer()
        val inputBuffer = Buffer().write(bytesToCompress)
        val compressor = DeflaterSink(outputBuffer, Deflater(9, false))
        compressor.write(inputBuffer, inputBuffer.size)
        compressor.close()
        val compressedBytes = outputBuffer.readByteArray()

        log.d(
            ("Zip ratio " + (bytesToCompress.size.toFloat() / compressedBytes.size.toFloat()) + ", input size "
                    + bytesToCompress.size + ", compressed size " + compressedBytes.size)
        )
        return compressedBytes
    }

    fun setVdsSealCodingRegistry(vdsSealCodingRegistry: VdsSealCodingRegistry) {
        DataEncoder.vdsSealCodingRegistry = vdsSealCodingRegistry
    }


    @OptIn(DelicateCryptographyApi::class)
    fun buildCertificateReference(certificateBytes: ByteArray): ByteArray {
        val hasher = CryptographyProvider.Default
            .get(SHA1)
            .hasher()

        val certSha1 = runBlocking {
            hasher.hash(certificateBytes)
        }
        return certSha1.sliceArray(15..19)
    }

    fun encodeDerTlv(vdsType: String, derTlv: DerTlv): Message? {
        val bytes = derTlv.value
        val name = vdsSealCodingRegistry.getMessageName(vdsType, derTlv)
        val tag = derTlv.tag.toInt()
        val coding = vdsSealCodingRegistry.getMessageCoding(vdsType, derTlv)
        if (name == "" || coding == MessageCoding.UNKNOWN) return null
        return Message(tag, name, coding, MessageValue.fromBytes(bytes, coding))
    }

    fun getVdsType(documentRef: Int): String? {
        return vdsSealCodingRegistry.getVdsType(documentRef)
    }

    fun getDocumentRef(vdsType: String): Int? {
        return vdsSealCodingRegistry.getDocumentRef(vdsType)
    }

    fun <T> encodeMessage(vdsType: String, messageName: String, value: T): DerTlv {
        return vdsSealCodingRegistry.encodeMessage(vdsType, messageName, value)
    }

    fun getMessageTag(vdsType: String, messageName: String): Int {
        return vdsSealCodingRegistry.getMessageTag(vdsType, messageName)
    }

    fun getMessageCoding(vdsType: String, tag: Int): MessageCoding {
        return vdsSealCodingRegistry.getMessageCoding(vdsType, tag)
    }

    /**
     * Encodes a value to ByteArray based on the given MessageCoding.
     *
     * @param coding The MessageCoding that determines how to encode the value
     * @param value The value to encode (String, ByteArray, Int, or Byte)
     * @param tag Optional tag number for error messages (used when coding is UNKNOWN)
     * @return The encoded ByteArray
     * @throws IllegalArgumentException if the coding is UNKNOWN and tag is provided,
     *         or if the value type doesn't match the expected type for the coding
     */
    fun <T> encodeValueByCoding(coding: MessageCoding, value: T, tag: Int? = null): ByteArray {
        return when (coding) {
            MessageCoding.C40, MessageCoding.MRZ -> encodeC40(value as String)
            MessageCoding.UTF8_STRING -> (value as String).encodeToByteArray()
            MessageCoding.BYTES -> value as ByteArray
            MessageCoding.BYTE -> when (value) {
                is Int -> byteArrayOf((value and 0xFF).toByte())
                is Byte -> byteArrayOf(value)
                else -> throw IllegalArgumentException("BYTE coding expects Int or Byte, got ${value!!::class.simpleName}")
            }

            MessageCoding.MASKED_DATE -> encodeMaskedDate(value as String)
            MessageCoding.DATE -> encodeDate(value as String)
            MessageCoding.UNKNOWN -> if (tag != null) {
                throw IllegalArgumentException("Unsupported tag: $tag")
            } else {
                value as ByteArray
            }
        }
    }

    fun getIdbMessageTypeName(tag: Int): String {
        return idbMessageTypeRegistry.getMessageType(tag)
    }

    fun getIdbMessageTypeTag(messageTypeName: String): Int? {
        return idbMessageTypeRegistry.getMessageType(messageTypeName)
    }

    fun getIdbMessageTypeCoding(messageTypeName: String): MessageCoding {
        return idbMessageTypeRegistry.getMessageTypeCoding(messageTypeName)
    }

    fun getIdbMessageTypeCoding(messageTypeTag: Int): MessageCoding {
        return idbMessageTypeRegistry.getMessageTypeCoding(messageTypeTag)
    }

    fun getIdbDocumentTypeName(tag: Int): String {
        return idbDocumentTypeRegistry.getDocumentType(tag)
    }

    /**
     * Checks if the given vdsType requires UUID-based profile lookup.
     *
     * @param vdsType The VDS type to check
     * @return true if this type requires UUID lookup, false otherwise
     */
    fun requiresUuidLookup(vdsType: String): Boolean {
        return vdsSealCodingRegistry.requiresUuidLookup(vdsType)
    }

    /**
     * Gets the tag number containing the UUID for profile lookup.
     *
     * @param vdsType The VDS type to check
     * @return The tag number (default 0 if not specified or type not found)
     */
    fun getUuidMessageTag(vdsType: String): Int {
        return vdsSealCodingRegistry.getUuidMessageTag(vdsType)
    }

    /**
     * Resolves an extended message definition based on UUID bytes.
     *
     * @param uuidBytes 16-byte UUID
     * @return The matching ExtendedMessageDefinitionDto, or null if no definition matches
     */
    fun resolveExtendedMessageDefinition(uuidBytes: ByteArray): ExtendedMessageDefinitionDto? {
        return extendedMessageDefinitionRegistry.resolve(uuidBytes)
    }

    /**
     * Resolves an extended message definition based on UUID hex string.
     *
     * @param uuidHex UUID as hex string (32 characters, without dashes)
     * @return The matching ExtendedMessageDefinitionDto, or null if no definition matches
     */
    fun resolveExtendedMessageDefinition(uuidHex: String): ExtendedMessageDefinitionDto? {
        return extendedMessageDefinitionRegistry.resolve(uuidHex)
    }

    /**
     * Encodes a DerTlv to a Message with extended message definition-aware lookup.
     *
     * @param vdsType The base VDS type
     * @param extendedDefinition The resolved extended message definition (may be null)
     * @param derTlv The DerTlv to encode
     * @return The Message, or null if encoding fails
     */
    fun encodeDerTlv(vdsType: String, extendedDefinition: ExtendedMessageDefinitionDto?, derTlv: DerTlv): Message? {
        val bytes = derTlv.value
        val tag = derTlv.tag.toInt()
        val name = vdsSealCodingRegistry.getMessageName(vdsType, extendedDefinition, tag)
        val coding = vdsSealCodingRegistry.getMessageCoding(vdsType, extendedDefinition, tag)
        if (name == "" || coding == MessageCoding.UNKNOWN) return null
        return Message(tag, name, coding, MessageValue.fromBytes(bytes, coding))
    }

    /**
     * Decodes a byte[] encoded masked date as described in ICAO TR "Datastructure
     * for Barcode". Returns a date string in format yyyy-MM-dd where unknown parts
     * of the date are marked with an 'x'. e.g. 19xx-10-xx
     *
     * @param maskedDateBytes byte array that contains an encoded masked date
     * @return date string where unknown parts of the date are marked with an 'x'
     */

    @Throws(IllegalArgumentException::class)
    fun decodeMaskedDate(maskedDateBytes: ByteArray): String {
        require(maskedDateBytes.size == 4) { "expected four bytes for masked date decoding" }
        val mask = maskedDateBytes[0]
        val intval = (toUnsignedInt(maskedDateBytes[1]).toLong() * 256 * 256 + toUnsignedInt(
            maskedDateBytes[2]
        ) * 256L + toUnsignedInt(maskedDateBytes[3]))
        val day = ((intval % 1000000) / 10000).toInt()
        val month = (intval / 1000000).toInt()
        val year = (intval % 10000).toInt()
        // Pattern: MMddyyyy
        val dateCharArray = (month.toString().padStart(2, '0') +
                day.toString().padStart(2, '0') +
                year.toString().padStart(4, '0'))
            .toCharArray()

        for (i in 0..7) {
            val unknownBit = ((mask.toInt() shr (7 - i)) and 1).toByte()
            if (unknownBit.toInt() == 1) {
                dateCharArray[i] = 'x'
            }
        }
        val dateString = dateCharArray.concatToString()
        return dateString.replace("(.{2})(.{2})(.{4})".toRegex(), "$3-$1-$2").lowercase()
    }

    fun decodeDate(dateBytes: ByteArray): LocalDate {
        require(dateBytes.size == 3) { "expected three bytes for date decoding" }

        val intval =
            (toUnsignedInt(dateBytes[0]).toLong() * 256 * 256 + toUnsignedInt(dateBytes[1]) * 256L + toUnsignedInt(
                dateBytes[2]
            ))
        val day = ((intval % 1000000) / 10000).toInt()
        val month = (intval / 1000000).toInt()
        val year = (intval % 10000).toInt()

        return LocalDate(year, month, day)
    }

    /**
     * Decodes a byte[] encoded datetime as described in ICAO TR "Datastructure for
     * Barcode". Returns a LocalDateTime object
     *
     * @param dateTimeBytes byte array with length 6 which contains encoded datetime
     * @return LocalDateTime object
     */
    fun decodeDateTime(dateTimeBytes: ByteArray): LocalDateTime {
        require(dateTimeBytes.size == 6) { "Expected six bytes for date decoding" }

        var dateTimeLong = 0L
        for (byte in dateTimeBytes) {
            dateTimeLong = (dateTimeLong shl 8) or (byte.toLong() and 0xFF)
        }

        val paddedDateString = dateTimeLong.toString().padStart(14, '0')

        val month = paddedDateString.substring(0, 2).toInt()
        val day = paddedDateString.substring(2, 4).toInt()
        val year = paddedDateString.substring(4, 8).toInt()
        val hour = paddedDateString.substring(8, 10).toInt()
        val minute = paddedDateString.substring(10, 12).toInt()
        val second = paddedDateString.substring(12, 14).toInt()

        return LocalDateTime(year, month, day, hour, minute, second)
    }

    @Throws(IllegalArgumentException::class)
    fun parseDerTLvs(rawBytes: ByteArray): List<DerTlv> {
        val dataBuffer = Buffer().write(rawBytes)
        val derTlvList: MutableList<DerTlv> = ArrayList()
        while (!dataBuffer.exhausted()) {
            val tag = dataBuffer.readByte()

            var le = dataBuffer.readByte().toInt() and 0xff
            if (le == 0x81) {
                le = dataBuffer.readByte().toInt() and 0xff
            } else if (le == 0x82) {
                le = ((dataBuffer.readByte().toInt() and 0xff) * 0x100) + (dataBuffer.readByte().toInt() and 0xff)
            } else if (le == 0x83) {
                le = ((dataBuffer.readByte().toInt() and 0xff) * 0x1000) + ((dataBuffer.readByte()
                    .toInt() and 0xff) * 0x100) + (dataBuffer.readByte().toInt() and 0xff)
            } else if (le > 0x7F) {
                log.e(
                    "Can't decode length: ${le.toString(16).padStart(2, '0').uppercase()}"
                )
                throw IllegalArgumentException(
                    "Can't decode length: ${le.toString(16).padStart(2, '0').uppercase()}"
                )
            }
            val value = dataBuffer.readByteArray(le.toLong())
            derTlvList.add(DerTlv(tag, value))
        }
        return derTlvList
    }

    private fun toUnsignedInt(value: Byte): Int {
        return (value.toInt() and 0x7F) + (if (value < 0) 128 else 0)
    }

    fun decodeC40(bytes: ByteArray): String {
        val sb = StringBuilder()

        for (idx in bytes.indices) {
            if (idx % 2 == 0) {
                val i1 = bytes[idx]
                val i2 = bytes[idx + 1]

                if (i1 == 0xFE.toByte()) {
                    sb.append((i2 - 1).toChar())
                } else {
                    var v16 = (toUnsignedInt(i1) shl 8) + toUnsignedInt(i2) - 1
                    var temp = v16 / 1600
                    val u1 = temp
                    v16 -= temp * 1600
                    temp = v16 / 40
                    val u2 = temp
                    val u3 = v16 - temp * 40

                    if (u1 != 0) {
                        sb.append(toChar(u1))
                    }
                    if (u2 != 0) {
                        sb.append(toChar(u2))
                    }
                    if (u3 != 0) {
                        sb.append(toChar(u3))
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun toChar(intValue: Int): Char {
        return when (intValue) {
            3 -> {
                32.toChar()
            }

            in 4..13 -> {
                (intValue + 44).toChar()
            }

            in 14..39 -> {
                (intValue + 51).toChar()
            }

            // if character is unknown return "?"
            else -> 63.toChar()
        }
    }

    fun decodeBase256(s: String): ByteArray {
        val ca = s.toCharArray()
        val ba = ByteArray(ca.size)
        for (i in ba.indices) {
            ba[i] = ca[i].code.toByte()
        }
        return ba
    }

    fun unzip(bytesToDecompress: ByteArray): ByteArray {
        val inputBuffer = Buffer().write(bytesToDecompress)
        val inflaterSource = InflaterSource(inputBuffer, Inflater())
        val decompressedBytes = Buffer().apply { writeAll(inflaterSource) }.readByteArray()
        return decompressedBytes
    }
}
