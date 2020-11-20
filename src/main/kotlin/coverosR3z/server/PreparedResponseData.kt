package coverosR3z.server

/**
 * Data for shipping to the client
 */
data class PreparedResponseData(val fileContents: String, val responseStatus: ResponseStatus, val headers : List<String> = emptyList())

data class PreparedResponseDataBytes(val fileContents: ByteArray, val responseStatus: ResponseStatus, val headers : List<String> = emptyList()) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PreparedResponseDataBytes

        if (!fileContents.contentEquals(other.fileContents)) return false
        if (responseStatus != other.responseStatus) return false
        if (headers != other.headers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileContents.contentHashCode()
        result = 31 * result + responseStatus.hashCode()
        result = 31 * result + headers.hashCode()
        return result
    }
}