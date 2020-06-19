package com.coveros.r3z.persistence.microorm

import kotlin.reflect.KClass

/**
 * a container for the parameters for the [SqlData] object.
 */
data class ParameterObject<T : Any> internal constructor(
    /**
         * The data we are injecting into the SQL statement
         */
    val data: Any,

    /**
         * The type of the data we are injecting into the SQL statement (e.g. Integer, String, etc.)
         */
    val type: KClass<out T>
)