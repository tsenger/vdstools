package de.tsenger.vdstools.internal

import de.tsenger.vdstools.VdsLogLevel
import de.tsenger.vdstools.VdsTools

internal fun logV(tag: String, msg: String) = VdsTools.logger.log(VdsLogLevel.VERBOSE, tag, msg, null)
internal fun logD(tag: String, msg: String) = VdsTools.logger.log(VdsLogLevel.DEBUG, tag, msg, null)
internal fun logI(tag: String, msg: String) = VdsTools.logger.log(VdsLogLevel.INFO, tag, msg, null)
internal fun logW(tag: String, msg: String, t: Throwable? = null) = VdsTools.logger.log(VdsLogLevel.WARN, tag, msg, t)
internal fun logE(tag: String, msg: String, t: Throwable? = null) = VdsTools.logger.log(VdsLogLevel.ERROR, tag, msg, t)
