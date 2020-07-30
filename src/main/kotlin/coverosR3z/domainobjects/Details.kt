package coverosR3z.domainobjects

import kotlinx.serialization.Serializable

const val MAX_DETAILS_LENGTH = 500

@Serializable
data class Details(val value : String = "") {
    init {
        assert(value.length <= MAX_DETAILS_LENGTH) { "no reason why details for a time entry would ever need to be this big. " +
                "if you have more to say than the lord's prayer, you're probably doing it wrong." }
    }
}