package de.tsenger.vdstools

enum class VdsLogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }

fun interface VdsLogger {
    fun log(level: VdsLogLevel, tag: String, message: String, throwable: Throwable?)
}

object VdsTools {
    var logger: VdsLogger = VdsLogger { _, _, _, _ -> }
}
