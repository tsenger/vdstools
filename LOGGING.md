# Logging

VdsTools does not log anything by default. The library defines a minimal logging interface that you can optionally wire up to whatever logging framework your application already uses.

## Interface

```kotlin
enum class VdsLogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }

fun interface VdsLogger {
    fun log(level: VdsLogLevel, tag: String, message: String, throwable: Throwable?)
}
```

## Enabling logging

Set `VdsTools.logger` once at application startup:

```kotlin
import de.tsenger.vdstools.VdsLogLevel
import de.tsenger.vdstools.VdsLogger
import de.tsenger.vdstools.VdsTools

VdsTools.logger = VdsLogger { level, tag, message, throwable ->
    // forward to your preferred framework here
}
```

## Examples

### Android – Timber

```kotlin
VdsTools.logger = VdsLogger { level, tag, message, throwable ->
    when (level) {
        VdsLogLevel.VERBOSE -> Timber.tag(tag).v(throwable, message)
        VdsLogLevel.DEBUG   -> Timber.tag(tag).d(throwable, message)
        VdsLogLevel.INFO    -> Timber.tag(tag).i(throwable, message)
        VdsLogLevel.WARN    -> Timber.tag(tag).w(throwable, message)
        VdsLogLevel.ERROR   -> Timber.tag(tag).e(throwable, message)
    }
}
```

### JVM – SLF4J

```kotlin
VdsTools.logger = VdsLogger { level, tag, message, throwable ->
    val logger = LoggerFactory.getLogger(tag)
    when (level) {
        VdsLogLevel.VERBOSE,
        VdsLogLevel.DEBUG -> logger.debug(message, throwable)
        VdsLogLevel.INFO  -> logger.info(message, throwable)
        VdsLogLevel.WARN  -> logger.warn(message, throwable)
        VdsLogLevel.ERROR -> logger.error(message, throwable)
    }
}
```

### Kermit (Kotlin Multiplatform)

```kotlin
VdsTools.logger = VdsLogger { level, tag, message, throwable ->
    val logger = Logger.withTag(tag)
    when (level) {
        VdsLogLevel.VERBOSE -> logger.v(message, throwable)
        VdsLogLevel.DEBUG   -> logger.d(message, throwable)
        VdsLogLevel.INFO    -> logger.i(message, throwable)
        VdsLogLevel.WARN    -> logger.w(message, throwable)
        VdsLogLevel.ERROR   -> logger.e(message, throwable)
    }
}
```

## What gets logged

| Level   | Examples                                                              |
|---------|-----------------------------------------------------------------------|
| VERBOSE | Raw bytes of keys, messages, and signatures during verification       |
| DEBUG   | Resolved profile definitions, compression ratios, parsing details     |
| INFO    | Registry replacements and resets via `DataEncoder`                    |
| WARN    | Unknown seal types, malformed cert ref lengths, deprecated identifiers |
| ERROR   | Encoding failures, unsupported versions, missing signature fields     |

WARN and ERROR messages are the most actionable — they indicate configuration problems or unexpected input that may require attention.
