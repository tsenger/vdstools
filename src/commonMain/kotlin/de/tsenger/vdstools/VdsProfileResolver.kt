package de.tsenger.vdstools

import de.tsenger.vdstools.vds.dto.VdsProfileDefinitionDto

/**
 * Supplies the profile definition used to decode a UUID-based seal (e.g. BSI TR-03171).
 *
 * Called while parsing with the document profile UUID (Tag 0x00), the seal's `PROFILE_URI`
 * (Tag 0x03, null if absent — always null for v0.8 seals) and the seal's base document type from the
 * header ([DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8] for 0xC8, [DataEncoder.ADMINISTRATIVE_DOCUMENTS_V9]
 * for 0xC9). Profile UUIDs are only unique together with the source they were published under and the
 * TR-03171 version, so an application that loads profiles at runtime can use URI and base type to pick
 * the matching definition. The default resolver ignores the URI and looks (UUID, base type) up in
 * [DataEncoder.vdsProfileDefinitions].
 *
 * Used whenever a seal's profile is not given: when parsing, and when a base document type is built
 * with the profile number set by hand. Building for a profile (`VdsSeal.Builder` by name,
 * `forProfileUuid`, `forProfile`) takes the profile from [DataEncoder.vdsProfileDefinitions] or from
 * the caller and does not call it; at that point the seal has no `PROFILE_URI` yet to decide by.
 */
fun interface VdsProfileResolver {
    fun resolve(uuid: ByteArray, profileUri: String?, baseDocumentType: String): VdsProfileDefinitionDto?
}
