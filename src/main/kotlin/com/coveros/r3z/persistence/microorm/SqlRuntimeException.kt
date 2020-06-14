package com.coveros.r3z.persistence.microorm


internal class SqlRuntimeException : RuntimeException {
    constructor(ex: Exception?) : super(ex) {}
    constructor(message: String?) : super(message) {}
}
