package de.tsenger.vdstools

import kotlin.test.*

@OptIn(ExperimentalStdlibApi::class)
class ExtendedMessageDefinitionRegistryCommonTest {

    private val testDefinitionsJson = """
        [
          {
            "definitionId": "9a4223406d374ef99e2cf95e31a23846",
            "definitionName": "MELDEBESCHEINIGUNG",
            "baseDocumentType": "ADMINISTRATIVE_DOCUMENTS",
            "version": 1,
            "messages": [
              {"name": "SURNAME", "tag": 4, "coding": "UTF8_STRING", "required": true, "minLength": 1, "maxLength": 255}
            ]
          },
          {
            "definitionId": "550e8400e29b41d4a716446655440002",
            "definitionName": "TEST_DEFINITION",
            "baseDocumentType": "ADMINISTRATIVE_DOCUMENTS",
            "version": 1,
            "messages": []
          }
        ]
    """.trimIndent()

    @Test
    fun testResolveByBytes() {
        val registry = ExtendedMessageDefinitionRegistry(testDefinitionsJson)
        val uuidBytes = "9a4223406d374ef99e2cf95e31a23846".hexToByteArray()

        val definition = registry.resolve(uuidBytes)

        assertNotNull(definition)
        assertEquals("MELDEBESCHEINIGUNG", definition.definitionName)
        assertEquals("ADMINISTRATIVE_DOCUMENTS", definition.baseDocumentType)
        assertEquals(1, definition.version)
    }

    @Test
    fun testResolveByHexString() {
        val registry = ExtendedMessageDefinitionRegistry(testDefinitionsJson)

        val definition = registry.resolve("9a4223406d374ef99e2cf95e31a23846")

        assertNotNull(definition)
        assertEquals("MELDEBESCHEINIGUNG", definition.definitionName)
    }

    @Test
    fun testResolveByHexStringUppercase() {
        val registry = ExtendedMessageDefinitionRegistry(testDefinitionsJson)

        val definition = registry.resolve("9A4223406D374EF99E2CF95E31A23846")

        assertNotNull(definition)
        assertEquals("MELDEBESCHEINIGUNG", definition.definitionName)
    }

    @Test
    fun testResolveByHexStringWithDashes() {
        val registry = ExtendedMessageDefinitionRegistry(testDefinitionsJson)

        val definition = registry.resolve("9a422340-6d37-4ef9-9e2c-f95e31a23846")

        assertNotNull(definition)
        assertEquals("MELDEBESCHEINIGUNG", definition.definitionName)
    }

    @Test
    fun testResolveUnknownDefinition() {
        val registry = ExtendedMessageDefinitionRegistry(testDefinitionsJson)
        val unknownUuid = "00000000000000000000000000000000".hexToByteArray()

        val definition = registry.resolve(unknownUuid)

        assertNull(definition)
    }

    @Test
    fun testResolveInvalidUuidLength() {
        val registry = ExtendedMessageDefinitionRegistry(testDefinitionsJson)
        val shortUuid = "9a422340".hexToByteArray()

        val definition = registry.resolve(shortUuid)

        assertNull(definition)
    }

    @Test
    fun testAvailableDefinitions() {
        val registry = ExtendedMessageDefinitionRegistry(testDefinitionsJson)

        val definitions = registry.availableDefinitions

        assertEquals(2, definitions.size)
        assertTrue(definitions.contains("MELDEBESCHEINIGUNG"))
        assertTrue(definitions.contains("TEST_DEFINITION"))
    }

    @Test
    fun testAvailableDefinitionUuids() {
        val registry = ExtendedMessageDefinitionRegistry(testDefinitionsJson)

        val uuids = registry.availableDefinitionUuids

        assertEquals(2, uuids.size)
        assertTrue(uuids.contains("9a4223406d374ef99e2cf95e31a23846"))
        assertTrue(uuids.contains("550e8400e29b41d4a716446655440002"))
    }

    @Test
    fun testResolveSecondDefinition() {
        val registry = ExtendedMessageDefinitionRegistry(testDefinitionsJson)
        val uuidBytes = "550e8400e29b41d4a716446655440002".hexToByteArray()

        val definition = registry.resolve(uuidBytes)

        assertNotNull(definition)
        assertEquals("TEST_DEFINITION", definition.definitionName)
    }

    @Test
    fun testEmptyDefinitionsJson() {
        val registry = ExtendedMessageDefinitionRegistry("[]")

        assertTrue(registry.availableDefinitions.isEmpty())
        assertTrue(registry.availableDefinitionUuids.isEmpty())
    }

    @Test
    fun testDefinitionMessages() {
        val registry = ExtendedMessageDefinitionRegistry(testDefinitionsJson)
        val uuidBytes = "9a4223406d374ef99e2cf95e31a23846".hexToByteArray()

        val definition = registry.resolve(uuidBytes)

        assertNotNull(definition)
        assertEquals(1, definition.messages.size)
        assertEquals("SURNAME", definition.messages[0].name)
        assertEquals(4, definition.messages[0].tag)
    }
}
