package coverosR3z.server.types

/**
 * These are mime types (see https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types)
 * which we'll use when conversing with clients to describe data
 */
enum class ContentType(val value: String) {
    // Text MIME types - see https://www.iana.org/assignments/media-types/media-types.xhtml#text
    TEXT_HTML("Content-Type: text/html; charset=UTF-8"),
    TEXT_CSS("Content-Type: text/css"),

    // Application MIME types - see https://www.iana.org/assignments/media-types/media-types.xhtml#application
    APPLICATION_JAVASCRIPT("Content-Type: application/javascript"),
    IMAGE_JPEG("Content-Type: image/jpeg"),
    IMAGE_WEBP("Content-Type: image/webp")

}