package de.tsenger.vdstools.asn1

import vdstools.asn1.ASN1Encoder
import kotlin.test.Test
import kotlin.test.assertContentEquals


class ASN1EncoderTest {

    @Test
    fun testGetDerLength_5() {
        assertContentEquals(byteArrayOf(5), ASN1Encoder.getDerLength(5))
    }

    @Test
    fun testGetDerLength_0x7f() {
        assertContentEquals(byteArrayOf(0x7f), ASN1Encoder.getDerLength(0x7f))
    }

    @Test
    fun testGetDerLength_0x80() {
        assertContentEquals(byteArrayOf(0x81.toByte(), 0x80.toByte()), ASN1Encoder.getDerLength(0x80))
    }

    @Test
    fun testGetDerLength_0x9812() {
        assertContentEquals(byteArrayOf(0x82.toByte(), 0x98.toByte(), 0x12), ASN1Encoder.getDerLength(0x9812))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testGetDerInteger_0x0123() {
        assertContentEquals(byteArrayOf(0x02, 0x02, 0x01, 0x23), ASN1Encoder.getDerInteger(0x0123))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testGetDerInteger_64Bytes() {
        val randomBytes =
            "6f98485505bf103c705f5d5e089d4091b144876e8335942cf411e7c02848d26175fc9c825ac265bfc4d2640b8b26795cf9a856c90507a01c62450c5954f2e0d4".hexToByteArray()
        assertContentEquals(byteArrayOf(0x02, 0x40) + randomBytes, ASN1Encoder.getDerInteger(randomBytes))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testGetDerInteger_64Byte_leadingZero() {
        val randomBytes =
            "ff98485505bf103c705f5d5e089d4091b144876e8335942cf411e7c02848d26175fc9c825ac265bfc4d2640b8b26795cf9a856c90507a01c62450c5954f2e0d4".hexToByteArray()
        assertContentEquals(byteArrayOf(0x02, 0x41, 0x00) + randomBytes, ASN1Encoder.getDerInteger(randomBytes))
    }

}