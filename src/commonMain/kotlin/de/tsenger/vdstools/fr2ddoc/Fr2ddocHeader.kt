package de.tsenger.vdstools.fr2ddoc

import co.touchlab.kermit.Logger
import de.tsenger.vdstools.fr2ddoc.Fr2ddocSeal.BufferReader
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class Fr2ddocHeader {

    var issuingCountry: String? = null
        private set
    var signerIdentifier: String? = null
        private set
    var certificateReference: String? = null
        private set
    var issuingDate: LocalDate? = null
        private set
    var sigDate: LocalDate? = null
        private set
    var docType: String? = null
        private set
    var perimeterId: String? = null
        private set
    var version: Byte = 0
        private set

    private constructor()

    companion object {
        private val log = Logger.withTag(this::class.simpleName ?: "")

        fun fromStringBuffer(buffer: BufferReader): Fr2ddocHeader {

            val header = Fr2ddocHeader()

            val markerDC = buffer.next(2)
            header.version = buffer.next(2).toByte()
            header.signerIdentifier = buffer.next(4)
            header.certificateReference = buffer.next(4)
            header.issuingDate = getDateFromDaysSince2000(buffer.next(4).toLong(16))
            header.sigDate = getDateFromDaysSince2000(buffer.next(4).toLong(16))
            header.docType = buffer.next(2)

            if (header.version > 2) {
                header.perimeterId = buffer.next(2)
                if (header.version > 3) {
                    header.issuingCountry = buffer.next(2)
                }
            }
            log.v { "buffer pointer after header parsing: ${buffer.pointer}" }
            return header
        }

        fun getDateFromDaysSince2000(days: Long): LocalDate? {
            if (days == 0xffffL) return null
            val epoch2000 = LocalDate(2000, 1, 1)
            return epoch2000.plus(days, DateTimeUnit.DAY)
        }
    }
}