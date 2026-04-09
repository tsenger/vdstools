# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased] - v0.16.0

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

[Unreleased]: https://github.com/tsenger/vdstools/compare/v0.15.0...HEAD
[0.15.0]: https://github.com/tsenger/vdstools/compare/v0.14.0...v0.15.0
[0.14.0]: https://github.com/tsenger/vdstools/compare/v0.13.0...v0.14.0
[0.13.0]: https://github.com/tsenger/vdstools/compare/v0.12.0...v0.13.0
[0.12.0]: https://github.com/tsenger/vdstools/compare/v0.10.3...v0.12.0
[0.10.3]: https://github.com/tsenger/vdstools/compare/v0.10.0...v0.10.3
[0.10.0]: https://github.com/tsenger/vdstools/compare/v0.9.3...v0.10.0