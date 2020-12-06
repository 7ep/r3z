package coverosR3z.server

import coverosR3z.misc.toBytes

/**
 * Data for shipping to the client
 */
data class PreparedResponseData(val fileContents: ByteArray, val statusCode: StatusCode, val headers : List<String> = emptyList()){
    constructor(fileContents: String, statusCode: StatusCode, headers : List<String> = emptyList())
            : this(toBytes(fileContents), statusCode, headers)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PreparedResponseData

        if (!fileContents.contentEquals(other.fileContents)) return false
        if (statusCode != other.statusCode) return false
        if (headers != other.headers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileContents.contentHashCode()
        result = 31 * result + statusCode.hashCode()
        result = 31 * result + headers.hashCode()
        return result
    }


}