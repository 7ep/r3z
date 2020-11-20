package coverosR3z.server

import coverosR3z.misc.toBytes

fun handleBadRequest(): PreparedResponseData {
    return PreparedResponseData(badRequestHTML, ResponseStatus.BAD_REQUEST, listOf(ContentType.TEXT_HTML.ct))
}

fun handleNotFound(): PreparedResponseData {
    return PreparedResponseData(notFoundHTML, ResponseStatus.NOT_FOUND, listOf(ContentType.TEXT_HTML.ct))
}

fun handleUnauthorized() : PreparedResponseData {
    return PreparedResponseData(unauthorizedHTML, ResponseStatus.UNAUTHORIZED, listOf(ContentType.TEXT_HTML.ct))
}

val badRequestHTML = toBytes("""
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
""")

val notFoundHTML = toBytes("""
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
""")

val unauthorizedHTML = toBytes("""
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
""")

fun generalMessageHTML(msg : String) : ByteArray {
    return toBytes("""
<!DOCTYPE html>        
<html>
    <head>
    </head>
        <title>general error page</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    <body>
       <p><a href="homepage">Homepage</a></p>
       <p>Error message: $msg</p>
       <p></p>
    </body>
</html>        
""")
}