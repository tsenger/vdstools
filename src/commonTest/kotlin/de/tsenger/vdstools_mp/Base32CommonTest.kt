package de.tsenger.vdstools_mp


import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class Base32CommonTest {

    @Test
    fun testEncode_foo() {
        assertEquals("MZXW6===", Base32.encode("foo".encodeToByteArray()))
    }

    @Test
    fun testEncode_foob() {
        assertEquals("MZXW6YQ=", Base32.encode("foob".encodeToByteArray()))
    }

    @Test
    fun testEncode_fooba() {
        assertEquals("MZXW6YTB", Base32.encode("fooba".encodeToByteArray()))
    }

    @Test
    fun testEncode_foobar() {
        assertEquals("MZXW6YTBOI======", Base32.encode("foobar".encodeToByteArray()))
    }

    @Test
    fun testDecode_foo() {
        assertContentEquals("foo".encodeToByteArray(), Base32.decode("MZXW6==="))
    }

    @Test
    fun testDecode_foob() {
        assertContentEquals("foob".encodeToByteArray(), Base32.decode("MZXW6YQ="))
    }

    @Test
    fun testDecode_fooba() {
        assertContentEquals("fooba".encodeToByteArray(), Base32.decode("MZXW6YTB"))
    }

    @Test
    fun testDecode_foobar() {
        assertContentEquals("foobar".encodeToByteArray(), Base32.decode("MZXW6YTBOI======"))
    }
}