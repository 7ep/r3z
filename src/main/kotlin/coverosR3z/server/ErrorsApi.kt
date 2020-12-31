package coverosR3z.server

import coverosR3z.logging.logTrace

fun handleBadRequest(): PreparedResponseData {
    return PreparedResponseData(badRequestHTML, StatusCode.BAD_REQUEST, listOf(ContentType.TEXT_HTML.value))
}

fun handleNotFound(): PreparedResponseData {
    return PreparedResponseData(notFoundHTML, StatusCode.NOT_FOUND, listOf(ContentType.TEXT_HTML.value))
}

fun handleUnauthorized() : PreparedResponseData {
    return PreparedResponseData(unauthorizedHTML, StatusCode.UNAUTHORIZED, listOf(ContentType.TEXT_HTML.value))
}

fun handleInternalServerError(shortMessage : String?, fullStackTrace : String) : PreparedResponseData {
    logTrace { "handling internal server error: $fullStackTrace" }
    return PreparedResponseData(internalServerErrorHTML(shortMessage ?: "(None)"), StatusCode.INTERNAL_SERVER_ERROR, listOf(ContentType.TEXT_HTML.value))
}

const val badRequestHTML = """
<!DOCTYPE html>    
<html>
    <head>
    </head>
        <title>400 error</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    <body>
       <p>400 error - BAD REQUEST</p>
    </body>
</html>
"""

const val notFoundHTML = """
<!DOCTYPE html>    
<html>
    <head>
    </head>
        <title>404 error</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    <body>
       <p>404 error - NOT FOUND</p>
    </body>
</html>
"""

const val unauthorizedHTML = """
<!DOCTYPE html>    
<html>
    <head>
    </head>
        <title>401 error</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    <body>
       <p>401 error - UNAUTHORIZED</p>
    </body>
</html>        
"""

fun internalServerErrorHTML(msg : String) : String {
    return """
<!DOCTYPE html>    
<html>
    <head>
    </head>
        <title>500 error</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    <body>
       <p>500 error - INTERNAL SERVER ERROR</p>
       <p><a href="homepage">Homepage</a></p>
       <p>Error message: $msg</p>
    </body>
</html>        
"""
}