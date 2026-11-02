@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package net.payrdr.mobile.payment.sdk.payment.ios

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.writeToFile

/**
 * Install a Kotlin/Native unhandled-exception hook that writes the exception class +
 * message + stack trace to a plain file in the app's Documents directory before
 * termination. NSLog is NOT used because Kotlin/Native's variadic bridging to NSLog
 * corrupts the format-string args and crashes inside `objc_opt_respondsToSelector`
 * — masking the real cause of the crash we're trying to diagnose.
 *
 * After a crash, read the text via:
 *   find ~/Library/Developer/CoreSimulator/Devices -name kotlin_crash.txt
 *
 * Sample-only debug helper; not part of the merchant-facing SDK surface.
 */
public fun installIosCrashLogger() {
    kotlin.native.setUnhandledExceptionHook { throwable ->
        val text =
            buildString {
                appendLine("[KotlinCrash] ${throwable::class.qualifiedName ?: "?"}: ${throwable.message ?: "<no message>"}")
                appendLine("[KotlinCrash stack]")
                appendLine(throwable.stackTraceToString())
                var cause: Throwable? = throwable.cause
                var depth = 0
                while (cause != null && depth < 10) {
                    appendLine("--- Caused by ---")
                    appendLine("${cause::class.qualifiedName ?: "?"}: ${cause.message ?: "<no message>"}")
                    appendLine(cause.stackTraceToString())
                    cause = cause.cause
                    depth += 1
                }
            }
        val docs =
            NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true,
            ).firstOrNull() as? String ?: "/tmp"
        val path = "$docs/kotlin_crash.txt"
        (text as NSString).writeToFile(
            path = path,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
        // Silent println in case the process survives long enough for Xcode console to flush.
        println(text)
    }
}
