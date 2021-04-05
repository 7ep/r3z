package coverosR3z.server.api

import coverosR3z.logging.ILogger
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.types.ContentType
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.StatusCode

fun handleBadRequest(msg: String? = null): PreparedResponseData {
    return PreparedResponseData(badRequestHTML(msg), StatusCode.BAD_REQUEST, listOf(ContentType.TEXT_HTML.value))
}

fun handleNotFound(): PreparedResponseData {
    return PreparedResponseData(notFoundHTML, StatusCode.NOT_FOUND, listOf(ContentType.TEXT_HTML.value))
}

fun handleUnauthenticated() : PreparedResponseData {
    return PreparedResponseData(unauthorizedHTML, StatusCode.UNAUTHORIZED, listOf(ContentType.TEXT_HTML.value))
}

/**
 * If the user tries doing something they shouldn't be allowed
 * to do, return this.  For example, if they collect the URL for
 * creating a new employee and POST to that, as a regular user,
 * this will be returned.
 */
fun handleUnauthorized(message: String? = null): PreparedResponseData {
    return PreparedResponseData(message ?: "This is forbidden", StatusCode.FORBIDDEN, listOf(ContentType.TEXT_HTML.value))
}

fun handleInternalServerError(shortMessage : String, fullStackTrace : String, logger: ILogger) : PreparedResponseData {
    logger.logTrace { "handling internal server error: $fullStackTrace" }
    return PreparedResponseData(
        internalServerErrorHTML(shortMessage), StatusCode.INTERNAL_SERVER_ERROR, listOf(
        ContentType.TEXT_HTML.value))
}

fun badRequestHTML(msg: String?) : String {
    val errorMessageLine = if (msg != null) "<p>Error message:  ${safeHtml(msg)}</p>" else ""
    return  """
<!DOCTYPE html>    
<html lang="en">
    <head>
    </head>
        <title>400 error</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta apifile="ErrorsAPI" >
    <body>
       <p>400 error - BAD REQUEST</p>$errorMessageLine
    </body>
</html>
"""
}

const val notFoundHTML = """
<!DOCTYPE html>    
<html lang="en">
    <head>
    </head>
        <title>404 error</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta apifile="ErrorsAPI" >
    <body>
       <p>404 error - NOT FOUND</p>
    </body>
</html>
"""

const val unauthorizedHTML = """
<!DOCTYPE html>    
<html lang="en">
    <head>
    </head>
        <title>401 error</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta apifile="ErrorsAPI" >
    <body>
       <p>401 error - UNAUTHORIZED</p>
    </body>
</html>        
"""

fun internalServerErrorHTML(msg : String) : String {
    val safeMsg = safeHtml(msg)
    return """
<!DOCTYPE html>    
<html lang="en">
    <head>
    </head>
        <title>500 error</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta apifile="ErrorsAPI" >
    <body>
       <p>500 error - INTERNAL SERVER ERROR</p>
       <p><a href="homepage">Homepage</a></p>
       <p>Error message: $safeMsg</p>
    </body>
</html>        
"""
}