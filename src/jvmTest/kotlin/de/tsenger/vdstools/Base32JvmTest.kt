package de.tsenger.vdstools

import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class Base32JvmTest {

    @Test
    fun testEncode_foo() {
        assertEquals("MZXW6===", Base32.encode("foo".toByteArray()))
    }

    @Test
    fun testEncode_foob() {
        assertEquals("MZXW6YQ=", Base32.encode("foob".toByteArray()))
    }

    @Test
    fun testEncode_fooba() {
        assertEquals("MZXW6YTB", Base32.encode("fooba".toByteArray()))
    }

    @Test
    fun testEncode_foobar() {
        assertEquals("MZXW6YTBOI======", Base32.encode("foobar".toByteArray()))
    }

    @Test
    fun testDecode_foo() {
        assertContentEquals("foo".toByteArray(), Base32.decode("MZXW6==="))
    }

    @Test
    fun testDecode_foob() {
        assertContentEquals("foob".toByteArray(), Base32.decode("MZXW6YQ="))
    }

    @Test
    fun testDecode_fooba() {
        assertContentEquals("fooba".toByteArray(), Base32.decode("MZXW6YTB"))
    }

    @Test
    fun testDecode_foobar() {
        assertContentEquals("foobar".toByteArray(), Base32.decode("MZXW6YTBOI======"))
    }
}