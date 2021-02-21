package coverosR3z.server.types

enum class Pragma(value: String) {

    /**
     * Used to designate content as definitely not cached,
     * useful for some security considerations
     */
    PRAGMA_DISALLOW_CACHING("no-cache");

    /**
     * Returns this enum as a full header
     */
    val details = "Pragma: $value"
}