package com.coveros.r3z.logging

class Logger {

    fun info(msg : String) {
        println("INFO: $msg")
    }

    fun error(msg : String) {
        println("ERROR: $msg")
    }
}