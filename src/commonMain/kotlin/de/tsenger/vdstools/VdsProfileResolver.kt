package de.tsenger.vdstools

import de.tsenger.vdstools.vds.dto.VdsProfileDefinitionDto

/**
 * Supplies the profile definition used to decode a UUID-based seal (e.g. BSI TR-03171).
 *
 * Called while parsing with the document profile UUID (Tag 0x00) and the seal's `PROFILE_URI`
 * (Tag 0x03, null if absent). Profile UUIDs are only unique together with the source they were
 * published under, so an application that loads profiles at runtime can use the URI to pick the
 * matching definition. The default resolver ignores the URI and looks the UUID up in
 * [DataEncoder.vdsProfileDefinitions].
 *
 * Only used when parsing. Building seals (e.g. `VdsSeal.Builder.forProfileUuid`)
 * still looks profiles up in [DataEncoder.vdsProfileDefinitions] directly.
 */
fun interface VdsProfileResolver {
    fun resolve(uuid: ByteArray, profileUri: String?): VdsProfileDefinitionDto?
}
