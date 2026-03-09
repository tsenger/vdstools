package de.tsenger.vdstools.fr2ddoc

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.generic.MessageValue

class Fr2ddocMessageGroup private constructor(
    val messages: List<Message>
) {
    companion object {
        private val log = Logger.withTag(this::class.simpleName ?: "")
        private const val GS = '\u001D'
        private const val RS = '\u001E'

        fun parse(dataString: String, perimeterId: String): Fr2ddocMessageGroup {
            val messages = mutableListOf<Message>()
            var pos = 0

            while (pos < dataString.length) {
                if (pos + 2 > dataString.length) break

                val fieldId = dataString.substring(pos, pos + 2)
                pos += 2

                val definition = Fr2ddocFieldRegistry.getDefinition(perimeterId, fieldId)
                if (definition == null) {
                    log.w { "Unknown field ID '$fieldId' for perimeter $perimeterId at position $pos" }
                    break
                }

                val rawValue: String
                val truncated: Boolean

                if (definition.isFixed) {
                    val end = pos + definition.maxLength
                    if (end > dataString.length) {
                        log.w { "Not enough data for fixed field '$fieldId': need ${definition.maxLength}, have ${dataString.length - pos}" }
                        break
                    }
                    rawValue = dataString.substring(pos, end)
                    truncated = false
                    pos = end
                    // Consume optional trailing GS
                    if (pos < dataString.length && dataString[pos] == GS) {
                        pos++
                    }
                } else {
                    // Variable-length: scan for RS, GS within maxLength range, or end of data
                    val endLimit = if (definition.maxLength > 0) {
                        minOf(pos + definition.maxLength, dataString.length)
                    } else {
                        dataString.length
                    }

                    val rsIdx = dataString.indexOf(RS, pos).let { if (it in pos until endLimit) it else -1 }
                    val gsIdx = dataString.indexOf(GS, pos).let { if (it in pos until endLimit) it else -1 }

                    val delimIdx = when {
                        rsIdx >= 0 && gsIdx >= 0 -> minOf(rsIdx, gsIdx)
                        rsIdx >= 0 -> rsIdx
                        gsIdx >= 0 -> gsIdx
                        else -> -1
                    }

                    if (delimIdx >= 0) {
                        rawValue = dataString.substring(pos, delimIdx)
                        truncated = dataString[delimIdx] == RS
                        pos = delimIdx + 1
                    } else {
                        // No delimiter within range: read up to endLimit
                        rawValue = dataString.substring(pos, endLimit)
                        truncated = false
                        pos = endLimit
                        // Consume optional trailing GS/RS after maxLength
                        if (pos < dataString.length && (dataString[pos] == GS || dataString[pos] == RS)) {
                            pos++
                        }
                    }
                }

                val tag = try {
                    fieldId.toInt(16)
                } catch (_: NumberFormatException) {
                    // Non-hex field IDs (e.g. "BK") get unique tags above 0xFF
                    fieldId[0].code shl 8 or fieldId[1].code
                }
                val encoding = try {
                    Fr2ddocEncoding.valueOf(definition.encoding)
                } catch (_: IllegalArgumentException) {
                    log.w { "Unknown encoding '${definition.encoding}' for field '$fieldId'" }
                    null
                }

                val message = if (encoding != null && rawValue.isNotEmpty()) {
                    try {
                        encoding.decode(rawValue, tag, definition.name)
                    } catch (e: Exception) {
                        log.w { "Failed to decode field '$fieldId': ${e.message}" }
                        Message(
                            tag, definition.name, MessageCoding.UTF8_STRING,
                            MessageValue.StringValue(rawValue, rawValue.encodeToByteArray())
                        )
                    }
                } else {
                    Message(
                        tag, definition.name, MessageCoding.UTF8_STRING,
                        MessageValue.StringValue(rawValue, rawValue.encodeToByteArray())
                    )
                }

                log.v { "Field $fieldId (${definition.name}): '$message'${if (truncated) " [truncated]" else ""}" }
                messages.add(message)
            }

            return Fr2ddocMessageGroup(messages)
        }
    }
}
