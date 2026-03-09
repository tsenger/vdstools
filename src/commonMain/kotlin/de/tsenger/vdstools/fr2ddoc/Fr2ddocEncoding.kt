package de.tsenger.vdstools.fr2ddoc

import de.tsenger.vdstools.Base32
import de.tsenger.vdstools.generic.Message
import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.generic.MessageValue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

enum class Fr2ddocEncoding {
    UTF8_STRING,
    DATE_HEX_DAYS,
    DATE_DDMMYYYY,
    DATE_TIME_DDMMYYYYHHMM,
    TIME_HHMMSS,
    TIME_HHMM,
    NUMERIC,
    NUMERIC_PADDED,
    DECIMAL,
    HEX_INT,
    BOOLEAN,
    BASE32_STRING,
    BASE36;

    fun decode(rawValue: String, tag: Int, name: String): Message {
        val rawBytes = rawValue.encodeToByteArray()
        val value: MessageValue
        val coding: MessageCoding

        when (this) {
            UTF8_STRING, NUMERIC, BOOLEAN, BASE36, TIME_HHMMSS, TIME_HHMM -> {
                coding = MessageCoding.UTF8_STRING
                value = MessageValue.StringValue(rawValue, rawBytes)
            }

            NUMERIC_PADDED, DECIMAL -> {
                coding = MessageCoding.UTF8_STRING
                val stripped = rawValue.trimStart('0').ifEmpty { "0" }
                value = MessageValue.StringValue(stripped, rawBytes)
            }

            DATE_HEX_DAYS -> {
                coding = MessageCoding.DATE
                val date = Fr2ddocHeader.getDateFromDaysSince2000(rawValue.toLong(16))
                value = if (date != null) {
                    MessageValue.DateValue(date, rawBytes)
                } else {
                    MessageValue.StringValue(rawValue, rawBytes)
                }
            }

            DATE_DDMMYYYY -> {
                coding = MessageCoding.DATE
                val day = rawValue.substring(0, 2).toInt()
                val month = rawValue.substring(2, 4).toInt()
                val year = rawValue.substring(4, 8).toInt()
                value = MessageValue.DateValue(LocalDate(year, month, day), rawBytes)
            }

            DATE_TIME_DDMMYYYYHHMM -> {
                coding = MessageCoding.DATE_TIME
                val day = rawValue.substring(0, 2).toInt()
                val month = rawValue.substring(2, 4).toInt()
                val year = rawValue.substring(4, 8).toInt()
                val hour = rawValue.substring(8, 10).toInt()
                val minute = rawValue.substring(10, 12).toInt()
                value = MessageValue.DateTimeValue(
                    LocalDateTime(year, month, day, hour, minute), rawBytes
                )
            }

            HEX_INT -> {
                coding = MessageCoding.BYTE
                val intValue = rawValue.toInt(16)
                value = MessageValue.ByteValue(intValue, rawBytes)
            }

            BASE32_STRING -> {
                coding = MessageCoding.UTF8_STRING
                val decoded = Base32.decode(rawValue).decodeToString()
                value = MessageValue.StringValue(decoded, rawBytes)
            }
        }

        return Message(tag, name, coding, value)
    }
}
