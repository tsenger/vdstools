# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- BSI TR-03171 v0.9 support (document category 0xC9) via new
  `ADMINISTRATIVE_DOCUMENTS_V9` base document type with reserved metadata tags
  0x00–0x06 (UUID, validity dates, profile/certificate/status URIs, status list index)
- `MessageCoding.DATE_STRING` for 8-byte `YYYYMMDD` UTF-8 date encoding used by
  TR-03171 v0.9 `validFrom` / `validTo` fields

### Changed
- **Breaking:** the legacy TR-03171 v0.8 base document type was renamed from
  `ADMINISTRATIVE_DOCUMENTS` to `ADMINISTRATIVE_DOCUMENTS_V8` (document category
  0xC8 / documentRef 0x01C8 unchanged). Update any custom profile JSON
  (`baseDocumentType`), `VdsHeader.Builder(...)` calls, and comparisons against
  `seal.baseDocumentType` accordingly. Use the constants
  `DataEncoder.ADMINISTRATIVE_DOCUMENTS_V8` / `…_V9` instead of string literals.
- `DataEncoder.loadVdsProfileDefinitionFromXml()` and
  `ProfileConverter.toVdsProfileDefinition()` now default to
  `ADMINISTRATIVE_DOCUMENTS_V9`; pass `ADMINISTRATIVE_DOCUMENTS_V8` for legacy 0xC8 seals

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

[Unreleased]: https://github.com/tsenger/vdstools/compare/v0.17.0...HEAD
[0.17.0]: https://github.com/tsenger/vdstools/compare/v0.16.0...v0.17.0
[0.16.0]: https://github.com/tsenger/vdstools/compare/v0.15.0...v0.16.0
[0.15.0]: https://github.com/tsenger/vdstools/compare/v0.14.0...v0.15.0
[0.14.0]: https://github.com/tsenger/vdstools/compare/v0.13.0...v0.14.0
[0.13.0]: https://github.com/tsenger/vdstools/compare/v0.12.0...v0.13.0
[0.12.0]: https://github.com/tsenger/vdstools/compare/v0.10.3...v0.12.0
[0.10.3]: https://github.com/tsenger/vdstools/compare/v0.10.0...v0.10.3
[0.10.0]: https://github.com/tsenger/vdstools/compare/v0.9.3...v0.10.0