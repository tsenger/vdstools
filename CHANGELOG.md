# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- `VdsProfileResolver` and `DataEncoder.vdsProfileResolver`: a replaceable hook
  that supplies the profile definition while parsing UUID-based seals (BSI
  TR-03171). It receives the profile UUID (Tag 0x00) and the seal's
  `PROFILE_URI` (Tag 0x03), so applications loading profiles at runtime can keep
  profiles with the same UUID from different sources apart. The default
  (`DataEncoder.defaultVdsProfileResolver`) looks the UUID up in
  `vdsProfileDefinitions` as before; `resetToDefaults()` restores it.
  Parsing only: building seals still uses `vdsProfileDefinitions` directly.
  Additive, non-breaking.

## [0.19.0] - 2026-10-06

### Added
- `VdsSeal.Builder.forProfileUuid(profileUuid)` — a typed factory to build a
  BSI TR-03171 seal for a registered document profile by its **UUID** instead of
  its profile name. Because profiles are uniquely identified by their UUID,
  several profiles may now share the same `profileName` without ambiguity. The
  profile must be registered first (e.g. via
  `DataEncoder.loadVdsProfileDefinitionFromXml`); the factory throws
  `IllegalArgumentException` if no profile is registered for the UUID. The
  existing `VdsSeal.Builder(documentType)` constructor (base document type or
  predefined profile name) is unchanged. Additive, non-breaking.
- `MessageCoding.BOOLEAN` with `MessageValue.BooleanValue` for TR-03171 profile
  fields of ASN.1 type `BOOLEAN`. Encodes DER-strict (`true` → `0xFF`, `false` →
  `0x00`; accepts `Boolean` or `"true"`/`"false"`), decodes BER-tolerant (any
  non-zero octet is `true`). Profiles parsed from XML now map `BOOLEAN` to this
  coding instead of `BYTE`.
- `VdsProfileDefinitionDto.validFromPresent` / `validToPresent` (nullable,
  default `null`): carried over from the TR-03171 XML profile.
- `MessageCoding.DATE_TIME_STRING` for the 14-byte `YYYYMMDDHHMMSS` UTF-8
  encoding of ASN.1 DATE-TIME (X.690 8.26.4), analogous to `DATE_STRING` for
  DATE. Accepts `LocalDateTime` or an ISO string (`yyyy-MM-ddTHH:mm:ss`), decodes
  to `DateTimeValue`. TR-03171 XML profile entries of type `DATE-TIME` now map to
  this coding (`maxBytes` 14) instead of the 6-byte ICAO `DATE_TIME`, so all XML
  profile types are encoded per ASN.1/X.690.

### Changed
- Dependency updates: Kotlin 2.4.0 → 2.4.20, BouncyCastle 1.84 → 1.86,
  xmlutil 0.91.3 → 1.0.2.1, Okio 3.17.0 → 3.18.2, Dokka 2.0.0 → 2.2.0,
  ZXing (tests only) 3.5.3 → 3.5.4, Gradle wrapper 9.6.0 → 9.8.0.
- TR-03171 v0.9 (0xC9): `VdsSeal.Builder.build()` now enforces the
  encoder-side requirements of the TR and throws `IllegalArgumentException` if
  `PROFILE_URI` (0x03) or `CERTIFICATE_URI` (0x04) is missing, if `VALID_FROM` /
  `VALID_TO` is present or absent contrary to the profile's `validFromPresent` /
  `validToPresent`, or if a non-optional profile entry is missing. Other
  document types are unaffected. Decoding is unchanged (no checks).
- TR-03171 v0.9: `STATUS_LIST_INDEX` (tag 0x06) is now coded as ASN.1 `INTEGER`
  (previously `BYTES`) and decodes to `IntegerValue` (e.g. `424242` instead of
  `067932`). Building expects `Int`, `Long` or a decimal `String`.
- TR-03171 XML profiles: the `length` of `INTEGER` entries is ignored as
  specified in TR-03171 §4; `maxBytes` is now always 8 (the INTEGER coding limit).

### Fixed
- TR-03171 / DEZV seals: the certificate reference length in the ICAO version 4
  header is now encoded as a **decimal** value for the special signer identifier
  `DEZV` (e.g. a 40-char SHA-1 hex reference encodes to `DEZV40`), matching the
  BSI TR-03171 specification. Previously the length was always hex-encoded
  (producing `DEZV28` for a 40-char reference). Other signer identifiers keep the
  ICAO hexadecimal encoding.
- TR-03171 / DEZV seals: decoding the cert ref length now derives the radix
  **deterministically** from the signer identifier (decimal for `DEZV`, hex
  otherwise), mirroring the encoder. The previous lenient approach tried both
  radices and kept whichever produced parseable dates; because `decodeDate` only
  rejects obviously out-of-range values, coincidentally valid dates after a
  wrong-radix over-read could win and silently swallow cert-ref bytes. The radix
  is no longer guessed, removing that instability. As a consequence, a non-`DEZV`
  seal that encodes its cert ref length decimally (a spec violation) is now
  rejected as malformed instead of being heuristically recovered.
- Parsing a malformed ICAO v4 header now consistently throws
  `IllegalArgumentException`. Previously a wrong cert ref length could surface as
  an okio `EOFException` (over-read past the buffer end) or, when the misalignment
  reached the message body, an `IndexOutOfBoundsException` from `DerTlv.parseAll`.
  Both are now wrapped into `IllegalArgumentException` with diagnostic context
  (signer, length field, radix / truncated tag), matching the documented contract.
- `DerTlv` parsing is hardened against truncated / misaligned input: a tag with no
  following length byte, a multi-byte length prefix that runs past the end, and a
  length value that overflows the `Int` range now all throw a descriptive
  `IllegalArgumentException` instead of an opaque `IndexOutOfBoundsException`.

## [0.18.0] - 2026-06-23

### Added
- BSI TR-03171 v0.9 support (document category 0xC9) via new
  `ADMINISTRATIVE_DOCUMENTS_V9` base document type with reserved metadata tags
  0x00–0x06 (UUID, validity dates, profile/certificate/status URIs, status list index)
- `MessageCoding.DATE_STRING` for 8-byte `YYYYMMDD` UTF-8 date encoding used by
  TR-03171 v0.9 `validFrom` / `validTo` fields
- TR-03171 v0.9 XML profile fields: `versionTR`, `validFromPresent`,
  `validToPresent` (all mandatory) are now parsed into `ProfileDto`

### Changed
- **Breaking:** the legacy TR-03171 v0.8 base document type was renamed from
  `ADMINISTRATIVE_DOCUMENTS` to `ADMINISTRATIVE_DOCUMENTS_V8` (document category
  0xC8 / documentRef 0x01C8 unchanged). Update any custom profile JSON
  (`baseDocumentType`), `VdsHeader.Builder(...)` calls, and comparisons against
  `seal.baseDocumentType` accordingly. Use the constants
  `DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8` / `…_V9` instead of string literals.
- **Breaking:** the XML profile parser now targets the TR-03171 **v0.9** schema:
  `profileName` / `creator` are optional, `versionTR` / `validFromPresent` /
  `validToPresent` are mandatory, the `statusIndicator` element was removed, and
  profile entry tags are restricted to `0x0A`–`0xFE` (10–254, max 245 entries).
  Profiles parsed from XML are always v0.9; `DataEncoder.loadVdsProfileDefinitionFromXml()`
  and `ProfileConverter.toVdsProfileDefinition()` no longer take a `baseDocumentType`
  argument and always produce `ADMINISTRATIVE_DOCUMENTS_V9` definitions. Legacy 0xC8
  seals remain decodable via the bundled JSON profile definitions.

- Upgraded Gradle wrapper to 9.6.0
- Bumped Kotlin to 2.4.0 and updated dependencies (kotlinx-datetime 0.8.0,
  kotlinx-serialization 1.11.0, BouncyCastle 1.84, okio 3.17.0, maven-publish 0.37.0)

### Removed
- `StatusIndicator` enum and the `statusIndicator` profile field (not part of the
  TR-03171 v0.9 schema)

### Fixed
- `VdsSeal.dissect()` no longer truncates the message zone for unsigned seals

---

## [0.17.0] - 2026-05-12

### Added
- `MRZ_MRVA` and `MRZ_MRVB` codings for spec-compliant Visa MRZ truncation

### Fixed
- Missing `DOCUMENT_REFERENCE` message in `CERTIFYING_PERMANENT_RESIDENCE`
- Missing `DOCUMENT_REFERENCE` message in `FRONTIER_WORKER_PERMIT`
- Incorrect MRZ type for `PROVISIONAL_RESIDENCE_DOCUMENT` (now `MRZ_TD2`)

---

## [0.16.0] - 2026-04-14

### Added
- Pluggable `VdsLogger` interface replacing the Kermit dependency
- `messageList` is now populated with raw bytes for unknown document types

### Changed
- Removed `kotlinx-coroutines` dependency by switching to `hashBlocking` API
- Downgraded `xmlutil` to core module, removed redundant `stdlib-jdk8` dependency
- Removed unused country code mapping and dead build tasks

---

## [0.15.0] - 2026-04-05

### Added
- `SealParser` with configurable seal type filtering
- `String` input accepted for `DATE` and `DATE_TIME` codings in `encodeValueByCoding`
- IDB Visa test and raw string

### Changed
- Unified `VdsSeal.Builder` and `IdbSeal.Builder` into a single builder API
- Introduced unified `MessageDefinition`/`MessageResolver` layer, consolidating DER-TLV parsing
- Consolidated `signedBytes` into `SignatureInfo`, unifying the signature API
- Renamed registries to consistent `{SealType}{Concept}Registry` schema
- Added `SUB_MESSAGES` coding to distinguish IDB container messages from plain `BYTES`
- Bumped Kotlin, serialization, cryptography, and Kermit versions

### Removed
- TdDoc/fr2ddoc support (2D-DOC parsing removed)
- `Message.tag` reverted from `String` back to `Int`
- `SealCodings.json` removed in favour of renamed `VdsDocumentTypes`

### Fixed
- `IdbPayload.encoded` wrote `signerCertificate` instead of duplicate `messageGroup`
- Used `assertTrue` instead of `assert` and removed unnecessary non-null assertions in `IdbPayloadCommonTest`
- Fixed iOS test resources and test cases

---

## [0.14.0] - 2026-03-03

### Added
- Configurable `metadataTagList` on `SealDto` for base-type metadata tags
- Documentation for `metadataMessageList` for administrative document seals
- New README section: "Byte-level structure inspection with the dissect package"

### Changed
- Reuse shared `Json` instance in all registry classes

---

## [0.13.0] - 2026-03-02

### Added
- `DefinitionRegistry` interface with `addCustom*` / `replaceCustom*` methods for custom registry support
- Expected messages for `IdbNationalDocumentTypes`, exposed via registry
- `dissect` package for byte-offset visualization of seal structures (`Seal.annotate()`)
- `Seal.encoded` property
- IDB message definitions for `PROOF_OF_VACCINATION`, `PROOF_OF_RECOVERY`, and `DIGITAL_TRAVEL_AUTHORIZATION`
- `documentProfileUuid` pulled up to `Seal` base class

### Changed
- Renamed `annotation` package to `dissect`
- IDB/VDS message definitions enriched with sub-message structure and `DATE_TIME` coding
- Removed compound message mechanism; `VALIDITY_DATES` now uses direct field structure

### Fixed
- `VISA` sub-message required flags corrected, top-level byte ranges recalculated
- `parseV4CertRefAndDatesLenient` signature cleaned up (removed `signerIdentifier` param)
- Tolerance for non-standard cert ref length encoding in DEZV version 4 headers

---

## [0.12.0] - 2026-02-16

### Added
- XML document profile parser for BSI TR-03171
- Integration tests for custom registry usage during seal parsing
- `ADDRESS_STICKER_RP` seal profile with `VdsHeader` tests
- Parse TR-03171 Tag 0 as metadata (UUID) and Tag 1 as `ValidityDatesValue`
- `baseDocumentType` property exposed on `Seal`
- `encodedSignerIdentifierAndCertificateReference` KDoc

### Changed
- Renamed registry classes for consistency; added `resetToDefaults()`
- Consolidated `IdbMessage` and `VdsMessage` into unified `Message` class
- Renamed `Feature` to `Message` across the entire codebase
- Unified VDS and IDB API naming for consistency
- Introduced `FeatureValue` sealed class for type-safe message value access
- `VdsMessageGroup.Builder` gains tag-based `addFeature()`
- Extended feature definitions for UUID-based seal profiles (TR-03171)

---

## [0.10.3] - 2026-01-07

### Added
- Comprehensive tests for `bytesToDecode` calculation
- Presumed profile data for registration document
- Additional length decoding for parsing `VdsHeader` as defined in TR-03171 "Verwaltungsdokumente"

### Changed
- Migrated to Gradle version catalogs
- Embedded JSON resources as Kotlin constants for reliable iOS Maven publishing

### Fixed
- Certificate length bytes-to-decode calculation in `VdsHeader` parsing
- iOS verifier tests for Brainpool curve certificates

---

## [0.10.0] - 2025-11-29

### Changed
- Updated IDB barcode identifier from `NDB` to `RDB`
- Updated dependencies
- Minor code cleaning (removed warnings)

---

[Unreleased]: https://github.com/tsenger/vdstools/compare/v0.19.0...HEAD
[0.19.0]: https://github.com/tsenger/vdstools/compare/v0.18.0...v0.19.0
[0.18.0]: https://github.com/tsenger/vdstools/compare/v0.17.0...v0.18.0
[0.17.0]: https://github.com/tsenger/vdstools/compare/v0.16.0...v0.17.0
[0.16.0]: https://github.com/tsenger/vdstools/compare/v0.15.0...v0.16.0
[0.15.0]: https://github.com/tsenger/vdstools/compare/v0.14.0...v0.15.0
[0.14.0]: https://github.com/tsenger/vdstools/compare/v0.13.0...v0.14.0
[0.13.0]: https://github.com/tsenger/vdstools/compare/v0.12.0...v0.13.0
[0.12.0]: https://github.com/tsenger/vdstools/compare/v0.10.3...v0.12.0
[0.10.3]: https://github.com/tsenger/vdstools/compare/v0.10.0...v0.10.3
[0.10.0]: https://github.com/tsenger/vdstools/compare/v0.9.3...v0.10.0