package coverosR3z.server.types

private const val secondsToCache = 60 * 10

enum class CacheControl(value: String) {

    /**
     * See https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control
     *
     * This is a highly aggressive caching approach.  We're basically telling
     * anyone and everyone to cache every single dang thing, which means
     * we won't hear from the client again once they request any data
     * marked with this, for a period of time
     */
    AGGRESSIVE_WEB_CACHE("public, max-age=$secondsToCache, immutable"),

    /**
     * Used to designate content as definitely not cached,
     * useful for some security considerations
     */
    DISALLOW_CACHING("no-cache, no-store, must-revalidate");

    /**
     * Returns this enum as a full header
     */
    val details = "Cache-Control: $value"
}