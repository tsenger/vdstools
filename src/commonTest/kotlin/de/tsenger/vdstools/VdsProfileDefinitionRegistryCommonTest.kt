package de.tsenger.vdstools

import de.tsenger.vdstools.generic.MessageCoding
import de.tsenger.vdstools.vds.dto.MessageDto
import de.tsenger.vdstools.vds.dto.VdsProfileDefinitionDto
import kotlin.test.*

@OptIn(ExperimentalStdlibApi::class)
class VdsProfileDefinitionRegistryCommonTest {

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

        val definition = registry.resolve(uuidBytes)

        assertNotNull(definition)
        assertEquals("MELDEBESCHEINIGUNG", definition.definitionName)
        assertEquals("ADMINISTRATIVE_DOCUMENTS_V8", definition.baseDocumentType)
        assertEquals(1, definition.version)
    }

    @Test
    fun testResolveByHexString() {
        val registry = createRegistry()

        val definition = registry.resolve("9a4223406d374ef99e2cf95e31a23846")

        assertNotNull(definition)
        assertEquals("MELDEBESCHEINIGUNG", definition.definitionName)
    }

    @Test
    fun testResolveByHexStringUppercase() {
        val registry = createRegistry()

        val definition = registry.resolve("9A4223406D374EF99E2CF95E31A23846")

        assertNotNull(definition)
        assertEquals("MELDEBESCHEINIGUNG", definition.definitionName)
    }

    @Test
    fun testResolveByHexStringWithDashes() {
        val registry = createRegistry()

        val definition = registry.resolve("9a422340-6d37-4ef9-9e2c-f95e31a23846")

        assertNotNull(definition)
        assertEquals("MELDEBESCHEINIGUNG", definition.definitionName)
    }

    @Test
    fun testResolveUnknownDefinition() {
        val registry = createRegistry()
        val unknownUuid = "00000000000000000000000000000000".hexToByteArray()

        val definition = registry.resolve(unknownUuid)

        assertNull(definition)
    }

    @Test
    fun testResolveInvalidUuidLength() {
        val registry = createRegistry()
        val shortUuid = "9a422340".hexToByteArray()

        val definition = registry.resolve(shortUuid)

        assertNull(definition)
    }

    @Test
    fun testAvailableDefinitions() {
        val registry = createRegistry()

        val definitions = registry.availableDefinitions

        assertEquals(2, definitions.size)
        assertTrue(definitions.contains("MELDEBESCHEINIGUNG"))
        assertTrue(definitions.contains("TEST_DEFINITION"))
    }

    @Test
    fun testAvailableDefinitionUuids() {
        val registry = createRegistry()

        val uuids = registry.availableDefinitionUuids

        assertEquals(2, uuids.size)
        assertTrue(uuids.contains("9a4223406d374ef99e2cf95e31a23846"))
        assertTrue(uuids.contains("550e8400e29b41d4a716446655440002"))
    }

    @Test
    fun testResolveSecondDefinition() {
        val registry = createRegistry()
        val uuidBytes = "550e8400e29b41d4a716446655440002".hexToByteArray()

        val definition = registry.resolve(uuidBytes)

        assertNotNull(definition)
        assertEquals("TEST_DEFINITION", definition.definitionName)
    }

    @Test
    fun testNewRegistryIsEmpty() {
        val registry = VdsProfileDefinitionRegistry()

        assertTrue(registry.availableDefinitions.isEmpty())
        assertTrue(registry.availableDefinitionUuids.isEmpty())
    }

    @Test
    fun testDefinitionMessages() {
        val registry = createRegistry()
        val uuidBytes = "9a4223406d374ef99e2cf95e31a23846".hexToByteArray()

        val definition = registry.resolve(uuidBytes)

        assertNotNull(definition)
        assertEquals(1, definition.messages.size)
        assertEquals("SURNAME", definition.messages[0].name)
        assertEquals(4, definition.messages[0].tag)
    }

    @Test
    fun testAddDefinitionWithSameUuidReplacesOldName() {
        val registry = createRegistry()
        registry.addDefinition(meldebescheinigung.copy(definitionName = "Meldebestätigung"))

        assertEquals("Meldebestätigung", registry.resolve("9a4223406d374ef99e2cf95e31a23846")?.definitionName)
        assertNotNull(registry.resolveByName("Meldebestätigung"))
        assertNull(registry.resolveByName("MELDEBESCHEINIGUNG"))
        assertEquals(2, registry.availableDefinitions.size)
    }
}
