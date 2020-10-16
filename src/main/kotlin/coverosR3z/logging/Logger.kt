package coverosR3z.logging

import coverosR3z.authentication.CurrentUserAccessor

val cua = CurrentUserAccessor()

fun logInfo(msg : String) {
    println("INFO: ${cua.get()?.name ?: "SYSTEM"}: $msg")
}

/**
 * Used to log finicky details of technical solutions
 */
fun logDebug(msg : String) {
    println("DEBUG: $msg")
}