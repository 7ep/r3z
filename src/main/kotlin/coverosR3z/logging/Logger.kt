package coverosR3z.logging

import coverosR3z.authentication.CurrentUserAccessor

val cua = CurrentUserAccessor()

fun logInfo(msg : String) {
    try {
        println("INFO: ${cua.get().name}: $msg")
    }catch (ex: AssertionError) {}
}