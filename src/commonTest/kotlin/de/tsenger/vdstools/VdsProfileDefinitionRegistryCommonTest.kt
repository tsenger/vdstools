package de.tsenger.vdstools

import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.vds.dto.MessageDto
import de.tsenger.vdstools.vds.dto.VdsProfileDefinitionDto
import kotlin.test.*

@OptIn(ExperimentalStdlibApi::class)
class VdsProfileDefinitionRegistryCommonTest {

    private val V8 = DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8
    private val V9 = DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9

    private val meldebescheinigung = VdsProfileDefinitionDto(
        definitionId = "9a4223406d374ef99e2cf95e31a23846",
        definitionName = "MELDEBESCHEINIGUNG",
        baseDocumentType = "ADMINISTRATIVE_DOCUMENTS_V8",
        version = 1,
        messages = listOf(MessageDto("SURNAME", 4, MessageCoding.UTF8_STRING, true, 1, 255))
    )

    private val testDefinition = VdsProfileDefinitionDto(
        definitionId = "550e8400e29b41d4a716446655440002",
        definitionName = "TEST_DEFINITION",
        baseDocumentType = "ADMINISTRATIVE_DOCUMENTS_V8",
        version = 1
    )

    private fun createRegistry() = VdsProfileDefinitionRegistry().apply {
        addDefinition(meldebescheinigung)
        addDefinition(testDefinition)
    }

    @Test
    fun testResolveByBytes() {
        val registry = createRegistry()
        val uuidBytes = "9a4223406d374ef99e2cf95e31a23846".hexToByteArray()

        val definition = registry.resolve(uuidBytes, V8)

        assertNotNull(definition)
        assertEquals("MELDEBESCHEINIGUNG", definition.definitionName)
        assertEquals("ADMINISTRATIVE_DOCUMENTS_V8", definition.baseDocumentType)
        assertEquals(1, definition.version)
    }

    @Test
    fun testResolveByHexString() {
        val registry = createRegistry()

        val definition = registry.resolve("9a4223406d374ef99e2cf95e31a23846", V8)

        assertNotNull(definition)
        assertEquals("MELDEBESCHEINIGUNG", definition.definitionName)
    }

    @Test
    fun testResolveByHexStringUppercase() {
        val registry = createRegistry()

        val definition = registry.resolve("9A4223406D374EF99E2CF95E31A23846", V8)

        assertNotNull(definition)
        assertEquals("MELDEBESCHEINIGUNG", definition.definitionName)
    }

    @Test
    fun testResolveByHexStringWithDashes() {
        val registry = createRegistry()

        val definition = registry.resolve("9a422340-6d37-4ef9-9e2c-f95e31a23846", V8)

        assertNotNull(definition)
        assertEquals("MELDEBESCHEINIGUNG", definition.definitionName)
    }

    @Test
    fun testResolveUnknownDefinition() {
        val registry = createRegistry()
        val unknownUuid = "00000000000000000000000000000000".hexToByteArray()

        val definition = registry.resolve(unknownUuid, V8)

        assertNull(definition)
    }

    @Test
    fun testResolveInvalidUuidLength() {
        val registry = createRegistry()
        val shortUuid = "9a422340".hexToByteArray()

        val definition = registry.resolve(shortUuid, V8)

        assertNull(definition)
    }

    @Test
    fun testDefinitionNames() {
        val registry = createRegistry()

        val definitions = registry.definitions.map { it.definitionName }

        assertEquals(2, definitions.size)
        assertTrue(definitions.contains("MELDEBESCHEINIGUNG"))
        assertTrue(definitions.contains("TEST_DEFINITION"))
    }

    @Test
    fun testDefinitionUuids() {
        val registry = createRegistry()

        val uuids = registry.definitions.map { it.definitionId }

        assertEquals(2, uuids.size)
        assertTrue(uuids.contains("9a4223406d374ef99e2cf95e31a23846"))
        assertTrue(uuids.contains("550e8400e29b41d4a716446655440002"))
    }

    @Test
    fun testResolveSecondDefinition() {
        val registry = createRegistry()
        val uuidBytes = "550e8400e29b41d4a716446655440002".hexToByteArray()

        val definition = registry.resolve(uuidBytes, V8)

        assertNotNull(definition)
        assertEquals("TEST_DEFINITION", definition.definitionName)
    }

    @Test
    fun testNewRegistryIsEmpty() {
        val registry = VdsProfileDefinitionRegistry()

        assertTrue(registry.definitions.isEmpty())
    }

    @Test
    fun testDefinitionMessages() {
        val registry = createRegistry()
        val uuidBytes = "9a4223406d374ef99e2cf95e31a23846".hexToByteArray()

        val definition = registry.resolve(uuidBytes, V8)

        assertNotNull(definition)
        assertEquals(1, definition.messages.size)
        assertEquals("SURNAME", definition.messages[0].name)
        assertEquals(4, definition.messages[0].tag)
    }

    @Test
    fun testAddDefinitionWithSameUuidReplacesOldName() {
        val registry = createRegistry()
        registry.addDefinition(meldebescheinigung.copy(definitionName = "Meldebestätigung"))

        assertEquals("Meldebestätigung", registry.resolve("9a4223406d374ef99e2cf95e31a23846", V8)?.definitionName)
        assertNotNull(registry.resolveByName("Meldebestätigung", V8))
        assertNull(registry.resolveByName("MELDEBESCHEINIGUNG", V8))
        assertEquals(2, registry.definitions.size)
    }

    @Test
    fun testSameUuidForV8AndV9IsKeptSeparately() {
        val registry = createRegistry()
        val v9 = meldebescheinigung.copy(baseDocumentType = V9, messages = emptyList())
        registry.addDefinition(v9)

        assertEquals(3, registry.definitions.size)
        assertEquals(1, registry.resolve("9a4223406d374ef99e2cf95e31a23846", V8)?.messages?.size)
        assertSame(v9, registry.resolve("9a4223406d374ef99e2cf95e31a23846", V9))
        assertEquals(1, registry.resolveByName("MELDEBESCHEINIGUNG", V8)?.messages?.size)
        assertSame(v9, registry.resolveByName("MELDEBESCHEINIGUNG", V9))
    }

    @Test
    fun testResolveUniqueFailsForUuidOrNameInSeveralBaseTypes() {
        val registry = createRegistry()
        assertSame(meldebescheinigung, registry.resolveUnique("9a4223406d374ef99e2cf95e31a23846"))
        assertSame(meldebescheinigung, registry.resolveByNameUnique("MELDEBESCHEINIGUNG"))

        registry.addDefinition(meldebescheinigung.copy(baseDocumentType = V9))

        assertFailsWith<IllegalArgumentException> { registry.resolveUnique("9a4223406d374ef99e2cf95e31a23846") }
        assertFailsWith<IllegalArgumentException> { registry.resolveByNameUnique("MELDEBESCHEINIGUNG") }
        assertSame(testDefinition, registry.resolveUnique("550e8400e29b41d4a716446655440002"))
        assertNull(registry.resolveUnique("00000000000000000000000000000000"))
    }
}
